// lib/core/services/guardian_service.dart
//
// Flutter-side wrapper for the Guardian Shield Android native module.
// Communicates over the "com.mindful.android/guardian" MethodChannel.

import 'dart:convert';
import 'package:flutter/services.dart';

class GuardianService {
  static final GuardianService instance = GuardianService._();
  GuardianService._();

  static const MethodChannel _channel =
      MethodChannel('com.mindful.android/guardian');

  // ── Service control ─────────────────────────────────────────────────────────
  Future<void> startGuardian() async =>
      _channel.invokeMethod('startGuardian');

  Future<void> stopGuardian() async =>
      _channel.invokeMethod('stopGuardian');

  // ── Protection settings ──────────────────────────────────────────────────────
  Future<void> setProtectionEnabled(bool enabled) =>
      _channel.invokeMethod('setProtectionEnabled', {'enabled': enabled});

  Future<void> setAiDetection(bool enabled) =>
      _channel.invokeMethod('setAiDetection', {'enabled': enabled});

  /// gender: "MALE" | "FEMALE" | "NONE"
  Future<void> setUserGender(String gender) =>
      _channel.invokeMethod('setUserGender', {'gender': gender});

  Future<void> setKeywordFilter(bool enabled) =>
      _channel.invokeMethod('setKeywordFilter', {'enabled': enabled});

  Future<void> setTempBlockDuration(int minutes) =>
      _channel.invokeMethod('setTempBlockDuration', {'minutes': minutes});

  Future<void> setAiThreshold(double value) =>
      _channel.invokeMethod('setAiThreshold', {'value': value});

  Future<void> setNsfwGateThreshold(double value) =>
      _channel.invokeMethod('setNsfwGateThreshold', {'value': value});

  Future<void> setGenderThreshold(double value) =>
      _channel.invokeMethod('setGenderThreshold', {'value': value});

  Future<void> setGridVoteCount(int count) =>
      _channel.invokeMethod('setGridVoteCount', {'count': count});

  // ── Keywords ─────────────────────────────────────────────────────────────────
  /// Returns the new keyword id
  Future<int> addKeyword({
    required String keyword,
    bool isRegex = false,
    int severity = 1,
  }) async {
    final id = await _channel.invokeMethod<int>('addKeyword', {
      'json': jsonEncode({
        'keyword': keyword,
        'isRegex': isRegex,
        'severity': severity,
      }),
    });
    return id ?? 0;
  }

  Future<void> removeKeyword(int id) =>
      _channel.invokeMethod('removeKeyword', {'id': id});

  // ── App rules ────────────────────────────────────────────────────────────────
  Future<void> blockApp(String packageName, {String? appName}) =>
      _channel.invokeMethod('blockApp', {
        'json': jsonEncode({
          'packageName': packageName,
          'appName': appName ?? packageName,
        }),
      });

  Future<void> unblockApp(String packageName) =>
      _channel.invokeMethod('unblockApp', {'packageName': packageName});

  Future<void> whitelistApp(String packageName, {String? appName}) =>
      _channel.invokeMethod('whitelistApp', {
        'json': jsonEncode({
          'packageName': packageName,
          'appName': appName ?? packageName,
        }),
      });

  Future<void> removeWhitelist(String packageName) =>
      _channel.invokeMethod('removeWhitelist', {'packageName': packageName});

  // ── Schedule rules ───────────────────────────────────────────────────────────
  Future<void> setSchedule({
    required String packageName,
    required int startHour,
    required int startMinute,
    required int endHour,
    required int endMinute,
    List<int> enabledDays = const [0, 1, 2, 3, 4, 5, 6],
    bool enabled = true,
  }) =>
      _channel.invokeMethod('setSchedule', {
        'json': jsonEncode({
          'packageName': packageName,
          'startHour': startHour,
          'startMinute': startMinute,
          'endHour': endHour,
          'endMinute': endMinute,
          'enabledDays': enabledDays,
          'enabled': enabled,
        }),
      });

  Future<void> removeSchedule(String packageName) =>
      _channel.invokeMethod('removeSchedule', {'packageName': packageName});

  // ── Block events ──────────────────────────────────────────────────────────────
  Future<List<GuardianBlockEvent>> getBlockEvents() async {
    final raw = await _channel.invokeMethod<String>('getBlockEvents');
    if (raw == null || raw.isEmpty) return [];
    try {
      final list = jsonDecode(raw) as List<dynamic>;
      return list.map((e) => GuardianBlockEvent.fromJson(e as Map<String, dynamic>)).toList();
    } catch (_) {
      return [];
    }
  }

  Future<void> clearBlockEvents() =>
      _channel.invokeMethod('clearBlockEvents');

  // ── PIN ───────────────────────────────────────────────────────────────────────
  /// Returns true if pin was set successfully
  Future<bool> setPin(String pin) async {
    final ok = await _channel.invokeMethod<bool>('setPinEnabled', {'pin': pin});
    return ok ?? false;
  }

  /// Returns PinVerifyResult
  Future<PinVerifyResult> verifyPin(String pin) async {
    final raw = await _channel.invokeMethod<String>('verifyPin', {'pin': pin}) ?? '';
    if (raw == 'SUCCESS') return PinVerifyResult.success();
    if (raw == 'NOT_SET') return PinVerifyResult.notSet();
    if (raw.startsWith('WRONG:')) {
      final remaining = int.tryParse(raw.substring(6)) ?? 0;
      return PinVerifyResult.wrong(remaining);
    }
    if (raw.startsWith('LOCKED:')) {
      final ms = int.tryParse(raw.substring(7)) ?? 0;
      return PinVerifyResult.lockedOut(Duration(milliseconds: ms));
    }
    return PinVerifyResult.notSet();
  }

  Future<void> clearPin() => _channel.invokeMethod('clearPin');
}

// ── Data models ────────────────────────────────────────────────────────────────

class GuardianBlockEvent {
  final int id;
  final String packageName;
  final String reason;
  final String matchedTerm;
  final DateTime timestamp;

  GuardianBlockEvent({
    required this.id,
    required this.packageName,
    required this.reason,
    required this.matchedTerm,
    required this.timestamp,
  });

  factory GuardianBlockEvent.fromJson(Map<String, dynamic> json) =>
      GuardianBlockEvent(
        id: json['id'] as int? ?? 0,
        packageName: json['packageName'] as String? ?? '',
        reason: json['reason'] as String? ?? '',
        matchedTerm: json['matchedTerm'] as String? ?? '',
        timestamp: DateTime.fromMillisecondsSinceEpoch(
            json['timestamp'] as int? ?? 0),
      );
}

sealed class PinVerifyResult {
  const PinVerifyResult();
  factory PinVerifyResult.success() = PinSuccess;
  factory PinVerifyResult.wrong(int remaining) = PinWrong;
  factory PinVerifyResult.lockedOut(Duration duration) = PinLockedOut;
  factory PinVerifyResult.notSet() = PinNotSet;
}

class PinSuccess extends PinVerifyResult { const PinSuccess(); }
class PinWrong extends PinVerifyResult {
  final int remainingAttempts;
  const PinWrong(this.remainingAttempts);
}
class PinLockedOut extends PinVerifyResult {
  final Duration lockoutDuration;
  const PinLockedOut(this.lockoutDuration);
}
class PinNotSet extends PinVerifyResult { const PinNotSet(); }
