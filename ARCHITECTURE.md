# Guraba — Architecture & Merge Notes

This document explains how two upstream apps (Mindful, Guardian Shield) were
merged into a single optimised Flutter app.

## Decision matrix

| Question | Choice | Rationale |
|---|---|---|
| Base stack? | Flutter (from Mindful) | 273 Dart files vs. 120 Kotlin files; multi-platform-ready; Mindful's UI is more complete |
| DI framework? | Plain singletons | Hilt (Guardian) and Mindful's manual wiring are incompatible; KAPT cost is unjustified for ~10 services |
| Database? | drift/sqlite (from Mindful) + JSONL/JSON for Guardian-only data | Single Room+drift conflict avoided; Guardian's rules/events are small enough for JSON |
| Accessibility service? | One (Mindful's), Guardian features called as callbacks | Android allows only one AccessibilityService per app process effectively used at a time |
| TFLite? | Added to base | 3 deps, ~3 MB; required for AI NSFW filter |
| Package id? | `com.guraba.app` | Neutral, neither original |
| App name? | "Guraba" | New unified brand |

## Module boundaries

The Guardian Shield port lives entirely under
`android/app/src/main/java/com/guraba/app/guardian/`. It has **zero
dependencies** on Mindful code — it only depends on:
- AndroidX (DataStore, Security-Crypto)
- TensorFlow Lite
- Kotlin coroutines
- Timber (already a transitive dep via Mindful's gradle, but it's not used —
  swapped for Log.* or silent catches)

This makes the Guardian module reusable; any other Flutter app could drop
this directory into its `android/` source set and use the `guraba.guardian`
MethodChannel.

## Refactors during the port

| Original (Guardian Shield) | Reworked for Guraba |
|---|---|
| `@Singleton @Inject` Hilt classes | Plain Kotlin singletons created in `GuardianModule.init()` |
| `Room` `BlockEventDao` with migration | `BlockEventLogger` writes JSONL to filesDir, auto-rotates at 5 000 entries |
| `Room` `KeywordRuleDao` | `KeywordRulesEngine` writes JSON, reloaded via Flow |
| `Timber.e(t, …)` | silent `catch (_: Throwable)` (avoid Timber dep) |
| `GuardianAccessibilityService` | not ported — Mindful's `MindfulAccessibilityService` is the single accessibility entry point |

## Public API surface

The Flutter side talks to native through one MethodChannel: `guraba.guardian`.
See `GuardianService` in `lib/core/services/guardian_service.dart` for the
full Dart-side surface (PIN, AI settings, models, keyword rules, block log).

## Build outputs

- `flutter build apk --release` → `build/app/outputs/flutter-apk/app-release.apk`
- `flutter build appbundle --release` → `build/app/outputs/bundle/release/app-release.aab`

R8/ProGuard rules from upstream Mindful are kept; the TFLite GPU delegate
requires `-keep class org.tensorflow.lite.gpu.** { *; }` which is already
present in Mindful's `proguard-rules.pro`.
