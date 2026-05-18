// lib/providers/guardian/guardian_provider.dart
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:mindful/core/services/guardian_service.dart';

// ── State Models ─────────────────────────────────────────────────────────────

class GuardianState {
  final bool protectionEnabled;
  final bool aiDetection;
  final bool keywordFilter;
  final String userGender;
  final int tempBlockMins;
  final double aiThreshold;
  final List<GuardianKeyword> keywords;
  final List<GuardianAppRule> blockedApps;
  final List<GuardianAppRule> whitelistedApps;
  final List<GuardianBlockEvent> recentEvents;
  final bool isLoading;

  const GuardianState({
    this.protectionEnabled = true,
    this.aiDetection = false,
    this.keywordFilter = true,
    this.userGender = 'NONE',
    this.tempBlockMins = 15,
    this.aiThreshold = 0.65,
    this.keywords = const [],
    this.blockedApps = const [],
    this.whitelistedApps = const [],
    this.recentEvents = const [],
    this.isLoading = false,
  });

  GuardianState copyWith({
    bool? protectionEnabled,
    bool? aiDetection,
    bool? keywordFilter,
    String? userGender,
    int? tempBlockMins,
    double? aiThreshold,
    List<GuardianKeyword>? keywords,
    List<GuardianAppRule>? blockedApps,
    List<GuardianAppRule>? whitelistedApps,
    List<GuardianBlockEvent>? recentEvents,
    bool? isLoading,
  }) => GuardianState(
    protectionEnabled: protectionEnabled ?? this.protectionEnabled,
    aiDetection: aiDetection ?? this.aiDetection,
    keywordFilter: keywordFilter ?? this.keywordFilter,
    userGender: userGender ?? this.userGender,
    tempBlockMins: tempBlockMins ?? this.tempBlockMins,
    aiThreshold: aiThreshold ?? this.aiThreshold,
    keywords: keywords ?? this.keywords,
    blockedApps: blockedApps ?? this.blockedApps,
    whitelistedApps: whitelistedApps ?? this.whitelistedApps,
    recentEvents: recentEvents ?? this.recentEvents,
    isLoading: isLoading ?? this.isLoading,
  );
}

class GuardianKeyword {
  final int id;
  final String keyword;
  final bool isRegex;
  final int severity;

  const GuardianKeyword({
    required this.id,
    required this.keyword,
    this.isRegex = false,
    this.severity = 1,
  });
}

class GuardianAppRule {
  final String packageName;
  final String appName;
  final bool isBlocked;
  final bool isWhitelisted;

  const GuardianAppRule({
    required this.packageName,
    required this.appName,
    this.isBlocked = false,
    this.isWhitelisted = false,
  });
}

// ── Provider ─────────────────────────────────────────────────────────────────

final guardianProvider =
    StateNotifierProvider<GuardianNotifier, GuardianState>((ref) {
  return GuardianNotifier();
});

class GuardianNotifier extends StateNotifier<GuardianState> {
  GuardianNotifier() : super(const GuardianState()) {
    _init();
  }

  final _svc = GuardianService.instance;

  Future<void> _init() async {
    state = state.copyWith(isLoading: true);
    try {
      _svc.startGuardian();
      await refreshEvents();
    } catch (e) {
      debugPrint('Guardian init error: $e');
    } finally {
      state = state.copyWith(isLoading: false);
    }
  }

  // ── Protection toggle ─────────────────────────────────────────────────────
  Future<void> toggleProtection(bool value) async {
    state = state.copyWith(protectionEnabled: value);
    await _svc.setProtectionEnabled(value);
    if (value) _svc.startGuardian(); else _svc.stopGuardian();
  }

  Future<void> toggleAiDetection(bool value) async {
    state = state.copyWith(aiDetection: value);
    await _svc.setAiDetection(value);
  }

  Future<void> toggleKeywordFilter(bool value) async {
    state = state.copyWith(keywordFilter: value);
    await _svc.setKeywordFilter(value);
  }

  Future<void> setGender(String gender) async {
    state = state.copyWith(userGender: gender);
    await _svc.setUserGender(gender);
  }

  Future<void> setTempBlockMins(int mins) async {
    state = state.copyWith(tempBlockMins: mins);
    await _svc.setTempBlockDuration(mins);
  }

  Future<void> setAiThreshold(double val) async {
    state = state.copyWith(aiThreshold: val);
    await _svc.setAiThreshold(val);
  }

  // ── Keywords ──────────────────────────────────────────────────────────────
  Future<void> addKeyword(String kw, {bool isRegex = false}) async {
    if (kw.trim().isEmpty) return;
    final id = await _svc.addKeyword(keyword: kw.trim(), isRegex: isRegex);
    final updated = [
      ...state.keywords,
      GuardianKeyword(id: id, keyword: kw.trim(), isRegex: isRegex),
    ];
    state = state.copyWith(keywords: updated);
  }

  Future<void> removeKeyword(int id) async {
    await _svc.removeKeyword(id);
    state = state.copyWith(
      keywords: state.keywords.where((k) => k.id != id).toList(),
    );
  }

  // ── App Rules ─────────────────────────────────────────────────────────────
  Future<void> blockApp(String pkg, String name) async {
    await _svc.blockApp(pkg, appName: name);
    final updated = [
      ...state.blockedApps.where((a) => a.packageName != pkg),
      GuardianAppRule(packageName: pkg, appName: name, isBlocked: true),
    ];
    state = state.copyWith(blockedApps: updated);
  }

  Future<void> unblockApp(String pkg) async {
    await _svc.unblockApp(pkg);
    state = state.copyWith(
      blockedApps: state.blockedApps.where((a) => a.packageName != pkg).toList(),
    );
  }

  Future<void> whitelistApp(String pkg, String name) async {
    await _svc.whitelistApp(pkg, appName: name);
    final updated = [
      ...state.whitelistedApps.where((a) => a.packageName != pkg),
      GuardianAppRule(packageName: pkg, appName: name, isWhitelisted: true),
    ];
    state = state.copyWith(whitelistedApps: updated);
  }

  Future<void> removeWhitelist(String pkg) async {
    await _svc.removeWhitelist(pkg);
    state = state.copyWith(
      whitelistedApps: state.whitelistedApps.where((a) => a.packageName != pkg).toList(),
    );
  }

  // ── Events ────────────────────────────────────────────────────────────────
  Future<void> refreshEvents() async {
    try {
      final events = await _svc.getBlockEvents();
      state = state.copyWith(recentEvents: events);
    } catch (e) {
      debugPrint('Failed to load block events: $e');
    }
  }

  Future<void> clearEvents() async {
    await _svc.clearBlockEvents();
    state = state.copyWith(recentEvents: []);
  }
}
