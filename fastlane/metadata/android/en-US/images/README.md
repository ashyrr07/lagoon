# Store images

This folder follows the Fastlane supply / F-Droid layout. F-Droid and the Play Console read
the same files, so one set of images serves both stores.

```
images/
  icon.png                 512 x 512, no alpha for Play (F-Droid accepts alpha)
  featureGraphic.png       1024 x 500 (Play only, F-Droid shows it as a banner)
  phoneScreenshots/
    1.png ... 8.png        1080 x 2400 (9:16 to 9:19.5), PNG or JPEG, 2 to 8 images
```

## How the screenshots are produced

Screenshots are taken from the in-app **Demo** screen (Settings app -> Demo), which fires
fake activities (now playing, incoming call, timer, charging, earbuds, navigation,
notification, control deck) into the real island so every state can be captured on demand
without waiting for a real call or a real song. The island is captured on a Pixel-class
emulator with Developer options -> "Simulate a display with a cutout" -> *Hole punch*, and
on a Samsung for the second set.

Suggested order (matches the feature list in `full_description.txt`):

1. Idle island hugging the camera, on the home screen
2. Now playing expanded (album art, seek bar, transport controls)
3. Incoming call with answer / decline
4. Timer compact next to a running stopwatch (split island)
5. Charging ring with wattage
6. Earbuds connected with per-bud battery
7. Control deck
8. Settings app home with the live preview and setup health check

Do not add marketing frames, device bezels or text that is not in the app; F-Droid shows the
raw files and Play rejects screenshots that misrepresent the app.

Screenshots are committed to the repository (F-Droid fetches them from git), so keep each
file under 1 MB (`pngquant --quality 70-90` is fine). Nothing in this folder is ever bundled
into the APK.
