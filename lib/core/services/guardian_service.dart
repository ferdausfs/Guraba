/*
 * Guraba — GuardianService
 *
 * Dart-side wrapper around the native Guardian MethodChannel (`guraba.guardian`).
 * Exposes the merged Guardian-Shield features (on-device AI NSFW filtering,
 * PIN protection, keyword rules, block-event log, TFLite model management)
 * to the rest of the Flutter app.
 */
import 'package:flutter/services.dart';

class GuardianAiSettings {
  final bool aiEnabled;
  final String userGender; // "NONE" | "MALE" | "FEMALE"
  final double aiThreshold;
  final double nsfwGateThreshold;
  final double genderThreshold;
  final int gridVoteCount;

  const GuardianAiSettings({
    required this.aiEnabled,
    required this.userGender,
    required this.aiThreshold,
    required this.nsfwGateThreshold,
    required this.genderThreshold,
    required this.gridVoteCount,
  });

  factory GuardianAiSettings.fromMap(Map m) => GuardianAiSettings(
        aiEnabled: m['aiEnabled'] as bool? ?? false,
        userGender: m['userGender'] as String? ?? 'NONE',
        aiThreshold: (m['aiThreshold'] as num? ?? 0.65).toDouble(),
        nsfwGateThreshold: (m['nsfwGateThreshold'] as num? ?? 0.60).toDouble(),
        genderThreshold: (m['genderThreshold'] as num? ?? 0.70).toDouble(),
        gridVoteCount: (m['gridVoteCount'] as num? ?? 2).toInt(),
      );
}

class GuardianModelStatus {
  final bool legacyImported;
  final int legacyBytes;
  final bool nsfwImported;
  final int nsfwBytes;
  final bool genderImported;
  final int genderBytes;

  const GuardianModelStatus({
    required this.legacyImported,
    required this.legacyBytes,
    required this.nsfwImported,
    required this.nsfwBytes,
    required this.genderImported,
    required this.genderBytes,
  });

  factory GuardianModelStatus.fromMap(Map m) => GuardianModelStatus(
        legacyImported: m['legacyImported'] as bool? ?? false,
        legacyBytes: (m['legacyBytes'] as num? ?? 0).toInt(),
        nsfwImported: m['nsfwImported'] as bool? ?? false,
        nsfwBytes: (m['nsfwBytes'] as num? ?? 0).toInt(),
        genderImported: m['genderImported'] as bool? ?? false,
        genderBytes: (m['genderBytes'] as num? ?? 0).toInt(),
      );
}

class GuardianKeywordRule {
  final String keyword;
  final bool isRegex;
  final bool enabled;
  const GuardianKeywordRule({
    required this.keyword,
    this.isRegex = false,
    this.enabled = true,
  });

  Map<String, dynamic> toMap() => {
        'keyword': keyword,
        'isRegex': isRegex,
        'enabled': enabled,
      };

  factory GuardianKeywordRule.fromMap(Map m) => GuardianKeywordRule(
        keyword: m['keyword'] as String? ?? '',
        isRegex: m['isRegex'] as bool? ?? false,
        enabled: m['enabled'] as bool? ?? true,
      );
}

class GuardianBlockEvent {
  final int timestamp;
  final String packageName;
  final String reason;
  final String detail;
  const GuardianBlockEvent({
    required this.timestamp,
    required this.packageName,
    required this.reason,
    required this.detail,
  });

  factory GuardianBlockEvent.fromMap(Map m) => GuardianBlockEvent(
        timestamp: (m['timestamp'] as num? ?? 0).toInt(),
        packageName: m['packageName'] as String? ?? '',
        reason: m['reason'] as String? ?? '',
        detail: m['detail'] as String? ?? '',
      );
}

/// PIN verification outcomes.
sealed class GuardianPinResult {
  const GuardianPinResult();
}
class GuardianPinSuccess  extends GuardianPinResult { const GuardianPinSuccess();  }
class GuardianPinNotSet   extends GuardianPinResult { const GuardianPinNotSet();   }
class GuardianPinWrong    extends GuardianPinResult { final int remaining; const GuardianPinWrong(this.remaining); }
class GuardianPinLockedOut extends GuardianPinResult { final int msRemaining; const GuardianPinLockedOut(this.msRemaining); }

class GuardianService {
  GuardianService._();
  static final GuardianService instance = GuardianService._();

