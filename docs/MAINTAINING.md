# Maintaining QuickCards

QuickCards is a local-first Android card vault (`com.quickcards.app`, Jetpack Compose,
Material 3, `minSdk 26`). This guide is for anyone (person or coding agent) changing it.
Read it before touching code, together with `AGENTS.md` for the non-negotiable
constraints and the craft rules in section 3a.

**Source of truth, in priority order**

1. `prototype/v2.html` — the behaviour spec. Its inline script is the reference for
   number rules, validation messages, copy/reveal, lock timing, notification, backups and
   settings. Copy text is part of the contract: do not paraphrase it.
2. `prototype/quickcards-v2.css` — exact tokens, dimensions (CSS px = dp), motion.
3. `prototype/timeline.html` — `#formats`, `#backup`, `#rules`, `#config`, `#system`.
4. `DESIGN.md` — named rules and the Compose mapping. `PRODUCT.md` — product truth.
5. The shipped code and its tests. Where code and prototype disagree, stop and resolve the
   disagreement explicitly; do not silently pick one.

Paths below follow the module structure (`:domain`, `core/*`, `feature/*`,
`navigation/`). Keep the dependency rules in section 1 when adding or moving a file.

To see the prototype running: `cd prototype && python3 -m http.server 8765`, then open
`http://127.0.0.1:8765/v2.html` (or read the HTML).

---

## 1. Architecture map

### Modules and packages

```
                            +--------------------------------------+
                            | :domain  (pure Kotlin/JVM, no Android)|
                            | card number rules, network detection, |
                            | CardValidator, EntityValidator,       |
                            | EntityUsage, CardQuery/search/order,  |
                            | Monogram, AppLockPolicy,              |
                            | CopyWindowPolicy, SecurityConfirmation|
                            | backup planning/merge rules           |
                            +------------------+-------------------+
                                               ^
                                               | plain data + pure calls
                                               |
+----------------------------------------------+-------------------------------+
| :app                                                                         |
|                                                                              |
|  MainActivity -> core/ui/base/BaseActivity (FLAG_SECURE in release)          |
|  QuickCardsApplication (database, repository, settings singletons)            |
|                                                                              |
|  app/navigation/          QcApp.kt: NavHost, bottom bar/rail, transitions     |
|                                                                              |
|  core/                                                                        |
|    data/                  Room database, DAOs/entities, VaultRepository,      |
|                           AppSettings, BackupCodec/Snapshot/Importer          |
|    security/              EncryptionHelper, PanHasher, ExportCrypto,          |
|                           UnlockSession, BiometricAuthHelper, ClipboardHelper, |
|                           AppLockController                                   |
|    notifications/         CopyCompanionNotifier                               |
|    ui/                    theme (Color/Type/Shape/Dimens/QcIcons), QcDialog/   |
|                           QcOverlays/QcPickerSheet, LockGate, base activity,  |
|                           shared components                                   |
|                                                                              |
|  feature/                                                                     |
|    cards/                 Cards tab: HomeScreen + HomeViewModel, PassCard,     |
|                           search, filters, holder chips, pin/delete dialogs   |
|    cardform/              Add/Edit form: CardFormViewModel/Session/Model,      |
|                           field components, smart paste, tag sheet,           |
|                           discard dialog                                     |
|    manage/                Manage tab + list editors, EntityDialog, rename/   |
|                           delete-blocked dialogs                              |
|    archive/               Archived cards screen: passes, search, restore,     |
|                           delete; reached from Manage › Archived cards        |
|    settings/              Settings rows, radio dialogs and the Card list      |
|                           section (expired, expires soon, archive, notes)     |
|    lock/                  LockScreen (full-window app lock UI)                |
|    backup/                BackupHost: export/import review/erase dialogs     |
+------------------------------------------------------------------------------+
```

### Dependency rules (enforced by `ArchitectureGuardTest`)

| From | May depend on | Must not |
|---|---|---|
| `feature/*` | `core/*`, `:domain`, its own package | import another `feature/*` |
| `app/navigation` | any feature (the only cross-feature wiring exception) | hold feature business logic |
| `core/*` | `:domain`, other `core/*` | import `feature/*` |
| `core/ui` | theme/components + `:domain` | import DAOs or Room entities |
| `:domain` | Kotlin/JVM + test dependencies | Android, Compose, Room, `:app` |

UI code never imports a DAO or a Room entity directly. It goes through a ViewModel and
`VaultRepository`, which maps entities to the plain card models. Navigation passes ids and
lambdas, not repositories or screens from another feature.

### Data flow

```
READ
Room DAO Flow<Entity> -> VaultRepository (decrypt fields, map) -> Flow<VaultCard>
  -> feature ViewModel (StateFlow + settings + :domain query/order) -> Compose

WRITE
Compose field -> feature ViewModel -> :domain validator/session
  -> VaultCard -> VaultRepository (dedup HMAC, encrypt, map to Entity)
  -> Room DAO in a transaction -> Flow re-emits -> UI

COPY
PassField tap -> UnlockSession.requireUnlock(activity, reason)
  -> ClipboardHelper.copySensitive(...) -> snackbar
  -> if the field was the number: CopyCompanionNotifier.notifyCardCopied(...)

REVEAL
Hold 450 ms -> UnlockSession.requireUnlock(...) -> show full value with a 5 s drain
  -> hideReveals() on lock or timeout

LOCK
AppLockController (launch / idle / leave / Lock now)
  -> UnlockSession.lockNow(), close overlays (clipboard and notification keep their timers)
  -> LockScreen covers the app; LockGate/LocalAppLocked keeps every overlay shut

BACKUP
BackupHost -> BackupSnapshot.capture (repository + settings)
  -> BackupCodec.encode -> ExportCrypto.encrypt -> SAF file
Import -> decrypt -> BackupCodec.decode -> BackupPlanner.plan (pure)
  -> review -> BackupImporter.import (Room transaction) -> snackbar counts

SMART PASTE
TextField value change -> SmartPaste.apply (pure, feature/cardform)
  -> one event inserted 2+ characters? -> CardTextParser (:domain)
  -> number and expiry from the pasted/scanned lines
  -> CardFormViewModel.updateNumber/updateExpiry (same rules as typing)
  -> snackbar; junk text falls back to the field's digit filter
```

### One owner per concern

- **Room schema and encryption envelopes:** `core/data` + `core/security`.
- **Card-number truth:** `:domain` only (`CardNetwork`, `CardNumberRules`, default list).
- **User-visible copy:** the owning feature's `strings_*.xml`.
- **Colour, type, size and icon tokens:** `core/ui/theme` only.
- **Overlay windows:** `core/ui/components/QcDialog.kt` / `QcOverlays.kt` only.
- **App lock and overlay lifetime:** `core/security/AppLockController.kt`,
  `core/ui/LockGate.kt`, `feature/lock` (the lock screen) + the Qc wrappers.
- **Backup merge policy:** `:domain` (`BackupPlanner`), with `core/data/backup` adapters.
- **Legacy repair, expiry state and list/archive order:** `:domain`
  (`LegacyCardNormalizer`, `ExpiryStatus`, `CardListOrder`), applied by
  `core/data` (`LegacyRepair`, `VaultRepository`) and drawn by `feature/cards`.
- **Repair undo and the archive reason:** `core/data/repository`; the Edit
  screen only shows `repairsFor`/`undoRepair`, never the encrypted rows.

---

## 2. Security model

