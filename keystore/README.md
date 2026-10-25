# Release signing

Release APKs are signed with a PKCS12 keystore that is **never committed**. Everything in
`keystore/` except this README is git-ignored, as is `keystore.properties` at the repo root.

- **Keystore:** `keystore/quickcards-release.jks` (PKCS12, RSA 4096, 10,000 days)
- **Alias:** `quickcards`
- **Passwords:** in the git-ignored `keystore.properties` locally, and in repository
  secrets for CI

`app/build.gradle` reads `keystore.properties`:

```properties
storeFile=keystore/quickcards-release.jks
storePassword=...
keyAlias=quickcards
keyPassword=...
```

## Back this up

Losing the keystore or its password means you can no longer sign updates that install
over an existing copy. Keep the `.jks` and its passwords in a password manager or on an
encrypted drive, never in the repository, an issue or a chat log.

## Legacy key — retired and compromised

The previous signing key, used for earlier builds, is **retired and must be treated as
compromised**.
Never use it to sign anything, never re-import it, and never trust an APK signed with it.
Builds signed with the current key do not install over builds signed with the legacy key:
export your cards (Settings › Backup), uninstall the old app, install the new APK and
import the backup.

## Rotation

1. Generate a new PKCS12 keystore with a fresh alias.
2. Update `keystore.properties` and the CI secrets below.
3. Increment `versionCode`/`versionName` in `app/build.gradle`.
4. Rebuild and verify with `apksigner verify --print-certs`.
5. Document in the release notes that users must export, uninstall and re-import.

## CI releases

`.github/workflows/release.yml` builds and publishes a signed APK on every push to `main`
of the main repository when `versionName` in `app/build.gradle` has no `v<versionName>`
tag yet. Pull requests run the tests and lint only and never see the secrets. It needs
four repository secrets (never commit them):

```bash
base64 -i keystore/quickcards-release.jks | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD   # paste storePassword
gh secret set KEY_ALIAS           # paste keyAlias
gh secret set KEY_PASSWORD        # paste keyPassword
```

To ship a release: bump `versionCode` (+1) and `versionName` (e.g. 2.0.1), merge to `main`.
