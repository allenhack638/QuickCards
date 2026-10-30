# QuickCards — contributor and coding-agent rules

QuickCards is a local-first Android card vault (`com.quickcards.app`, Compose, Material 3,
`minSdk 26`). Read `docs/MAINTAINING.md` for the architecture map, security model, craft
rules and recipes before changing code. `prototype/v2.html` is the behaviour spec; copy
text is part of the contract.

These rules apply to human contributors and AI coding agents alike.

## Non-negotiable rules

1. **Offline only.** Never add `android.permission.INTERNET`, a network client, analytics,
   telemetry or a remote font/icon. Everything is bundled. Keep `minSdk 26`; do not raise
   it.
2. **Encryption is mandatory.** Card number, expiry, CVV use
   `EncryptionHelper.encrypt/decrypt`; holder/bank/note metadata uses
   `encryptMetadata/decryptMetadata`. Never log or persist plaintext card data. Duplicate
   detection stays the Keystore HMAC (`PanHasher`) and the unique `cards.panHash` index.
3. **No destructive migrations.** No `fallbackToDestructiveMigration`. Every Room version
   bump gets a non-destructive `Migration`, a committed schema JSON and an instrumented
   `MigrationTest` for that step. Existing data must survive.
4. **Overlays use the Qc wrappers.** Raw `Dialog`, `AlertDialog`, `ModalBottomSheet`,
   `ExposedDropdownMenu`, `DropdownMenu` and `Popup` are forbidden outside
   `core/ui/components/QcDialog.kt` and `QcOverlays.kt`. The wrappers are the lock guard;
   never bypass them.
5. **Strings and tokens only via resources/theme.** All user-visible text lives in the
   owning `res/values/strings_*.xml`; all colours, sizes, shapes, type and icons come from
   `core/ui/theme` (`Color.kt`, `Dimens.kt`, `Type.kt`, `Shape.kt`, `QcIcons.kt`). No
   `Color(0x...)` outside the theme and no literal prose in feature composables.
6. **Tests and lint must pass.** `:domain:test`, `:app:assembleDebug`,
   `:app:testDebugUnitTest` and `:app:lintDebug` must all be green before a change is
   proposed.
7. **Keep the contract in sync.** A behaviour or copy change updates `prototype/v2.html`,
   `prototype/timeline.html`, `prototype/quickcards-v2.css` when visual, `DESIGN.md`,
   `PRODUCT.md`, the reference screenshots and the feature strings in the same change.
   Code and prototype must never disagree.
8. **Never commit keys.** No keystore (`.jks`/`.keystore`), `keystore.properties`,
   password or signing secret may be committed. Release signing is documented in
   `keystore/README.md`.
9. **Auth is fail-closed.** A cancelled/failed BiometricPrompt never lets a copy, reveal,
   edit, delete, export, import or erase proceed. Locking closes overlays and clears
   revealed values; the clipboard and the notification run on their own timers and are
   never cleared by leaving the app.
10. **Small, reviewable changes.** Work on a feature branch and open a pull request; never
    push directly to `main`. Coding agents commit, push, reset or switch branches only
    when the person they work for asks.

## Build and test

JDK 17, Android SDK 36, run from the repo root:

```bash
./gradlew :domain:test
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleRelease        # needs git-ignored keystore.properties
./gradlew :app:bundleRelease          # Play upload (.aab)
```

A Kapt warning about Kotlin 1.9 fallback for Room is expected. Test results live under
`app/build/test-results/testDebugUnitTest/`. Play Console declarations and the tester plan
are in [`docs/PLAY_RELEASE.md`](docs/PLAY_RELEASE.md).

## Instrumented tests

`connectedDebugAndroidTest` and `installDebug` fan out to **every** connected device. Do
not use them; pick one test device or emulator and target it explicitly:

```bash
adb devices                                   # find the serial of your test device
export ANDROID_SERIAL=<serial>
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s "$ANDROID_SERIAL" shell am instrument -w -r \
  com.quickcards.app.test/androidx.test.runner.AndroidJUnitRunner
```

An API 26 emulator is the reference target. Use a throwaway emulator with a screen lock
set, never a personal phone with real card data.

## Where to look

- `docs/MAINTAINING.md` — architecture, security model, craft rules, current
  DB/backup/settings/network facts, and step-by-step recipes.
- `prototype/v2.html` — the behaviour source of truth.
- `prototype/reference/` — target screenshots (layout, spacing, type, colour).

## Before finishing

- [ ] No `INTERNET` permission or network call.
- [ ] No plaintext secret in storage, logs, intents or a backup.
- [ ] Migration + schema + `MigrationTest` if the DB changed.
- [ ] Strings in resources; tokens in theme; overlays via Qc wrappers.
- [ ] Dark and light checked against `prototype/reference/`.
- [ ] Build, unit tests and lint pass.
- [ ] Prototype/PRODUCT/DESIGN updated if behaviour changed.
