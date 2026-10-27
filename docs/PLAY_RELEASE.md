# Google Play release

This is the working checklist for the Play upload. It is not part of the app contract; keep
it current when the Console answers or the release process change.

## Current status (2.4.0 branch)

| Requirement | Status |
|---|---|
| `targetSdk`/`compileSdk` **36** (required since 31 Aug 2026) | Done in `app/build.gradle`; AGP 8.9.2 |
| Signed **AAB** | `./gradlew :app:bundleRelease`; CI uploads the `.aab` + `mapping.txt` as the `play-upload` artifact |
| Public privacy-policy URL | `docs/privacy.html` — publish it (GitHub Pages on `/docs`) and put the URL in Play Console |
| In-app privacy policy | Settings › About › Privacy policy, bundled in `res/values/strings_privacy.xml` (no network) |
| Data safety / content declarations | Answers below; enter them in Play Console |
| 12 testers × 14 continuous days | Not started; see the tester plan below |
| Play App Signing | Must be enabled on the first upload; decide the key first (below) |

## Signing decision (do this first)

`keystore/README.md` says the **legacy key is compromised**. Never upload or reuse it.

- Use `keystore/quickcards-release.jks` (alias `quickcards`) as the **upload key** for Play.
- Play App Signing is mandatory for new apps. If you upload your current key as the **app
  signing key** at enrollment, the Play build keeps the same signature as the GitHub APK, so
  sideload users can update in place. If you let Google generate the app-signing key, Play
  builds have a different signature and sideload users must uninstall first.
- Back up the upload key and its passwords outside the repo. With Play App Signing you can
  reset a lost upload key, but you cannot recover a lost Play account.
- If the app was ever uploaded with the legacy key, stop and use Play Console's upload-key
  reset / a new listing; do not simply switch keystores.

## Console declarations

### App content

- **Privacy policy:** the published `docs/privacy.html` URL.
- **Ads:** No.
- **Sign-in details:** the app has no account, but the app lock is an authentication gate.
  Under "Any other instructions", say:
  > No account. On a device with a screen lock, unlock with the device PIN/pattern or
  > fingerprint (the app uses the device credential). On a device without a screen lock the
  > app opens. To test: Cards › Add card, use 4111 1111 1111 1111, any future expiry,
  > CVV 123, holder "Test", bank "HDFC Bank". Settings › About › Privacy policy shows the
  > in-app policy.
- **Target audience:** 18+ (financial info; not child-directed).
- **Content rating:** complete the IARC questionnaire (no objectionable content; likely
  Everyone). Do not select "Designed for Families".
- **News / Government / Health / Families:** No.
- **Financial features:** the app does not provide a financial service (no payments, lending,
  banking, investing or insurance); it is a personal organizer. Answer accordingly.
- **Export compliance:** the app uses AES-256-GCM and PBKDF2. Declare encryption and use the
  mass-market/consumer exemption (self-classification); see
  <https://support.google.com/googleplay/android-developer/answer/113770>.
- **Permissions declaration:** none needed. The app requests no high-risk permission
  (`USE_BIOMETRIC`, `NFC`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`,
  `HIDE_OVERLAY_WINDOWS` are normal permissions).

### Data safety

The app has no server, no account, no analytics/ads SDK and no `INTERNET` permission.

- **Does your app collect or share required user data?** No.
  - "Collect" means transmitting data off the device; everything is on-device only.
  - Share, Autofill and Export are **user-initiated** transfers to a destination the user
    chooses, which the Data safety definition exempts from "sharing". The privacy policy
    spells this out.
- **Data encrypted in transit:** not applicable (nothing is transmitted).
- **Deletion:** users can permanently erase everything in Settings › Backup › Erase all data.
- **Independent security review:** optional; not claimed.
- Keep the policy and the form consistent. If a reviewer asks why a card vault says "no data
  collected", point to the no-network permission, the on-device-only design and the policy.

### Store listing

- Icon: `docs/brand/stub-lock/play-store-512.png` (512×512).
- Feature graphic: `docs/brand/stub-lock/feature-1024x500.png`.
- Screenshots: at least 2, 9:16, no real card data; `docs/screenshots/*` are 540×960 and use
  test numbers. Prefer exporting 1080×1920 for quality.
- Short and full description: say "offline card organizer", not "bank", "wallet" or
  "payment app"; do not imply bank/network affiliation. Network marks are trademarks and are
  identified as such in `THIRD_PARTY_NOTICES.md`.
- Privacy policy URL, support email, category **Finance** (or Tools), countries, free.

## Closed test (personal account created after 13 Nov 2023)

1. Upload the AAB to **Closed testing** (not internal; internal testers are not eligible for
   the closed test until they opt out).
2. Create a Google Group (e.g. `quickcards-testers@googlegroups.com`) and use it as the tester
   list; share the group + opt-in links.
3. Recruit **15–20** testers for **12** required; each needs an Android phone and a Google
   account. Ask them to install, use a fake card, try copy/reveal/backup, and send one piece
   of feedback.
4. Testers must stay opted in **continuously for 14 days**. A late addition starts their own
   14-day clock, so keep the buffer.
5. On day 14, confirm 12+ are still opted in, then Dashboard › Apply for production. Review
   is usually ≤7 days.

## Release commands

```bash
# Local (SDK 36 + JDK 17)
./gradlew :domain:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew :app:bundleRelease
# app/build/outputs/bundle/release/*.aab
# app/build/outputs/mapping/release/mapping.txt  (upload with the AAB)

# Install the debug build on one chosen device only
adb devices
export ANDROID_SERIAL=<serial>
./gradlew :app:assembleDebug
adb -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/QuickCards-debug.apk
```

CI builds the AAB on a `v<versionName>` release and uploads `play-upload` (AAB + mapping).
Upload that artifact manually, or add a Play Developer API step with a service account.

## After launch

- Keep the public privacy policy and the Data safety form in sync with the app.
- Upload `mapping.txt` with every AAB so ANRs/crashes are deobfuscated.
- Use a staged rollout (10% → 50% → 100%) and watch the pre-launch report. `FLAG_SECURE`
  makes its screenshots black; that is expected, not a failure.
- Keep the upload key and Play account access backed up.
