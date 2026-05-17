# Build Fix Notes — Guraba

## Errors Resolved (from CI log `0_build-android.txt`)

CI `flutter analyze --no-fatal-infos` was failing with **8 errors** that
blocked the Android APK / AAB build. All have been fixed.

### 1. Database migration files — `gurabaSettingsTable` undefined on `Schema3` / `Schema4`

The schema getter in `lib/core/database/schemas/schema_versions.dart` is still
named `mindfulSettingsTable` (legacy name from before the "Mindful → Guraba"
rebrand). The migration files were calling the new name, which doesn't exist on
the old `Schema3` / `Schema4` shapes.

**Fixed files**
- `lib/core/database/migrations/from2To3.dart`
- `lib/core/database/migrations/from3To4.dart`

**Change applied** (sed-style):
```
schema.gurabaSettingsTable  →  schema.mindfulSettingsTable
```
(6 occurrences total — matches the 6 analyzer errors.)

### 2. Localization — `guraba_tagline` getter missing on `AppLocalizations`

The generated `lib/l10n/generated/app_localizations*.dart` files expose a
`guraba_tagline` getter (already aligned with the new brand), but the **source**
`.arb` files in `lib/l10n/` still used the old key `mindful_tagline`. On the
next `flutter gen-l10n` run, the generated getter would disappear and the two
usage sites would break.

**Fixed files** (all 14 locale .arb files updated):
```
"mindful_tagline":  →  "guraba_tagline":
```
- `lib/l10n/app_de.arb`, `app_el.arb`, `app_en.arb`, `app_es.arb`,
  `app_fr.arb`, `app_it.arb`, `app_ja.arb`, `app_pl.arb`, `app_pt.arb`,
  `app_sr.arb`, `app_tr.arb`, `app_uk.arb`, `app_vi.arb`, `app_zh.arb`

Now the .arb source and the generated localization classes are consistent,
and the two call sites
- `lib/ui/screens/settings/about/tab_about.dart:63`
- `lib/ui/splash_screen.dart:155`
will keep compiling on every regeneration.

---

## Known Issue (not blocking build, flagged for review)

Two migration files still issue raw SQL against the new table name even though
**Schema3 / Schema4 entityName is `'mindful_settings_table'`**:

- `from2To3.dart:29` — `SELECT excluded_apps FROM guraba_settings_table`
- `from3To4.dart:40` — `SELECT * FROM guraba_settings_table`

The current generated `app_database.g.dart` uses `guraba_settings_table` as the
runtime table name, so a fresh install will work. However, **existing users
upgrading from app versions that wrote `mindful_settings_table` may hit a
runtime migration failure**. If you want me to address this, the safe pattern is:

```dart
final tableName = (await m.database.customSelect(
  "SELECT name FROM sqlite_master WHERE type='table' AND name IN "
  "('guraba_settings_table','mindful_settings_table')"
).getSingleOrNull())?.read<String>('name') ?? 'guraba_settings_table';
```
…then interpolate `tableName` into the SELECT. Let me know if you'd like this
hardening applied.

---

## Verification

After the fixes:
- `grep "schema\.gurabaSettingsTable" lib/core/database/migrations/` → **0 matches** ✔
- `grep "mindful_tagline" lib/l10n/*.arb` → **0 matches** ✔
- `mindfulSettingsTable` getter confirmed present on `Schema3` (line 842) and
  `Schema4` (line 1163) in `schema_versions.dart`.

The 8 analyzer errors from the build log are all eliminated. The CI workflow
(`.github/workflows/*.yml`) is unchanged and should now reach the
`flutter build apk` / `flutter build appbundle` steps.
