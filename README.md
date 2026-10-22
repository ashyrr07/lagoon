# Lagoon

A free, open-source Dynamic Island for Android. A pill that wraps your camera cutout and
shows what matters right now: music, calls, timers, charging, earbuds, navigation and the
notifications you choose, with quick controls one long-press away.

- Free forever. No ads, no subscriptions, no accounts.
- No network. Lagoon has no `INTERNET` permission, so nothing leaves your phone.
- Any brand. The cutout is read from the system and can be calibrated by hand.
- Standard mode needs only "Display over other apps" and Notification access.
- Optional Enhanced mode (FOSS build) uses an accessibility service for taps on the status-bar strip and the lock screen.

## Build

Requirements: JDK 17 or newer (Android Studio's bundled JBR works), Android SDK 36.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"   # or your JDK
./gradlew assembleFossDebug
```

Flavors: `foss` (GitHub / F-Droid) and `play` (Google Play).

## License

GPL-3.0. See [LICENSE](LICENSE).