| Layer | Mechanism | Where |
|---|---|---|
| Sensitive card fields | Envelope encryption: a non-exportable Android Keystore AES key (KEK) wraps one random AES-256 data key (DEK, stored wrapped in `quickcards_vault_key` prefs, unwrapped once per process, never persisted in the clear). Fields are AES-256-GCM under the DEK in software (`v2:` prefix, random 12-byte IV, 128-bit tag), so a decrypt costs microseconds. Values without the prefix are pre-2.1 rows encrypted directly with the KEK; they still decrypt and `DataMaintenance.wrapUnderDataKey` re-wraps them at start-up (`EnvelopeMigrationTest`). A wrapped DEK that no longer unwraps means the KEK is gone: no new DEK is generated, so the vault reads as unavailable instead of restarting empty. | `core/security/EncryptionHelper.kt` |
| Holder / card bank / note metadata | `md1:` envelope around the same `v2:` ciphertext, idempotent (`encryptMetadata`). The `banks`, `card_types`, `networks`, `tags` tables and the legacy `cardVariant` label are plain (not card data); the bank name copied onto a card is encrypted. | `EncryptionHelper.kt` |
| Duplicate detection | Separate Keystore HMAC-SHA256 key over ASCII digits; unique index on `cards.panHash`. In a backup file the first copy of a number wins (`BackupPlanner.uniqueCards`, review shows the skipped count); on upgrade `DataMaintenance.removeDuplicateCards` deletes extra copies, keeping the most recently edited one and merging tags and a missing note (`DuplicateCleanupTest`) | `core/security/PanHasher.kt`, `Card` index |
| Repair log / archive reason | `card_repairs.before` and `.after` use `encrypt` (both are card data); `cards.archiveReason` uses `encryptMetadata`; never logged | `core/data/repository/LegacyRepair.kt`, `VaultRepository` |
| Key-loss detection | `core/security/KeySentinel.kt` keeps an encrypted sentinel in private prefs and checks it at start-up; when the Keystore key is gone, Cards shows the "Cards unavailable" dialog (`feature/cards/KeyUnavailableDialog.kt`) instead of rendering ciphertext. Covered by `KeySentinelTest`. |
| Export/import | PBKDF2-HMAC-SHA256, 600,000 iterations, 16-byte salt, AES-256-GCM, AAD-bound envelope header | `core/security/ExportCrypto.kt` |
| Authentication | `BiometricPrompt` with device-credential fallback (API 30+ combined, older `setDeviceCredentialAllowed`); fail-closed | `core/security/UnlockSession.kt` |
| Unlock window | One successful prompt; idle time only (touch/key/scroll/typing extends); no countdown shown | `UnlockSession`, `CopyWindowPolicy` |
| App lock | Launch, inactivity, leaving the app, Lock now; lock closes overlays and clears reveals; the clipboard and the notification keep their own timers so a paste into another app works | `core/security/AppLockController.kt` |
| Overlay lifetime | `QcDialog`/`QcModalBottomSheet`/`QcDropdownMenu`/`QcPopup` return nothing while `LocalAppLocked` is true; `trackAppLockInteraction()` feeds idle tracking; `SecureOverlayWindow()` applies `filterTouchesWhenObscured` and (API 31+) `setHideOverlayWindows` to each overlay window | `core/ui/components`, `core/ui/LockGate.kt` |
| Clipboard | Sensitive clip marked `EXTRA_IS_SENSITIVE` on API 33+; cleared after the chosen time, by Clear, or by a new copy. The value deliberately survives switching apps so it can be pasted. The timer dies with the process, so the deadline (a keyed HMAC-SHA256 token, never the value) is persisted and an overdue copy is cleared on the first focused read (onWindowFocusChanged). It is not cleared merely because the app went to the background. | `core/security/ClipboardHelper.kt` |
| Notification | `VISIBILITY_PRIVATE` with a public “Card details hidden” version; no actions; silent, low-importance channel (new id, the old channel is deleted on upgrade); **off by default for fresh installs**; a 2.3 upgrade with no stored key is migrated to on once; turning it on asks for a fresh unlock before the CVV is ever shown | `core/notifications/CopyCompanionNotifier.kt` |
| Screen capture | `FLAG_SECURE` on every activity in release (`BuildConfig.SECURE_WINDOW`); recents screenshot disabled on API 33+; debug deliberately allows QA screenshots | `core/ui/base/BaseActivity.kt` |
| Network | No `INTERNET` or `ACCESS_NETWORK_STATE` permission and no network calls; no backup of app data (`allowBackup=false`). The merged manifests are parsed by `OfflineManifestGuardTest` / `verifyOfflineManifest`. The EmojiCompat initializer is removed in the manifest so the default downloadable-font path cannot run. Play in-app updates (`com.google.android.play:app-update`, `core/update/AppUpdates.kt`) is the one Google library: it binds to the Play Store app over IPC, adds no permission, and fails quietly on non-Play installs. | `AndroidManifest.xml`, `app/build.gradle` |
| Privacy policy | Bundled as text in Settings › About, so it needs no WebView or network permission; the public copy is `docs/privacy.html` | `feature/settings/PrivacyScreen.kt`, `res/values/strings_privacy.xml` |

**Invariants**

- Never log or persist plaintext PAN, expiry, CVV, holder name, bank or note.
- Never add `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE` or `android.permission.CAMERA`; keep `verifyOfflineManifest` and `OfflineManifestGuardTest` green.
- Pasted or keyboard-scanned text fills only the number and expiry: never read a CVV or a holder name from it, and never store or log the pasted text.
- Never make a sensitive action succeed after a cancelled/failed prompt.
- Never store the export backup password. Clear backup password char arrays after use.
- New sensitive fields must use `encrypt`/`decrypt` (not metadata) unless they are
  explicitly non-secret.
- The lock screen must cover dialogs and sheets, not sit behind them. Back does nothing
  on the lock screen; only a successful prompt leaves it.
- Keystore aliases: AES `QuickCardsSecretKey`, HMAC `QuickCardsPanHashKey`. Changing or
  rotating either alias makes existing data unreadable; treat it as a data migration.

---

## 3. Build, test and device rules

Requirements: JDK 17 (the version CI uses), Android SDK 36.
Use `./gradlew` from the repository root.

```bash
./gradlew :domain:test                 # pure JVM rules
./gradlew :app:assembleDebug           # debug APK
./gradlew :app:testDebugUnitTest       # JVM tests
./gradlew :app:lintDebug               # lint must report no issues
./gradlew :app:assembleRelease         # R8 + shrink, needs keystore.properties
```

`:app:testDebugUnitTest` must end with `0 failures` and no skipped guard. A build warning
about Kapt falling back to Kotlin 1.9 for Room is expected; it is not a failure.

Where tests live:

- `domain/src/test/kotlin/...` — pure rules: `CardNumberRulesTest`, `CardValidatorTest`,
  `CardTextParserTest`, `LegacyCardNormalizerTest`, `ExpiryStatusTest`,
  `CardListOrderTest`, `EntityValidatorTest`, `EntityUsageTest`, `CardQueryTest`,
  `MonogramTest`, `AppLockPolicyTest`, `CopyWindowPolicyTest`,
  `SecurityConfirmationTest`, `BackupPlannerTest`.
- `app/src/test/java/com/quickcards/app/...` — `CardFormViewModelTest` (also covers
  review errors and Undo), `AppSettingsTest`, `BackupCodecTest`, `LegacyRepairTest`,
  `ImportSummaryTest`, `PassColorsTest`, `BandColorTest`, `BackupProtectionTest`,
  `SmartPasteTest`, `OfflineManifestGuardTest`, and the guard suite (section 6).
- `app/src/androidTest/java/com/quickcards/app/...` — `MigrationTest`,
  `BackupFlowTest`, `ArchiveRepositoryTest`, `PanHasherTest`, `VaultDedupTest`,
  `KeySentinelTest`, `PinOrderingTest`, `PassCardTest`, `CardFieldLimitsTest`,
  `SettingsCardListFlowTest`, `ImportFormatsSummaryTest`.

Add a JVM test at the lowest layer that can express the rule; use an instrumented test only
for Room, crypto/Keystore, Compose semantics or a real flow.

### Instrumented tests — device rule

`connectedDebugAndroidTest` and `installDebug` fan out to **every** connected device, so
they are not used in this project: they would install a test build on any phone that
happens to be attached.

```bash
# Correct: only the chosen test device
adb devices                       # pick the serial of a throwaway test device/emulator
export ANDROID_SERIAL=<serial>
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$ANDROID_SERIAL" shell am instrument -w -r \
  com.quickcards.app.test/androidx.test.runner.AndroidJUnitRunner
```

