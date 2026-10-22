# Lagoon

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![CI](https://github.com/OWNER/lagoon/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/lagoon/actions/workflows/ci.yml)
![Android 12+](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)
![No INTERNET permission](https://img.shields.io/badge/network-none-brightgreen)

A free, open-source Dynamic Island for Android. A black pill wraps your camera cutout and
shows what matters right now (music, calls, timers, charging, earbuds, navigation and the
notifications you choose) with quick controls one long-press away.

- **Free forever.** No ads, no subscriptions, no accounts, no "pro" tier.
- **No network.** Lagoon has no `INTERNET` permission. CI fails if one ever appears in the merged manifest.
- **Any brand.** The cutout is read from the system and can be calibrated by hand: hole, corner hole or notch.
- **Play-safe by default.** Standard mode needs only "Display over other apps" and Notification access.
- **Enhanced mode is optional** and uses an accessibility service that never reads screen content.

> Status: under active development, not yet released. The sections below say what works
> today and what is planned. Nothing is on the Play Store or F-Droid yet.

## Screenshots

Screenshots are produced from the in-app Demo screen; see
[`fastlane/metadata/android/en-US/images/README.md`](fastlane/metadata/android/en-US/images/README.md).

| Idle | Now playing | Call | Control deck |
|---|---|---|---|
| _coming soon_ | _coming soon_ | _coming soon_ | _coming soon_ |

## Features

Done means it builds, has tests where the plan asks for them, and passes the emulator check
with a simulated cutout. Everything else is planned and tracked in the milestone list below.

| Area | State |
|---|---|
| Idle pill around the cutout, auto-detected, with manual calibration (offset, size, radius) | Done |
| Overlay hosted by the notification listener (no foreground service needed) | Done |
| Activity engine: priority, split island, queue, TTL, dismiss; spring motion; gestures | Done |
| Settings app: Home with permission status and setup health, Position screen with live calibration | Done |
| Media: album art, title/artist, transport controls, seek bar, art tint | Planned (M3) |
| Notifications: per-app allowlist, compact and expanded, actions, inline reply | Planned (M3) |
| Calls: incoming / ongoing / ended from the dialer's `CallStyle` notification | Planned (M4) |
| Timers and stopwatch from Clock apps, built-in timer, next alarm | Planned (M4) |
| Charging alert with wattage, low / full battery | Planned (M4) |
| Bluetooth connect / disconnect with earbud battery | Planned (M4) |
| Navigation (Google Maps, Waze) and Android 16 Live Updates | Planned (M4) |
| Control deck: torch, DND, rotation, brightness, volume, Wi-Fi / Bluetooth panels, camera, shortcuts, widgets | Planned (M5) |
| Appearance, gestures, keep-alive guides, export / import, onboarding | Planned (M6) |
| Enhanced mode (accessibility service, FOSS flavor) | Planned (M7) |
| Release tooling: signing, R8, store metadata, privacy policy, Play declarations | Done (M8 scaffolding) |

Explicitly out of scope for v1: hosting real AppWidgets, Galaxy Buds per-bud battery, weather,
a plugin API for third-party apps, Wear OS.

## Standard vs Enhanced mode

Android draws the status bar *above* every app overlay, so a normal overlay cannot receive
taps inside the status-bar strip. Lagoon handles that with two modes.

| | Standard (default) | Enhanced (optional, off by default) |
|---|---|---|
| Permissions | Display over other apps, Notification access | Standard + an accessibility service |
| Island hosted by | `IslandNotificationListener` (system-bound) | `IslandAccessibilityService` |
| Touch | The pill hangs ~12 dp below the status bar; that band takes tap / long-press / swipe. The status-bar strip stays with the system (clock, icons, shade swipe). | The whole pill is tappable, including the "hug the camera only" size. |
| Lock screen | Not shown (app overlays draw under the keyguard) | Shown |
| Extra tiles | - | Screenshot, lock screen, recents |
| Foreground app detection | From window insets (immersive / keyboard) | Exact, from window-state events |
| Reads screen content | No | **No.** `canRetrieveWindowContent=false`; only window-state change events (package name) are consumed. |
| Availability | `play` and `foss` flavors | `foss` flavor now; `play` once the Play accessibility declaration is approved |

Everything works in Standard mode. Enhanced mode only adds the items in its column.

## Permissions, explained

| Permission | Why | Revoke |
|---|---|---|
| Display over other apps (`SYSTEM_ALERT_WINDOW`) | Draws the island | Settings > Apps > Lagoon > Display over other apps |
| Notification access | Reads media sessions, notifications, call and timer notifications to show them on the island. Also the process that hosts the island. | Settings > Notifications > Notification access |
| `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` | The optional keep-alive foreground service (Health screen toggle) for phones that unbind the listener | Turn the toggle off |
| `RECEIVE_BOOT_COMPLETED` | Restores the island after a reboot | - |
| `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` (`neverForLocation`) | Earbud connect / disconnect alerts and battery level | Settings > Apps > Lagoon > Permissions > Nearby devices |
| `READ_PHONE_STATE` | Ringing / off-hook / idle only; caller identity comes from the dialer's own notification. No call log, no contacts. | Settings > Apps > Lagoon > Permissions > Phone |
| `WRITE_SETTINGS`, `ACCESS_NOTIFICATION_POLICY` | Brightness / rotation tiles and the DND tile in the control deck | System screens linked from the Controls settings |
| `VIBRATE` | Haptics | Appearance settings |
| Accessibility service (Enhanced mode, `foss`) | See the table above | Settings > Accessibility > Lagoon > Off |

Not requested: `INTERNET`, `QUERY_ALL_PACKAGES` (a `<queries>` filter for launcher apps is used
instead), `READ_CALL_LOG`, `READ_CONTACTS`, `READ_SMS`, location. Full details in
[docs/privacy.md](docs/privacy.md).

## Build

Requirements: JDK 17 or newer (Android Studio's bundled JBR is JDK 21 and works), Android
SDK platform 36 with build-tools 36. No `gradle` or `kotlinc` on the PATH is needed: use the
wrapper.

```bash
# macOS with Android Studio installed in the default location:
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
# or any JDK 17+: export JAVA_HOME=/opt/homebrew/opt/openjdk@21

./gradlew assembleFossDebug          # debug APK -> app/build/outputs/apk/foss/debug/
./gradlew testFossDebugUnitTest      # unit tests
./gradlew lintFossDebug              # lint
./gradlew assembleFossRelease        # release APK; unsigned unless keystore.properties exists
```

If `./gradlew` complains that no Java runtime is installed, `JAVA_HOME` is not set: the
system `java` launcher on macOS is only a stub.

`local.properties` with `sdk.dir=/path/to/Android/sdk` is created by Android Studio; on a
headless machine set `ANDROID_HOME` instead.

### Flavors

| Flavor | For | Differences |
|---|---|---|
| `foss` | GitHub Releases and F-Droid | Declares the Enhanced-mode accessibility service (`src/foss/AndroidManifest.xml`), `ENHANCED_MODE_AVAILABLE = true` |
| `play` | Google Play | No accessibility service until the Play declaration is approved, `ENHANCED_MODE_AVAILABLE = false` |

Both flavors share all code and neither has network access. Debug builds use the
`com.lagoon.island.debug` application id so they can be installed next to a release build.

### Release builds

Release builds are signed only when a gitignored `keystore.properties` exists at the repo
root; otherwise the APK is produced unsigned. The `release.yml` workflow signs from GitHub
secrets on `v*` tags and attaches the FOSS APK with SHA-256 checksums to a GitHub Release.
See [docs/release.md](docs/release.md).

## Installing

- **GitHub Releases**: download `lagoon-<version>-foss.apk`, check it against
  `SHA256SUMS.txt`, install. On Android 13+ a sideloaded app must be allowed through
  App info > "Allow restricted settings" before notification access or the accessibility
  service can be granted; onboarding walks through it.
- **F-Droid**: planned. The `foss` flavor is built without the Google Play dependency
  metadata blob and the repository carries Fastlane metadata so the listing can be
  submitted to `fdroiddata` once the first tagged release exists.
- **Google Play**: planned. Requires a closed test with 12 testers for 14 days first
  (new developer account rule); see [docs/release.md](docs/release.md).

## Keeping it alive

Some manufacturers kill background apps aggressively. The Health screen reports when the
notification listener was disconnected and links to per-brand steps, collected in
[docs/oem-keep-alive.md](docs/oem-keep-alive.md).

## Project layout

```
app/src/main/kotlin/com/lagoon/island/
  cutout/     CutoutDetector, CutoutInfo, IslandGeometry (sizes in dp, tested)
  engine/     IslandActivity model, ActivityRegistry, IslandResolver (tested), gestures
  overlay/    OverlayHost, AppOverlayHost, IslandWindow, Compose lifecycle owner for the overlay
  service/    IslandNotificationListener (hosts the island), IslandEngine, IslandController
  sources/    IslandSource contract, NotificationBus, SourceManager, Hilt module
  data/       DataStore settings (kotlinx.serialization JSON), export / import
  ui/island/  IslandRoot and the island's Compose tree
  ui/app/     Settings app: MainActivity, Navigation 3, Home, Position
  util/       IntentLauncher (background activity starts from the overlay)
app/src/foss/, app/src/play/   flavor manifests
docs/                          privacy policy, Play declarations, release and OEM guides
fastlane/metadata/android/     store listing (F-Droid / Play)
```

Stack: Kotlin, Jetpack Compose (island and settings app), Material 3, Hilt via KSP,
DataStore, Coil, Navigation 3, kotlinx.serialization. minSdk 31, targetSdk 36.

## Contributing

Bug reports with the device model, Android version and manufacturer skin are the most
useful thing you can send. Code contributions: see [docs/contributing.md](docs/contributing.md).
The one hard rule: nothing may add network access.

## Privacy

Lagoon collects nothing and cannot send anything: it has no `INTERNET` permission. The
privacy policy is [docs/privacy.md](docs/privacy.md); the text submitted to Google Play is in
[docs/play-declarations.md](docs/play-declarations.md).

## License

GPL-3.0-or-later. See [LICENSE](LICENSE). Forks must stay open source under the same
license, which is the point: no paid repackaging.
