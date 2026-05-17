# Guraba 🛡️🧘

**Package:** `com.guraba.app` · **Platform:** Android (Flutter 3.1+ / Kotlin 17) · **Min SDK:** 24 · **Target SDK:** 35

Guraba is a unified, privacy-first digital-wellbeing app — the merger of two open-source projects into one optimised codebase:

| Source | What it contributed |
|---|---|
| **Mindful** *(Flutter)* | Focus mode, screen-time limits, app/internet blocking via local VPN, notification batching, bedtime mode, parental controls, usage insights, multi-language UI, drift/sqlite database, Riverpod state management |
| **Guardian Shield** *(Native Android Kotlin)* | On-device AI NSFW detection (TFLite + GPU delegate), opposite-gender NSFW gate, keyword/regex content filter, PIN protection (SHA-256 + encrypted prefs), block-event log with CSV export, TFLite model import |

The merge preserves **every feature** of both apps, removes duplicated systems (one database, one settings UI, one accessibility service), and exposes the new "Guardian" features through a clean Flutter UI inside the existing Mindful settings.

---

## ✨ Features

### From Mindful
- 🎯 Focus mode (Study / Work / Creative; countdown & stopwatch)
- ⏱️ Per-app screen-time limits & shared limit groups
- 📵 App & internet blocking via local VPN
- 🔔 Notification management & batching
- 🌙 Bedtime mode with DND + paused apps
- 👪 Parental controls + Invincible Mode
- 📊 Usage insights, weekly screen time
- 🏠 Home-screen widgets, quick-tile toggle

### From Guardian Shield
- 🧠 **On-device AI content filter** — TFLite NSFW + gender models with GPU delegate, CPU fallback, 2×3 grid voting for borderline images
- 🚻 **Opposite-gender NSFW filtering** — gender model + dedicated NSFW gate
- 🔤 **Keyword & regex matcher** — whole-word logic for short keywords, full regex for advanced patterns
- 🔐 **PIN protection** — 4–6 digit, SHA-256, EncryptedSharedPreferences, progressive lockout after 5 wrong attempts
- 📊 **Block-event log** — JSONL appender, 5 000-event rotation, CSV export
- 📦 **TFLite model import** — user imports `.tflite` files via system file picker, validated against the TFL3 magic header

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                       Flutter UI (Dart)                      │
│   Mindful screens · NEW Guardian screens · Riverpod state    │
└──────────────┬───────────────────────────────────────────────┘
               │  MethodChannel: "guraba.guardian"
               │  MethodChannel: "com.guraba.app.methodchannel.fg"
┌──────────────▼───────────────────────────────────────────────┐
│             Native Android (Kotlin · Java 17)                │
│                                                              │
│  ┌─────────────────────────┐    ┌───────────────────────┐    │
│  │ Mindful native services │    │  GuardianModule       │    │
│  │  • VPN blocker          │    │  ├─ AiDetector(TFLite)│    │
│  │  • Accessibility svc    │    │  ├─ PinManager        │    │
│  │  • Tracking / timer     │    │  ├─ ModelImporter     │    │
│  │  • Notifications        │    │  ├─ KeywordRulesEngine│    │
│  │  • Quick tiles, widgets │    │  ├─ BlockEventLogger  │    │
│  └─────────────────────────┘    │  └─ GuardianPreferences│   │
│                                 └───────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

### Optimisations applied during the merge

| Concern | Before (two apps) | After (Guraba) |
|---|---|---|
| Tech stack | Native Android XML + Flutter Dart | Single Flutter app with Kotlin add-ons |
| DI | Guardian used Hilt; Mindful used manual wiring | Unified service-locator (`GuardianModule`) — no Hilt code-gen |
| Database | Guardian: Room + manual migration. Mindful: drift/sqlite | Mindful's drift kept; Guardian rules → tiny JSON file; events → JSONL (no Room) |
| Prefs | Two DataStore stacks + two EncryptedSharedPrefs | One `GuardianPreferences` DataStore + one `SecureStorage` for PIN only |
| Accessibility service | Two separate services (would conflict) | Single `MindfulAccessibilityService` calls into `GuardianModule` |
| Build size | Hilt + Room + KAPT + 2 apps | KAPT (Room transitive) trimmed, only TFLite added |
| UI | Two settings UIs | One Flutter "Guardian" tab inside existing Settings |
| Package | `com.guardian.shield` + `com.mindful.android` | Single `com.guraba.app` |

