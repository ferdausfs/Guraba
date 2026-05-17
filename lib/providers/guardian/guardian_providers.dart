/*
 * Guraba — Riverpod providers for the Guardian module.
 */
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:guraba/core/services/guardian_service.dart';

/// Live AI settings (re-fetched after every mutation).
final guardianAiSettingsProvider =
    FutureProvider<GuardianAiSettings>((ref) async {
  return GuardianService.instance.getAiSettings();
});

/// TFLite model import status.
final guardianModelStatusProvider =
    FutureProvider<GuardianModelStatus>((ref) async {
  return GuardianService.instance.getModelStatus();
});

/// Keyword rules.
final guardianKeywordRulesProvider =
    FutureProvider<List<GuardianKeywordRule>>((ref) async {
  return GuardianService.instance.getKeywordRules();
});

/// Block-event log.
final guardianBlockEventsProvider =
    FutureProvider<List<GuardianBlockEvent>>((ref) async {
  return GuardianService.instance.getBlockEvents();
});

/// Whether a PIN is currently set.
final guardianPinSetProvider = FutureProvider<bool>((ref) async {
  return GuardianService.instance.isPinSet();
});
