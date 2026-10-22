# Contributing to Lagoon

Thanks for helping. Lagoon is GPL-3.0, has no company behind it, and is built by people
who want a Dynamic Island that is free and private. This page covers how to report
problems, how to build, and what a pull request needs.

## Reporting a bug

The island depends on the phone's cutout, status-bar height, manufacturer skin and the
notification formats of the apps around it, so a report without device details is rarely
actionable. Please include:

- Device model and Android version, and the manufacturer skin version (One UI 7, HyperOS 2,
  ColorOS 15, ...).
- Lagoon version (Settings app > About) and where it came from (GitHub, F-Droid, Play).
- Standard or Enhanced mode.
- Steps to reproduce, and what you expected.
- For alignment problems: a screenshot of the idle island and the values from the Position
  screen.
- For "the island disappeared" problems: whether the Health screen reports a listener
  disconnect, and which steps from `docs/oem-keep-alive.md` you already tried.
- For a notification or media app that is parsed wrongly: the app's name and version. If you
  can, use Settings app > Health > "Copy last notification extras" (planned) so the keys can
  be added to the test fixtures.

Crash logs from release builds are obfuscated; the `mapping.txt` for every GitHub release
is attached to the release so maintainers can retrace them. Post the trace as-is.

## Building

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # any JDK 17+
./gradlew assembleFossDebug
./gradlew testFossDebugUnitTest
./gradlew lintFossDebug
```

Android Studio: open the folder, pick the `fossDebug` build variant. The settings app and
the island are both plain Compose; the Demo screen fires fake activities into the real
overlay so you do not need real calls or music to see every state.

Emulator tip: Developer options > *Simulate a display with a cutout* gives you hole,
tall-cutout and corner variants on the Pixel images; the Position screen's live calibration
must work for all of them.

## Ground rules for code

1. **No network, ever.** No `INTERNET` permission, no dependency that brings one
   (`CI` fails on the merged manifest if it appears). This rules out crash reporters,
   analytics, remote config, update checkers and web views with remote content.
2. **No new dangerous data access** without discussion in an issue first: no call log,
   contacts, SMS, location, `QUERY_ALL_PACKAGES`, screen content via accessibility.
3. **Everything must work in Standard mode.** Enhanced mode (accessibility) may add
   capabilities, never gate basic features.
4. **Both flavors build**: `foss` and `play` share all code; flavor differences live only in
   the flavor manifests and `BuildConfig.ENHANCED_MODE_AVAILABLE`.
5. **Kotlin + Compose only**, Hilt via KSP, no kapt, no Java sources, no new Gradle modules
   without a reason. Versions live in `gradle/libs.versions.toml`.
6. **Tests for logic**: the resolver, geometry, parsers and settings serialisation have unit
   tests; a change to any of them updates or adds tests. Notification and Bluetooth parsers
   take fixture data (an `Intent`/`Bundle` dump), not a live device, so they can be tested
   for phones the maintainers do not own.
7. **Smooth by construction**: no `updateViewLayout` per animation frame, no per-frame
   recomposition (use `graphicsLayer` lambdas and `Animatable`), window sized to its
   content, reduced-motion path honoured.
8. **Honest strings**: permission screens explain what is used and what is not; keep
   `docs/privacy.md` and `docs/play-declarations.md` in sync with any manifest change.

Code style follows `.editorconfig` (4 spaces, 120 columns, official Kotlin style). There is
no linter beyond Android Lint at the moment; keep imports explicit and files focused.

## Pull requests

- One topic per PR. Refactors and features in separate PRs.
- Fill the description with *what* and *why*, and the device/emulator you checked it on.
- `./gradlew testFossDebugUnitTest lintFossDebug assembleFossDebug assemblePlayDebug` must
  pass locally; CI runs the same plus the no-INTERNET check.
- If the change touches the manifest, update the permission tables in `README.md`,
  `docs/privacy.md` and `docs/play-declarations.md` in the same PR.
- If the change is visible in the store listing, update
  `fastlane/metadata/android/en-US/` too.
- By opening a PR you agree that your contribution is licensed under GPL-3.0-or-later like
  the rest of the project. No CLA.

## Translations

The settings app uses standard Android string resources. Add a `values-<lang>/strings.xml`;
store listing translations go in `fastlane/metadata/android/<locale>/`. Please do not
machine-translate the permission disclosures: they are reviewed by Google and must stay
accurate.

## Adding a source or a control tile

- A **source** implements `IslandSource` (`sources/`) and publishes `IslandActivity` values
  to the `ActivityRegistry`; it must not touch the overlay directly. Register it in
  `SourcesModule`.
- A **control tile** is registered in the controls registry (M5) with an icon, a label, a
  toggle or slider contract and the permission it needs; the Controls screen picks it up
  automatically.
- Both need a Demo-screen entry so screenshots and manual checks are reproducible.

## Security

If you find a way for Lagoon to leak data or to be abused through its listener or overlay,
please report it privately through GitHub's "Report a vulnerability" (Security tab) rather
than a public issue.