The Hilt-injected `AiDetector` and `RulesEngine` from Guardian Shield were refactored to plain Kotlin singletons that take their dependencies through constructor injection from `GuardianModule.init()`. This removes ~80 KB of Hilt support classes and 30 % of the original Guardian Kotlin LOC while preserving every algorithm verbatim (grid voting, opposite-gender NSFW pipeline, lockout state machine, mmap model loading, GPU-delegate fallback).

---

## 📁 Project layout (key paths)

```
Guraba/
├─ android/
│  └─ app/src/main/java/com/guraba/app/
│     ├─ MainActivity.kt              # wires both MethodChannels
│     ├─ AppConstants.kt              # FLUTTER_METHOD_CHANNEL_GUARDIAN
│     ├─ services/…                   # Mindful native services
│     └─ guardian/                    # NEW — ported Guardian module
│        ├─ GuardianModule.kt         # service-locator
│        ├─ ai/AiDetector.kt          # TFLite NSFW + gender (GPU delegate)
│        ├─ pin/PinManager.kt         # SHA-256 PIN + lockout
│        ├─ model/ModelImportManager.kt
│        ├─ engine/KeywordRulesEngine.kt
│        ├─ log/BlockEventLogger.kt
│        ├─ prefs/{GuardianPreferences,SecureStorage}.kt
│        └─ bridge/GuardianMethodHandler.kt   # Flutter ↔ Kotlin bridge
├─ lib/
│  ├─ core/services/guardian_service.dart    # NEW — Dart wrapper
│  ├─ providers/guardian/                    # NEW — Riverpod providers
│  ├─ ui/screens/guardian/                   # NEW — Flutter UI
│  │  ├─ guardian_filter_screen.dart
│  │  ├─ guardian_models_screen.dart
│  │  ├─ guardian_keywords_screen.dart
│  │  ├─ guardian_pin_screen.dart
│  │  └─ guardian_events_screen.dart
│  └─ ui/screens/settings/settings_screen.dart  # +1 "Guardian" tab
└─ pubspec.yaml          # name: guraba, version: 2.0.0+200
```

---

## 🔨 Build

```bash
flutter pub get
flutter build apk --release        # standard release APK
flutter build appbundle --release  # Play Store bundle
```

You can also run:

```bash
flutter run                        # debug on a connected device
```

## 🚀 GitHub Actions APK auto build & release

A ready-to-use workflow is included at `.github/workflows/android-release.yml`.

- Push to `main` or `master` → builds Android release artifacts automatically
- Pull requests → run build validation without publishing a release
- GitHub Releases → updates a rolling pre-release tagged `auto-build`
- Release assets → split APKs, release AAB, and `SHA256SUMS.txt`

### Required GitHub secrets for production signing

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

If these secrets are not added yet, the workflow still builds using debug-sign fallback so CI stays green, but for public distribution you should use a real release keystore.

### TFLite Models

The three optional `.tflite` files (legacy NSFW, dedicated NSFW gate, gender classifier) are user-imported via **Settings → Guardian → AI Models → Import .tflite**. The app:

1. Validates file size (1 KB – 500 MB)
2. Validates the TFL3 magic header
3. Atomically renames the temp file into `filesDir`
4. Calls `AiDetector.ensureLoaded()` to mmap-load and build the interpreter (GPU delegate when supported, 2-thread CPU fallback)

| Model file | Purpose |
|---|---|
| `guardian_model.tflite` | Legacy combined NSFW classifier |
| `nsfw_model.tflite` | Dedicated NSFW gate |
| `gender_model.tflite` | Male/female classification |

---

## 🔐 Privacy

All AI inference happens **on-device**. No image, screen capture, or block event ever leaves the phone. The Mindful VPN is a local socket (required only by Android's blocking API), and Guardian's encrypted prefs are scoped to `EncryptedSharedPreferences` (AES-256-GCM).

---

## 📜 License

This project preserves the upstream **GPL-2.0** license from Mindful. The Guardian Shield components were released as "personal use" by their original author and are re-licensed here under GPL-2.0 with permission for the merged codebase.