Do not run `connectedDebugAndroidTest` or `installDebug`, even with `ANDROID_SERIAL`
set: the task graph can still fan out. Install explicitly with `adb -s`.

The test device needs a screen lock (PIN or pattern) so BiometricPrompt's
device-credential fallback works; enter it with key events in automation. An API 26
emulator is the reference target for instrumented tests. Never run instrumented tests on
a phone that holds real card data.

---

## 3a. Craft rules

These alignment and layout rules apply to every screen. The prototype and
`prototype/reference/*.png` (index in its README) are the visual target; UI changes must
be compared against them in dark and light.

- 16dp screen gutters everywhere; passes 12dp apart; list sections 24dp apart.
- Every touch target ≥ 48dp. Icon buttons 48dp with 24dp icons.
- List rows: leading slot 56dp wide for icons, monograms and avatars (icons 24dp centred;
  monograms/avatars 40dp with 8dp side margin) and 56–128dp for a network badge that grows
  with its label, so **short-label row titles start at the same x**.
- Text fields: M3 `OutlinedTextField`, 56dp, 4dp corners, floating label, focus = 2dp
  onSurface outline (no colour), **helper line always reserved** (supportingText always
  present, even empty) so errors never shift layout; character counters right-aligned in
  the supporting line.
- **Edge alignment:** a field's supporting line (helper, error, character counter) aligns
  flush with the outline edges (0dp inset): helper/error text starts at the outline's left
  edge and the counter ends at its right edge, for every input (forms, dialogs, settings).
  Group labels above controls ("Tags", "Card colour", "CVV digits") and group counters
  ("1 of 3") also align to the outline edges. Section subheads align with list text.
  Checkbox and radio glyphs share one 20dp slot so their labels start at the same x.
- Errors: outline + label + supporting text in `error`, message text exactly as in
  `#rules`; focus moves to the first invalid field on save.
- **Error colouring:** while a field is in error, its outline, floating label, supporting
  text and counter are all `error` colour, **including while focused** (use
  `OutlinedTextField(isError = true)` and never override label/outline colours in error).
- **Validation timing:** required-field errors appear only after a save attempt or once
  the user has typed something; leaving an untouched empty field shows no error.
- **Caret:** when a field is focused automatically (dialog opens, edit screen opens, error
  focus), place the caret at the **end** of existing text
  (`TextFieldValue(selection = TextRange(text.length))`). A user tap keeps the tapped
  position.
- Nothing overlaps: no FAB (Add is an app-bar action); sticky footers and sheets respect
  navigation-bar and IME insets; the last list item fully clears the bars.
