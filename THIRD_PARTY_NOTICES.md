# Third-party notices

QuickCards bundles the following third-party software and assets. The list matches the
runtime dependencies in `app/build.gradle`; test-only dependencies (JUnit, AndroidX Test,
Espresso, Room testing, Compose UI test) are not shipped in the APK and are not listed.

## Apache License 2.0

The full text is available at <https://www.apache.org/licenses/LICENSE-2.0>.

| Component | Artifacts | Copyright |
|---|---|---|
| AndroidX core and lifecycle | `androidx.core:core-ktx`, `androidx.lifecycle:lifecycle-runtime-ktx`, `lifecycle-process`, `lifecycle-viewmodel-ktx`, `lifecycle-viewmodel-compose` | The Android Open Source Project |
| AndroidX Activity, Navigation and WindowManager | `androidx.activity:activity-compose`, `androidx.navigation:navigation-compose`, `androidx.window:window` | The Android Open Source Project |
| AndroidX Profile Installer | `androidx.profileinstaller:profileinstaller` | The Android Open Source Project |
| Jetpack Compose | Compose BOM `2024.12.01`: `ui`, `ui-graphics`, `material3`, `material-icons-extended` (debug builds also include `ui-tooling` and `ui-test-manifest`) | The Android Open Source Project |
| Material Components for Android | `com.google.android.material:material` | The Android Open Source Project / Google LLC |
| Room | `androidx.room:room-runtime`, `room-ktx` (and the `room-compiler` annotation processor at build time) | The Android Open Source Project |
| Biometric | `androidx.biometric:biometric` | The Android Open Source Project |
| Gson | `com.google.code.gson:gson` | Google LLC |
| colorpicker-compose | `com.github.skydoves:colorpicker-compose` | skydoves (Jaewoong Eum) |
| Kotlin standard library and kotlinx.coroutines | `org.jetbrains.kotlin:kotlin-stdlib`, `org.jetbrains.kotlinx:kotlinx-coroutines-android` | JetBrains s.r.o. and Kotlin contributors |

## SIL Open Font License 1.1

| Component | Copyright | Licence text |
|---|---|---|
| Roboto Flex (bundled font) | Copyright 2017 The Roboto Flex Project Authors | [`app/src/main/assets/licenses/RobotoFlex-OFL.txt`](app/src/main/assets/licenses/RobotoFlex-OFL.txt) |

## Not covered by the MIT licence

The Stub Lock logo and the QuickCards brand assets (`docs/brand/`, the launcher, splash,
notification and lock-screen marks) are © allenhack638 and are not licensed under MIT
unless stated otherwise. Card network names appear as text labels; the network marks and
their trademark status are listed in the next section.

## Network marks

`app/src/main/res/drawable/net_*.xml` and `prototype/assets/networks/*.svg` reproduce the
acceptance marks of Visa, Mastercard, American Express, RuPay, Diners Club, Discover, JCB,
Maestro, UnionPay and Mir, derived from the vector files published on Wikimedia Commons and
re-authored on a common 56x28 canvas. The marks are trademarks of their respective owners,
shown solely to identify a card's network; they are not covered by this project's MIT
licence and no affiliation or endorsement is implied.