  static const _ch = MethodChannel('guraba.guardian');

  // ─── PIN ──────────────────────────────────────────────────
  Future<bool> isPinSet() async =>
      (await _ch.invokeMethod<bool>('isPinSet')) ?? false;

  Future<bool> setPin(String pin) async =>
      (await _ch.invokeMethod<bool>('setPin', {'pin': pin})) ?? false;

  Future<GuardianPinResult> verifyPin(String pin) async {
    final res = await _ch.invokeMethod<Map>('verifyPin', {'pin': pin});
    final status = res?['status'] as String? ?? 'wrong';
    switch (status) {
      case 'success': return const GuardianPinSuccess();
      case 'not_set': return const GuardianPinNotSet();
      case 'wrong':   return GuardianPinWrong((res?['remaining'] as num? ?? 0).toInt());
      case 'locked':  return GuardianPinLockedOut((res?['msRemaining'] as num? ?? 0).toInt());
      default:        return const GuardianPinNotSet();
    }
  }

  Future<void> clearPin() async => _ch.invokeMethod('clearPin');

  // ─── AI settings ──────────────────────────────────────────
  Future<GuardianAiSettings> getAiSettings() async {
    final m = await _ch.invokeMethod<Map>('getAiSettings');
    return GuardianAiSettings.fromMap(m ?? const {});
  }

  Future<void> setAiEnabled(bool v) => _ch.invokeMethod('setAiEnabled', {'value': v});
  Future<void> setUserGender(String v) => _ch.invokeMethod('setUserGender', {'value': v});
  Future<void> setAiThreshold(double v) => _ch.invokeMethod('setAiThreshold', {'value': v});
  Future<void> setNsfwGateThreshold(double v) => _ch.invokeMethod('setNsfwGateThreshold', {'value': v});
  Future<void> setGenderThreshold(double v) => _ch.invokeMethod('setGenderThreshold', {'value': v});
  Future<void> setGridVoteCount(int v) => _ch.invokeMethod('setGridVoteCount', {'value': v});

  // ─── Models ───────────────────────────────────────────────
  Future<GuardianModelStatus> getModelStatus() async {
    final m = await _ch.invokeMethod<Map>('getModelStatus');
    return GuardianModelStatus.fromMap(m ?? const {});
  }

  /// Imports a TFLite model file from a content:// or file:// URI.
  Future<bool> importModel({required String uri, required String modelName}) async =>
      (await _ch.invokeMethod<bool>(
        'importModel',
        {'uri': uri, 'modelName': modelName},
      )) ?? false;

  Future<bool> deleteModel(String modelName) async =>
      (await _ch.invokeMethod<bool>('deleteModel', {'modelName': modelName})) ?? false;

  // ─── Keyword rules ────────────────────────────────────────
  Future<List<GuardianKeywordRule>> getKeywordRules() async {
    final list = await _ch.invokeMethod<List<dynamic>>('getKeywordRules');
    return (list ?? const [])
        .whereType<Map>()
        .map((m) => GuardianKeywordRule.fromMap(m))
        .toList(growable: false);
  }

  Future<void> setKeywordRules(List<GuardianKeywordRule> rules) async =>
      _ch.invokeMethod('setKeywordRules', {
        'rules': rules.map((r) => r.toMap()).toList(growable: false),
      });

  Future<bool> evaluateText(String text) async =>
      (await _ch.invokeMethod<bool>('evaluateText', {'text': text})) ?? false;

  // ─── Block events ─────────────────────────────────────────
  Future<List<GuardianBlockEvent>> getBlockEvents() async {
    final list = await _ch.invokeMethod<List<dynamic>>('getBlockEvents');
    return (list ?? const [])
        .whereType<Map>()
        .map((m) => GuardianBlockEvent.fromMap(m))
        .toList(growable: false);
  }

  Future<void> clearBlockEvents() => _ch.invokeMethod('clearBlockEvents');

  Future<String?> exportBlockEventsCsv() async =>
      _ch.invokeMethod<String>('exportBlockEventsCsv');
}

/// Standard model file names — keep in sync with [ModelImportManager].
class GuardianModels {
  static const legacy = 'guardian_model.tflite';
  static const nsfw   = 'nsfw_model.tflite';
  static const gender = 'gender_model.tflite';
}