- At most one red (`primary` #C8202B) element per screen. Selection controls use
  `secondary` (neutral), never `primary`.
- Tabular figures (`fontFeatureSettings = "tnum"`) on every number.
- **Empty state buttons:** red filled "Add card" with the add icon and a tonal "Import"
  with the download icon, equal width (see `prototype/reference/25-empty-state.png`).
- **No orphan wraps:** keep list subtitles to one line where the prototype does.

### Release signing

`app/build.gradle` reads a git-ignored `keystore.properties` at the repository root:

```properties
storeFile=keystore/quickcards-release.jks
storePassword=...
keyAlias=quickcards
keyPassword=...
```

`keystore/README.md` documents custody and rotation. Never commit a `.jks`, a password or
`keystore.properties`. Debug builds intentionally allow screenshots for visual QA; never
distribute a debug build.

### Google Play release

The Play upload is an `.aab` (`./gradlew :app:bundleRelease`) built with `targetSdk 36`, a
public privacy policy plus the bundled in-app copy, and a complete Data safety form. See
`docs/PLAY_RELEASE.md` for the Console answers and the tester plan. Play App Signing is
mandatory for new apps; the legacy key described in `keystore/README.md` is compromised and
must never be uploaded.

---

## 4. Current facts

### Permissions

| Permission | Why | Notes |
|---|---|---|
| `android.permission.USE_BIOMETRIC`, `USE_FINGERPRINT` | App lock and sensitive actions | Device-credential fallback |
| `android.permission.BIND_AUTOFILL_SERVICE` (on the service, not requested) | `core/autofill/QuickCardsAutofillService` offers "Search all cards" plus up to five cards (`AutofillChoice`: pinned, recent use from `CardUsage` ids and times, list order). Values are only decrypted after a fresh prompt, in `AutofillAuthActivity` (one card) or `AutofillPickerActivity` (search list, cancelled when the user leaves it). Type-to-narrow filters carry only the first six and last four digits. `onSaveRequest` is a no-op | Off via Settings › "Fill cards in other apps" |
| `android.permission.NFC` (feature not required) | `core/nfc/NfcCardReader` reads PAN and expiry from a contactless chip while the Add-card "Tap your card" sheet is open; the CVV is never on the chip | Settings › "Add cards by tapping"; the row is hidden without NFC hardware |
| `android.permission.RECEIVE_BOOT_COMPLETED` | `core/reminders/BootReceiver` re-arms the daily reminder alarm after boot, an app update, or a clock/time-zone change | Each reminder has a Settings switch |
| `android.permission.POST_NOTIFICATIONS` | Copy companion notification (off by default; API 33+) | Text is private on the lock screen |
| `android.permission.HIDE_OVERLAY_WINDOWS` | Biometric prompt hardening (API 31+) | No runtime prompt |
| `android.permission.INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA` | — | **Never present.** No network or camera dependency is declared; `OfflineManifestGuardTest` and `verifyOfflineManifest` parse the merged manifests |

The app is camera-free by design: an earlier in-app scanner and its CameraX/ML Kit
payload were removed. Card text comes from typing (number pad), the clipboard or the
NFC reader instead.

### Database

- Room database `quickcards_database`, **version 14**, `exportSchema = true`.
- Schema JSON: `app/schemas/com.quickcards.app.core.data.database.QuickCardsDatabase/<version>.json`.
- Tables: `cards`, `banks`, `holders`, `networks`, `card_types`, `tags`, `card_tags`,
  `card_repairs`.
- Migrations shipped: `MIGRATION_6_7` (no-op), `MIGRATION_7_8` (add `panHash` + unique
  index), `MIGRATION_8_9` (rebuild `cards`, drop the v1 `tags` table), `MIGRATION_9_10`
  (managed-list tables, card columns, seed networks/types, order backfill),
  `MIGRATION_10_11` (add `pinned`, `pinnedAt`), `MIGRATION_11_12` (add `archived`,
  `archivedAt`, `archiveReason`, `needsReview`; create `card_repairs`),
  `MIGRATION_12_13` (add `dueDay`), `MIGRATION_13_14` (add `networks.cvvOptional`, on for
  built-in Maestro). No `fallbackToDestructiveMigration`.
- `cards` archive/review columns: `archived INTEGER NOT NULL DEFAULT 0`,
  `archivedAt INTEGER`, `archiveReason TEXT` (`encryptMetadata`),
  `needsReview INTEGER NOT NULL DEFAULT 0` (bitmask 1 number, 2 expiry, 4 CVV).
- `card_repairs`: `id` autoincrement, `cardId` (FK to `cards`, cascade),
  `field` (`NUMBER`/`EXPIRY`/`CVV`), `before`/`after` (`EncryptionHelper.encrypt`),
  `at`; indexed on `cardId`. Rows older than 30 days are pruned at app start and
  the Edit screen offers Undo inside that window.
- `LegacyRepair` runs once after `LegacyBackfill` (guarded by
  `quickcards_repairs` prefs), normalizes legacy number/expiry/CVV values, writes
  the repair log and records the dismissible `RepairSummary`. It is idempotent and
  never logs a value.
- The schema directory is also an `androidTest` asset so `MigrationTestHelper` can open
  old versions.

### Backup

- Payload format **2** (`BACKUP_FORMAT = 2`, `BACKUP_APP_NAME = "QuickCards"`); the
  encrypted envelope has its own version **1** (`ExportCrypto.VERSION`). Do not conflate
  the two.
- Format 2 JSON: `meta { app, format, version, exportedAt }`, then `holders`, `banks`
  (`id,name,mono`), `types`, `tags`, `networks`
  (`id,name,code,prefixes,lengths,cvv,luhn,groups`), `cards`, and optional `cfg` (present
  when “Include settings” is ticked, which is the default).
  Card keys: `id, holderId, bankId, networkAuto, networkId, typeId, variant, number, exp,
  cvv, nameOnCard, color, note, tagIds, sortOrder, pinned, pinnedAt` and the
  optional archive/review keys `archived, archivedAt, archiveReason, needsReview`.
  Older files lack the optional keys and import with `archived=false`,
  `archivedAt=null`, `archiveReason=""`, `needsReview=0`.
- Every import (format 1 and 2) runs `LegacyCardNormalizer` before the review and
  inside the merge. Fixed fields write the same encrypted `card_repairs` rows as
  the upgrade job and set `needsReview`; the review exposes `formatsFixed` and
  `needCheck`, and the Cards banner is recorded through `RepairSummaryStore`.
- Envelope: `quickcards-export`, PBKDF2WithHmacSHA256 / 600,000 iterations, AES-256-GCM,
  salt/iv/ciphertext Base64; AAD is
  `quickcards-export|1|PBKDF2WithHmacSHA256|600000|<salt>|AES-256-GCM|<iv>`.
- Format 1 is a bare JSON array of the old Room `Card` rows (plaintext after opening).
  It remains importable forever: owner -> holder (`me` -> `Me`), `cardType` -> type,
  `cardIssuer` -> network by name, `cardColor` -> nearest band, `description` -> note.
- Import matching: cards by normalized digits across all holders; holders, banks, types
  and tags by name ignoring case (the phone's item is reused); networks by name (the
  phone's rules win); settings replace the phone's values when “Restore settings” is
  ticked. Existing cards are updated or kept as chosen; new cards are appended.
- Never included: unlock state, clipboard, search/filters, backup password, Keystore keys.
- File name: `quickcards-backup-YYYY-MM-DD.qcb`. Export is the only way data leaves the
  app.

### Settings

`AppSettings` is the single registry. Each setting has a SharedPreferences key, a
`AppSettingsDefaults` entry (default + allowed values), a `StateFlow`, a validating
setter, a camelCase key in `toMap()`, and a matching `applyMap()` branch. The backup
`cfg` uses the camelCase keys; `BackupSettings.defaultMap()` must list every setting so a
restore fills missing keys with defaults, not the phone's current values.

| Section | Setting | Options | Default | Backup key |
|---|---|---|---|---|
| Appearance | Theme | System default / Light / Dark | Dark | `theme` |
| Cards | Hide card numbers | On / Off | On | `maskNumbers` |
| Card list | Expired cards | Keep in place / Move to the end / Hide from the list | Move to the end | `expiredCards` |
| Card list | Expires soon | Within 1 / 2 / 3 months | Within 1 month | `expiresSoonMonths` |
| Card list | Show notes on cards | On / Off | On | `showNotes` |
| Copying | Clear clipboard | Never / 30 s / 1 min / 2 min | 1 min | `clipClearSec` |
| Card list | Add cards by tapping (NFC phones only) | On / Off | On | `nfcAddEnabled` |
| Reminders | Expiry reminders | On / Off | On | `expiryReminders` |
| Reminders | Payment reminders | On / Off | On | `paymentReminders` |
| Copying | Show expiry and CVV after copying (turning on asks to unlock) | On / Off | Off (fresh); On for a migrated 2.3 install | `notifEnabled` |
| Copying | Notification time | 15 s / 30 s / 1 min | 30 s | `notifSec` |
| Copying | Fill cards in other apps | On / Off (a restore can only turn it off) | On | `autofillEnabled` |
| Tags | Show tags on cards | On / Off | On | `tagsShow` |
| Security | App lock | On / Off | On | `appLockEnabled` |
| Security | Lock after inactivity | 10 s / 30 s / 1 min / 2 min / 5 min / 10 min / 30 min / Never | 1 min | `appLockAfterSec` |
| Security | Lock when you leave the app | On / Off | On | `lockOnLeave` |
| Security | Instant copy window | 30 s / 1 min / 2 min | 1 min | `copyWindowSec` |
| Security | Lock now | action | — | — |
| Backup | Include settings (export) | On / Off | On | — |
| Backup | Saved cards (import) | Update with the backup's details / Keep what's on this phone | Update | — |
| Backup | Restore settings (import) | On / Off | On | — |
| Backup | Erase all data | needs “I understand” | — | — |

SharedPreferences file: `quickcards_settings`. Internal keys are `theme_mode`,
`copy_window_sec`, `clip_clear_sec`, `mask_numbers`, `notif_enabled`,
`notif_sec`, `tags_show`, `expired_cards`, `expires_soon_months`, `show_notes`,
`app_lock_enabled`, `app_lock_after_sec`, `lock_on_leave`, `autofill_enabled`, `nfc_add_enabled`,
`expiry_reminders`, `payment_reminders`. Do not rename them casually: they
are existing users' data. Older backups may still carry `pinnedMax`, `tagsMax`, `tagsMaxLen`,
`showCardType`, `archivedCards`, `tagsSearch`, `notifShowExpiry` or `notifShowCvv`;
`AppSettings.applyMap()` ignores unknown keys, so they import silently, except that a false
`notifShowExpiry` or `notifShowCvv` turns the merged notification off (the same rule migrates
the old prefs on first launch). Reminder bookkeeping lives in `quickcards_reminders` (keys hold
card ids, due months and a keyed hash of the expiry, never the expiry) and is cleared by Erase all. Pinned cards are capped
at 10, tags at 10 per card and 16 characters, search always includes tag names, archived cards
always join search, and a card type always shows when set.

Security-sensitive reversals require a prompt before changing:
`SecurityConfirmation.reasonForAppLock(false)`, `reasonForInactivity(0)`,
`reasonForLockOnLeave(false)`.

### Default card networks

Seeded on a fresh install and by `MIGRATION_9_10`. Numbers always auto-detect by the
longest matching prefix; there is no network field or manual override.

| Name | Label | Starting digits | Lengths | CVV | Luhn | Grouping |
|---|---|---|---|---|---|---|
| Visa | VISA | `4` | 13, 16, 19 | 3 | yes | length default |
| Mastercard | MC | `51-55, 2221-2720` | 16 | 3 | yes | length default |
| American Express | AMEX | `34, 37` | 15 | 4 | yes | 4-6-5 |
| RuPay | RUPAY | `508500-508999, 606985-607984, 608001-608500, 652150-653149, 817200-820199` | 16 | 3 | yes | length default |
| Diners Club | DINERS | `300-305, 36, 38, 39` | 14-19 | 3 | yes | 4-6-4 |
| Discover | DISC | `6011, 644-649, 65` | 16-19 | 3 | yes | length default |
| JCB | JCB | `3528-3589` | 16-19 | 3 | yes | length default |
| Maestro | MAESTRO | `5018, 5020, 5038, 5893, 6304, 6759, 6761-6763` | 12-19 | 3 | yes | length default |
| UnionPay | UPI | `62` | 16-19 | 3 | **no** | length default |
| Mir | MIR | `2200-2204` | 16-19 | 3 | yes | length default |

Default grouping by length when the network has no custom grouping: 12 `4-4-4`,
13 `4-4-5`, 14 `4-6-4`, 15 `4-6-5`, 16 `4-4-4-4`, 17 `4-4-4-5`, 18 `4-4-4-6`,
19 `4-4-4-4-3`. While typing, group 4-4-4-4 up to 16 digits and settle into the exact
grouping on blur. A number with no matching network shows no badge and is checked as 12-19
digits + Luhn.

### Managed lists and validation

- Five lists: holders, banks, networks, types, tags. Names trimmed, spaces collapsed,
  required, at least 2 characters, unique per list ignoring case. Limits: holders and
  banks 40, networks 24, types 20, tags 16. A leading `#` is removed from tags.
- Network labels are 2-15 uppercase letters, digits or spaces and unique; starting digits
  accept digits, ranges and commas; lengths must stay within 12-19.
- Renaming an item that cards use, or changing a network's rules, opens “Update N cards?”
  with the affected cards. Deleting an item in use is blocked until those cards are gone;
  “Show cards” opens the list filtered to exactly those cards.
- Card rules: holder, number, expiry, CVV and bank required; type, tags and note optional
  (note max 80). Number length must match the detected network; Luhn unless the network
  disables it; duplicate check across all holders. Expiry MM/YY or MM/YYYY, stored MM/YY,
  not past, at most 20 years ahead. CVV exactly the network's length (3 or 4 when
  unknown). Exact messages live in `#rules` and the feature string resources.

---

## 5. Recipes

Every recipe ends with the same checks unless it says otherwise:

```bash
./gradlew :domain:test :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For a behaviour change, also add/extend an instrumented test and run it on one chosen
test device only (section 3), then update the prototype/docs in the same change
(recipe 5.10).

### 5.1 Add or rename a user-visible string

**Files**

- `app/src/main/res/values/strings_common.xml` for shared words (app name, tabs, generic
  actions, the tag chips and the biometric unlock title/cancel). `strings.xml` holds only
  `app_name`. Otherwise the owning feature's file: `strings_cards.xml`,
  `strings_cardform.xml` (the card-form feature), `strings_manage.xml`,
  `strings_settings.xml`, `strings_lock.xml`, `strings_backup.xml`,
  `strings_notifications.xml`. Keep one file per feature; never duplicate keys.
- Every call site in that feature (`feature/<feature>/**`), or `core/notifications` for
  notification text.
- `app/src/test/java/com/quickcards/app/core/ui/components/HardcodedStringGuardTest.kt`
  and its allow-list file only if the literal is intentionally non-linguistic (for example
  `•`); never add prose to the allow-list.

**Steps**

1. Pick the feature's prefix: `cards_`, `form_`, `manage_`, `settings_`, `lock_`,
   `backup_`, `notif_`; accessibility strings use `a11y_`.
2. Add the string with a semantic key and the exact English copy from the prototype.
   Use positional placeholders (`%1$s`, `%1$d`) and `<plurals>` for counts; never build a
   sentence by concatenation.
3. Replace the literal with `stringResource(R.string.key)` in Compose,
   `pluralStringResource(...)` for counts, or `context.getString(...)` outside Compose.
4. Rename keys only with every call site in the same change; remove the old key. Lint's
   `UnusedResources` must stay clean.
5. Domain validators must return a typed error (sealed class / enum with arguments), not
   English text. Map that error to the string in the feature layer. Tests assert the
   typed error, not the copy. If a validator still returns English, convert it as part of
   the change and keep the English byte-for-byte.

**Tests:** `HardcodedStringGuardTest`; `:app:lintDebug`; the feature's existing UI test
or screenshot for the affected screen; `:domain:test` if a validator changed.

### 5.2 Change a colour, size, shape or type style

**Files**

- Colour: `app/src/main/java/com/quickcards/app/core/ui/theme/Color.kt` (all `Color(...)`
  values, `PassColors`, `QcColors`) and `Theme.kt` (Material role mapping). Never put
  `Color(0x...)` in a feature file.
- Size/spacing/radius: `core/ui/theme/Dimens.kt`. Move a repeated literal there once;
  do not create a parallel constant in a feature.
- Shape: `core/ui/theme/Shape.kt` (`Shapes`, `QcShapes`).
- Type: `core/ui/theme/Type.kt` (`QcType`; keep `fontFeatureSettings = "tnum"`).
- Icons: `core/ui/theme/QcIcons.kt`; never import a `material-icons` vector directly in a
  feature.
- `DESIGN.md` tokens/prose, `prototype/quickcards-v2.css` and `prototype/timeline.html`
  when the change is a design decision (recipe 5.10).

The core `Dimens` values are: gutter 16dp, pass gap 12dp, section gap 24dp, touch target
48dp, list lead slot 56dp, one-line row 56dp, two-line row 72dp, field height 56dp, helper
line 20dp, pass band 48dp, pass corner 18dp, notch 9dp, dialog/sheet corner 28dp,
notification corner 24dp, field/menu/snackbar corner 4dp, badge corner 6dp, chip/tag
corner 8dp, copy-target state layer 10dp, monogram corner 12dp, app-mark corner 22dp.
Add one name there rather than repeating a literal. `QcShapes`/`Shapes` should consume the
`Dimens` radii, and `PassColors`/`Color.kt` should own the brand and band colours, so the
pass geometry and palette exist exactly once.

**Steps**

1. Check `DESIGN.md` first. The pass bands, brand red and pass geometry are defined
   exactly once; a second source is a bug.
2. Add or change a named token, then use it through `MaterialTheme.colorScheme`,
   `QcColors`, `PassColors`, `Dimens` or `QcType`. No ad-hoc colours, text styles or
   design dp.
3. Keep selection controls on `secondary`/`secondaryContainer`; brand red is only the one
   filled action per screen.
4. Update the reference screenshot if the change is intentional and approved; compare
   dark and light.

**Tests:** `ColorGuardTest` (no colour literals outside theme), `:app:lintDebug`, visual
comparison against `prototype/reference/`. If a pass-band colour changes, also run
`PassColorsTest`.

### 5.3 Add a setting

**Files**

1. `core/data/settings/AppSettings.kt` — one registry entry: default, allowed values,
   SharedPreferences key, camelCase backup key, `StateFlow`, validating setter,
   `toMap()` and `applyMap()`.
2. `core/data/backup/BackupSettings.kt` — add the default to `defaultMap()` so old
   backups get the default, not the phone's value.
3. `feature/settings/SettingsScreen.kt` — a `SwitchRow` or `ValueRow` under the right
   sentence-case subhead, with the right icon, supporting text and enabled state.
4. `feature/settings` strings (`strings_settings.xml`) for the title, supporting text,
   dialog title/body and option labels.
5. If the setting weakens security (turns a lock off, allows “Never”, exposes the CVV),
   add a reason to `:domain/SecurityConfirmation.kt` and call
   `UnlockSession.requireUnlock(host, reason)` before applying.
6. If the setting affects a screen, collect it in that feature's ViewModel and pass the
   value down; do not read `AppSettings` from a composable deep in the tree.

**Notes**

- Use the one `secText`/`TimeText` helper for “30 seconds”, “1 minute”, “Never”; do not
  duplicate duration wording.
- Dependent rows disable at 38% when the parent is off.
- A setting that changes a live screen must keep that screen's scroll position.

**Tests:** `AppSettingsTest` (default, invalid value rejected, apply/round-trip);
`BackupCodecTest` or a `BackupSettings` test for missing-key defaults; a settings UI test
if the row has a special interaction; `SecurityConfirmationTest` if a prompt was added.

### 5.4 Add a card field

This is the widest change in the app. Do it in this order so no layer is forgotten.

**Data and domain**

1. Add the field to the Room entity
   `core/data/model/Card.kt` with a SQL default if it is non-null. Add it to the plain
   model (`VaultCard` / the `:domain` card model).
2. Change the database (recipe 5.8): version + migration + schema + `MigrationTest`.
3. Map it in `VaultRepository` in both directions (`toEntity`, `toDomain`, insert,
   update, restore, duplicate/backup paths). Encrypt it with
   `EncryptionHelper.encrypt/decrypt` if it is card data; use
   `encryptMetadata/decryptMetadata` for holder/bank/note-like metadata. Never store a
   new secret in plaintext.
4. If it participates in search or filters, add it to `:domain/CardQueryEntry` and
   `CardQuery`, then map it in the home ViewModel.
5. If it is a user-editable card value, add it to `CardDraft`/`CardValidator` (typed
   error), `CardFormModel`, `CardFormSession` (dirty comparison and field updates),
   `CardFormViewModel.save()`, and `CardFormScreen` (field + helper line). Use
   `CardFieldInput` for any filtering/formatting and write the filtered
   `TextFieldValue` back, caret at the end.

**Presentation and copy**

6. If it is shown on the pass, add it to `PassCard`/`PassField`. If it can be copied,
   add its copy label and the value to the copy path; the label/field must respect the
   masking rules and the unlock window.
7. If it appears in the cards list, keep row titles aligned (56dp lead slot) and use
   `QcType`/`Dimens`.

**Backup and import**

8. Add the field to `BackupCard` (`BackupModels.kt`), `BackupCodec.encode` and `decode`
   (with a default for files that lack it), `BackupSnapshot.capture` and
   `phoneSnapshot`, `BackupPlanner.resolve` and `differs`, and
   `BackupImporter.toEntity`. Old formats must keep importing.
9. If the value is part of the backup format's meaning, bump the format (recipe 5.9);
   adding an optional field with a default normally stays format 2.

**Tests**

- `:domain`: `CardValidatorTest` (rules/messages), `CardQueryTest` (search/filter),
  `CardNumberRulesTest` if the field affects grouping/masking.
- JVM: `CardFormViewModelTest` (dirty, limits, save), `BackupCodecTest` (round-trip and
  a file without the new key), `BackupPlannerTest` (change detection).
- Instrumented: `MigrationTest` (old version -> new), `BackupFlowTest` (export -> erase ->
  import), and a form UI test for the new input/limit.

### 5.5 Add or edit a default card network

**Files**

- `domain/src/main/kotlin/com/quickcards/domain/CardNetwork.kt` (`DefaultNetworks`) — the
  pure default list used for validation/detection.
- `app/src/main/java/com/quickcards/app/core/data/model/SeedData.kt` — the list seeded
  into `networks` on a fresh install and by `MIGRATION_9_10`. Keep the two identical.
- A new Room migration if existing installs need the row or rule change (recipe 5.8).
  New networks: `INSERT OR IGNORE` the row with a stable id, `builtIn = 1`,
  `sortOrder` after the current last. Changed built-in rules: update only rows that still
  match the old default, so a user's deliberate edit in Manage is not overwritten; any
  other policy needs an explicit, documented product decision.
- `BackupModels.kt`/`BackupCodec.kt` only if the network schema itself changes. The
  backup already carries all network fields.
- `prototype/v2.html` (`NETWORKS0`), `prototype/timeline.html` (`#formats`),
  `DESIGN.md` (“Card Networks”), `PRODUCT.md`.

**Steps**

1. Decide the exact name, 2-15 character label, prefixes (digits and ranges), allowed
   lengths (12-19), CVV length (3/4), Luhn flag and optional grouping.
2. Add it to both default lists in the same order, with a stable id and the next
   `sortOrder`.
3. Make sure a new prefix cannot steal detection from a more specific existing network.
   Detection picks the longest matching prefix; equal scores keep the first list entry.
4. Add a migration for existing installs; bump the DB version and export the schema even
   if the table shape is unchanged, because the data migration still needs a version.
5. Update the prototype formats table and docs.

**Tests**

- `CardNumberRulesTest`: detection, prefix score, lengths, CVV, Luhn, grouping,
  formatting/masking for the new network.
- `MigrationTest`: the network row exists after the step and existing cards/lists are
  untouched.
- `BackupCodecTest`: network fields round-trip; `BackupFlowTest` if import/export is
  affected.
- `:domain:test` and a manual add-card check (the badge and CVV helper must follow the
  new network).

### 5.6 Add a dialog, bottom sheet, menu or popup

**Files**

- Reuse `core/ui/components/QcDialog.kt` (`QcDialog`), `QcOverlays.kt`
  (`QcModalBottomSheet`, `QcDropdownMenu`, `QcPopup`). `QcPickerSheet` is the shared
  picker built on `QcModalBottomSheet`; use it for holder/bank/type selection.
- Feature screen that owns the open/closed state.
- `strings_<feature>.xml`.

**Rules**

1. Never call `Dialog`, `AlertDialog`, `ModalBottomSheet`, `DropdownMenu`,
   `ExposedDropdownMenu` or `Popup` directly. `OverlayGuardTest` fails the build. The only
   allowed constructors are in `QcDialog.kt` and `QcOverlays.kt`.
2. The wrappers already check `LocalAppLocked` and call `trackAppLockInteraction()`. Do
   not add a second lock check; do close your state on `LaunchedEffect(locked)` if the
   state would otherwise survive and reopen after unlock (the backup host does this).
3. Dialog frame: `surfaceContainerHigh`, 28dp corners, 24dp padding, max 360dp, actions
   right-aligned text buttons. Sheet: `surfaceContainer`, 28dp top corners, handle,
   Headline title, optional footer above a hairline. Menu: `surfaceContainerHigh`, 4dp
   corners, 48dp rows, neutral check.
4. Use the reserved helper line for every field; helper/error text starts at the outline
   edge and a counter ends at the outline edge. Focus moves to the first invalid field.
5. When auto-focusing, place the caret at the end of existing text.
6. If you add a new overlay *type*, add a wrapper in `QcOverlays.kt`/`QcDialog.kt` and
   register that wrapper file/token in `OverlayGuardTest`; never allow-list a feature.
7. Add a test tag/content description for the new overlay only when a test needs it.

**Tests:** `OverlayGuardTest`; an instrumented lock test that opens the overlay and taps
Lock now (it must disappear); screenshot check in dark and light.

### 5.7 Add a screen, route or feature package

**Files**

- `feature/<feature>/<Name>Screen.kt` and `<Name>ViewModel.kt` (one screen + its
  ViewModel per file). Private section composables stay in the same package.
- `app/src/main/java/com/quickcards/app/navigation/QcApp.kt` — route constant,
  `composable(...)`, argument wiring, and `RootRoutes`/`NavigationItems` only for a new
  root tab.
- `core/ui/theme/QcIcons.kt` for a new icon; `strings_common.xml` for a tab label.
- `res/values/strings_<feature>.xml`.

**Steps**

1. Keep the screen in its feature. It may import `core/*` and `:domain`, never another
   feature. Pass navigation lambdas from `QcApp`; use `navController.navigate(...)` only
   in the screen or in `QcApp` wiring.
2. Nested screens hide the bottom bar automatically because `isRoot(route)` is false; add
   `bottomInset`/system-bar padding and a footer above a hairline. Root tabs keep the bar
   and rail at 600dp+.
3. Load data through the feature ViewModel -> `VaultRepository`. Do not use a DAO or Room
   entity in the screen.
4. Use `QcType`, `Dimens`, `MaterialTheme` and `QcIcons`; all user text via resources.
5. If the screen pushes another screen, use the shared-axis 280ms transition and preserve
   scroll position on tab switches.
6. Add a test tag/semantics for automation; do not encode state in a way that breaks
   process death (`SavedStateHandle` for ids).

**Tests:** `ArchitectureGuardTest` (dependency scan), an instrumented navigation/back
test, and a screenshot against the reference. If the screen has a ViewModel, add a JVM
test with fakes for its state transitions.

### 5.8 Change the database

**Files**

- `core/data/database/QuickCardsDatabase.kt` — increment `version`, add
  `val MIGRATION_N_N1 = object : Migration(N, N + 1) { ... }`, add it to
  `.addMigrations(...)`.
- Entity/DAO files under `core/data/model`, `core/data/dao`.
- `app/schemas/com.quickcards.app.core.data.database.QuickCardsDatabase/<newVersion>.json`
  (generated by the build; commit it).
- `app/src/androidTest/java/com/quickcards/app/core/data/database/MigrationTest.kt`.

**Rules**

1. Migrations are **non-destructive**. Never add `fallbackToDestructiveMigration`, never
   drop a column/table containing user data without a copy step and an explicit product
   decision.
2. SQLite `ALTER TABLE` can add a column and rename a table; for a new NOT NULL column use
   a SQL default. For constraints, indexes or foreign keys, create a `_new` table, copy
   every row, drop the old table, rename, and recreate indexes (the 8→9 migration is the
   pattern).
3. Preserve table names, column names and entity names. A package move must not rename a
   table; set `tableName` explicitly if a class moves.
4. Seed/update managed lists with `INSERT OR IGNORE` (or a carefully targeted `UPDATE`);
   never wipe user edits.
5. Export the schema (`exportSchema = true` is already set; the kapt schema location is in
   `app/build.gradle`). The generated JSON must be committed.
6. Add a migration test that opens the old version, inserts representative rows (including
   encrypted-looking strings), runs the migration with `runMigrationsAndValidate`, and
   asserts the new columns/rows and that old data survived.

**Tests:** `MigrationTest` for each step, `MigrationGuardTest`, and the affected
`BackupFlowTest`/repository test. Run instrumented tests only on one chosen test device
(section 3).

### 5.9 Bump the backup format

**Files**

- `core/data/backup/BackupModels.kt` — `BACKUP_FORMAT` (payload format) and the new model
  fields; keep the old constants.
- `core/data/backup/BackupCodec.kt` — encode the new shape and branch on `meta.format` in
  `decodePayload`; keep the old decoder.
- `core/data/backup/BackupPlanner.kt` (target `:domain`) — resolve defaults and `differs`
  for cards from older formats.
- `core/data/backup/BackupImporter.kt` — defaults for missing fields.
- `core/data/backup/BackupSettings.kt` — new settings keys in `defaultMap()`.
- Tests and at least one fixture per old format under the test resources.

**Rules**

1. Never reuse or reinterpret a format number. A new payload shape is `format 3`; format 1
   and 2 stay importable forever.
2. The envelope version (`ExportCrypto.VERSION`) is separate. Bump it only if the
   encrypted envelope/crypto changes; a payload-format bump alone does not change it.
3. A missing field in an old file must map to a documented default, not crash and not
   overwrite the phone with garbage.
4. The import review (`new/changed/same`, list counts, settings included) must work for
   every supported format.
5. Update `prototype/v2.html` (`snapshot`, `planImport`, `importDialog`),
   `prototype/timeline.html` `#backup`, `DESIGN.md` “Backup and Restore”, and
   `PRODUCT.md`.
6. Add a fixture file and a test that decodes it; do not rely on a hand-written file
   only. Test export -> erase -> import on a test device (`BackupFlowTest` or manual).

**Tests:** `BackupCodecTest`, `BackupPlannerTest`, `BackupFlowTest` (instrumented),
`AppSettingsTest`/`BackupSettings` for `cfg`, plus a migration test if the stores change.

### 5.10 Update the prototype and docs

The prototype is not a mock-up; it is the behaviour spec. When a behaviour or copy change
is agreed:

1. Change `prototype/v2.html` (logic and exact copy) and, if visual, the tokens in
   `prototype/quickcards-v2.css`.
2. Update `prototype/timeline.html` sections `#formats`, `#backup`, `#rules`, `#config`,
   `#features` and the dated step.
3. Update `DESIGN.md` (frontmatter tokens, components, named rules) and `PRODUCT.md`
   (capabilities, constraints, product decisions).
4. Re-capture the affected `prototype/reference/*.png` and update its README index.
5. Update the feature strings and tests in the same change; the guard tests must pass.
6. Describe files, decisions, deviations and anything unfinished in the pull request.

`prototype/**`, `DESIGN.md` and `PRODUCT.md` are the agreed contract: change them only
as part of an agreed behaviour or design change, never as a side effect of an
implementation. Never let the code, prototype and docs describe different behaviour.

### 5.11 Build and sign a release

1. Increment `versionCode`/`versionName` in `app/build.gradle`.
2. Confirm `keystore.properties` exists and is git-ignored; it must point at
   `keystore/quickcards-release.jks`. Never commit either. `keystore/README.md` covers
   custody and rotation.
3. `./gradlew :app:assembleRelease` (R8 + shrink). The release build has
   `BuildConfig.SECURE_WINDOW = true` and `FLAG_SECURE` on every activity.
4. Verify the artifact:

   ```bash
   apksigner verify --print-certs app/build/outputs/apk/release/QuickCards.apk
   $ANDROID_SDK_ROOT/build-tools/34.0.0/aapt dump permissions \
     app/build/outputs/apk/release/QuickCards.apk
   ```

   The permission list must not contain `android.permission.INTERNET`.
5. Install over the previous build only with the same signing key; a new key requires an
   uninstall and loses the Keystore data, so export first.
6. Do not distribute a debug build: debug allows screenshots by design.

### 5.12 Change card text parsing (paste and smart paste)

**Files**

- `domain/src/main/kotlin/com/quickcards/domain/CardTextParser.kt` — pasted or recognised
  lines to `ParsedCardText(number, expiry)`. Never add a CVV or name field.
- `feature/cardform/SmartPaste.kt` — detects a paste (one event that inserted 2+ characters)
  and turns the field text into a number and, when useful, an expiry.
- `feature/cardform/CardFormContent.kt` — the number and expiry `onValueChange` handlers
  (both on the number pad, `DigitsKeyboard`: a text keyboard drops back to letters after every
  group space);
  the number field keeps only the network badge as its trailing element (16dp end inset).
- `strings_cardform.xml` — the two paste snackbars.

**Rules**

1. `SmartPaste.apply` stays pure (`previous`, `incoming`, field, networks, today,
   `expiryEmpty`) so its cases run on the JVM. A single typed character must return an empty
   result and take the field's existing digit filter unchanged.
2. Fill through `CardFormViewModel.updateNumber` / `updateExpiry`; never bypass grouping,
   network detection, limits or validation. Let the field values re-render from the session.
3. Never fill the CVV or a holder name from pasted text. CVV keeps its own filter.
4. An expiry found while pasting into the number field fills Expiry only when that field is
   empty. Pasting into Expiry fills only the normalised `MM/YY` date, with no snackbar.
5. Junk text keeps the digit-filter behaviour and shows no snackbar.
6. Do not add a camera permission, a camera dependency or a camera route; the guard tasks
   fail if one appears.

**Tests:** `CardTextParserTest` (JVM, realistic OCR/paste fixtures), `SmartPasteTest` (JVM:
multi-line scan text, spaces/dashes, Expiry, junk, single digits, replacing a partly typed
number, Amex, a phone-number line), `OfflineManifestGuardTest`.

### 5.12a Built-in network rules, Maestro CVV and updates

- `domain/.../CardNetwork.kt` — `DefaultNetworks` is the single source of the built-in rules;
  `SeedData.networks` is built from it (`SeedDataTest`). `standard(id)`, `changedRules(network)`
  and the per-network `cvvOptional` flag ("Cards may have no CVV"; on for built-in Maestro, stored
  in `networks.cvvOptional`, carried in backups as `cvvOptional`, missing means off).
  `DefaultNetworksTest` pins the CVV lengths to the scheme rules (Amex 4, everything else 3).
- `core/ui/entity/EntityDialog.kt` — the "Change Visa rules?" warning for a new difference and the
  Reset to standard row. `feature/cardform` — `CardFormSession.changedRulesBlocking()` and
  `CardFormViewModel.resetNetworkRules`; `withLists` revalidates the number and CVV when networks
  change.
- `VaultRepository.updateNetwork` recomputes every card's Check details flag; values are never
  rewritten (`NetworkRuleReviewTest`).
- Import and its review screen check cards against the phone's networks plus the backup's
  networks the phone does not have yet (`BackupNormalizer`, matched by name, phone rules win),
  so a blank CVV on a backup-only "Cards may have no CVV" network is not flagged
  (`BackupNormalizerTest`, `BackupFlowTest`).
- `core/update/AppUpdates.kt` + `AppUpdatePrompts.kt` — flexible Play update, prompted once per
  version (`UpdatePolicy`, prefs `app_updates`); no lock grace, so a Play sheet that backgrounds the app locks it as usual.

### 5.13 Change expired / expiring-soon behaviour

**Files**

- `domain/src/main/kotlin/com/quickcards/domain/ExpiryStatus.kt` — `ExpiryState`
  (`VALID`, `EXPIRES_SOON`, `EXPIRED`) and `expiryState(expiry, today, soonMonths)`.
- `domain/src/main/kotlin/com/quickcards/domain/CardListOrder.kt` — `ExpiredPlacement`
  (`KEEP`, `END`, `HIDE`) and the stable list/visibility rules.
- `core/data/settings/AppSettings.kt` — `expiredCards`, `expiresSoonMonths` and their
  defaults/options; `BackupSettings.defaultMap()`.
- `feature/cards` — pass label/chip and the Status filter group. `feature/settings` —
  the Card list rows. `feature/cardform` never classifies a card.

**Rules**

1. A card is expired **after the last day of its expiry month** (local date). It
   expires soon when it is not expired and its month is inside the window;
   `ExpiredPlacement.HIDE` still shows it in search and when the Expired status filter
   is on. `END` moves only expired **unpinned** cards.
2. Expiry is display state only: copy, reveal, notification, edit, pin, archive and
   delete keep working. Expired uses the whole-pass grey and one `Expired` chip;
   expires-soon is a label only.
3. Copy is the §6b glossary: **Expired**, **Expires soon**, **Status**. Never
   "inactive", "old" or "hidden".
4. Settings › Card list shows the current choice as the row subtitle; the radio dialog
   writes through `AppSettings` (no local state) so the list updates immediately.

**Tests:** `ExpiryStatusTest` (month-end, 1/2/3-month windows, year rollover, expired
is never also soon), `CardListOrderTest` (KEEP/END/HIDE, search, status filter,
pinned expired), `AppSettingsTest`, `SettingsCardListFlowTest`.

### 5.14 Archive a card

**Files**

- `core/data/repository/VaultRepository.kt` — `archive(cardId, reason)`,
  `restore(cardId)` and `observeArchived()`; archive unpins, restore leaves it
  unpinned.
- `core/data/model/Card.kt` / `VaultCard.kt` — `archived`, `archivedAt`,
  `archiveReason` (metadata encryption) and the DAO queries.
- `feature/archive/**` — the Archived cards screen, search, restore/delete menu.
- `feature/cards` — the `Archive` / `Restore` menu entries and dialogs.
- `feature/manage` — the **Archived cards** row with its count.
- `feature/settings` — the **Archived cards** visibility row.

**Rules**

1. Archiving is reversible: the snackbar offers **Undo**, the card leaves the main
   list, and its pin is cleared. Expired + archived resolves as archived.
2. The optional reason is trimmed to 40 characters and stored with
   `encryptMetadata`; it is shown as `Archived · {reason}` on the pass.
3. Archived cards are never in the main list; with **Also in search results** the
   main search finds them and labels them. Delete still requires the normal unlock.
4. Backup format 2 carries `archived`, `archivedAt` and `archiveReason`; older files
   import with defaults. Do not invent a new list that bypasses the repository.

**Tests:** `ArchiveRepositoryTest` (archive/unpin/restore/reason), `CardListOrderTest`,
`BackupCodecTest`, `BackupFlowTest`, and the archive Compose flows.

### 5.15 Repair legacy values and Undo

**Files**

- `domain/src/main/kotlin/com/quickcards/domain/LegacyCardNormalizer.kt` — the rules
  and the `fixed`/`needsReview` result.
- `core/data/repository/LegacyRepair.kt` — the one-shot upgrade job, the encrypted
  `card_repairs` log, the 30-day window and `undo`.
- `core/data/repository/VaultRepository.kt` — `repairsFor`, `undoRepair`,
  `recordRepairSummary`, `dismissRepairSummary`.
- `core/data/backup/BackupNormalizer.kt` / `BackupImporter.kt` — every import runs
  the normalizer before review and in the merge.
- `feature/cardform` — Edit shows `Changed from {before}` + **Undo** for a repair
  inside the window, and a `needsReview` field opens with its normal error.

**Rules**

1. Repair only values that can be proved valid; keep anything else unchanged and
   flag the field. Never lose data, never log a value.
2. `before` and `after` are card data: encrypt both with
   `EncryptionHelper.encrypt`. The job is idempotent and guarded by
   `RepairSummaryStore.isCompleted()`; `LegacyRepair.prune` runs at app start.
3. Undo restores the exact old value, deletes that log row and marks the field for
   checking again. It is offered only inside 30 days.
4. The Cards banner uses `RepairSummary` ("Fixed the format of n cards…"), and an
   import with fixes records the same summary. Do not show plaintext values.

**Tests:** `LegacyCardNormalizerTest`, `LegacyRepairTest`, `MigrationTest` (11→12),
`BackupCodecTest` (normalized import), `CardFormViewModelTest` (review error + Undo),
`ImportSummaryTest`, `ImportFormatsSummaryTest`.

---

## 6. Guard tests

These are JVM source scans; they fail the build on a pattern, not on a screenshot. They
live under `app/src/test/java/com/quickcards/app/`:
`ArchitectureGuardTest` at the root, `core/data/database/MigrationGuardTest`,
`core/ui/components/OverlayGuardTest` and `HardcodedStringGuardTest`, and
`core/ui/theme/ColorGuardTest`.

| Test | What it protects | How to fix a failure |
|---|---|---|
| `OverlayGuardTest` | No raw `Dialog`, `AlertDialog`, `ModalBottomSheet`, `ExposedDropdownMenu`, `DropdownMenu` or `Popup` outside the Qc wrappers | Move the call into `QcDialog`/`QcOverlays`; only add a new wrapper file to the allow-list |
| `ArchitectureGuardTest` | `feature`/`core`/`:domain` dependency direction; no UI->DAO/entity imports; no `:domain` Android imports | Move shared code to `core` or `:domain`; wire features in `app/navigation` |
| `HardcodedStringGuardTest` | No user-visible string literals in `feature/**`/`core/ui/**` | Move to the owning `strings_*.xml`; allow-list only typographic literals such as `•` |
| `ColorGuardTest` | No `Color(0x...)` outside `core/ui/theme` | Add a named token in `Color.kt`, `PassColors` or `QcColors` |
| `MigrationGuardTest` | `exportSchema = true`, a committed schema JSON per version, a migration for every step, no destructive fallback | Add the migration/schema/test (recipe 5.8) |
| `OfflineManifestGuardTest` + `verifyOfflineManifest` | The merged debug and release manifests contain no `INTERNET`, no `ACCESS_NETWORK_STATE`, no `CAMERA` and no `com.google.android.datatransport` component | Remove the dependency or add `tools:node="remove"` for the exact component; never allow a network or camera library |

`HardcodedStringGuardTest` is enabled and every guard runs in the unit-test task. Its
allow-list (`app/src/test/resources/hardcoded-strings-allowlist.txt`) holds only the
bullet `•`; punctuation-only literals are allowed automatically. Never silence a guard by
adding production code to its allow-list unless the exception is genuinely non-user-facing
and documented.

---

## 7. Before you finish

- [ ] `./gradlew :domain:test :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
      passes with no failures and no new warnings.
- [ ] No `INTERNET`/`ACCESS_NETWORK_STATE` permission or network call was added;
      `verifyOfflineManifest` and `OfflineManifestGuardTest` pass.
- [ ] No plaintext card value, pasted text or parser line is stored or logged; no camera
      permission or camera dependency exists.
- [ ] Every new secret is encrypted; no plaintext card data in logs, intents or backups.
- [ ] Migrations are non-destructive, schema JSON is committed and `MigrationTest` covers
      the step.
- [ ] Every user-visible string is in the owning `strings_*.xml`; every design value is in
      `Dimens`/theme/`QcIcons`.
- [ ] Overlays use the Qc wrappers and close on lock.
- [ ] New tests cover the rule, the migration or the flow you changed.
- [ ] `prototype/`, `PRODUCT.md`, `DESIGN.md` and the reference screenshots still describe
      the shipped behaviour.
- [ ] Pull request describes the change; no keystore or password committed.
