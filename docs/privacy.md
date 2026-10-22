# Lagoon privacy policy

Last updated: 2026-10-22. Applies to every build of Lagoon (`com.lagoon.island`) from
Google Play, F-Droid or GitHub Releases.

## The short version

Lagoon does not collect, store, transmit or sell any personal data. It cannot: the app has
no `INTERNET` permission, so the operating system blocks it from opening any network
connection. There are no analytics, no crash reporting services, no advertising SDKs, no
accounts and no server.

Everything Lagoon shows on the island is read from your phone, displayed on your phone, and
stays on your phone.

## What stays on the device

- **Your settings** (position, appearance, which apps may show on the island, gestures) are
  stored in the app's private storage. They are not backed up to the cloud
  (`android:allowBackup="false"`). You can export them to a JSON file yourself from the
  settings app and import them on another phone; that file is written where you choose and
  is never uploaded by Lagoon.
- **Notification and media content** is held in memory only for as long as the island shows
  it and is discarded afterwards. Lagoon keeps no history of notifications, calls or songs.

## Permissions and what they are used for

| Permission | Used for | Not used for |
|---|---|---|
| Display over other apps | Drawing the island on top of other apps | Reading what is on screen |
| Notification access | Showing media playback, notifications from the apps you allow, the dialer's call notification and Clock timers on the island, and acting on their buttons (play, reply, answer) on your behalf | Storing, forwarding or analysing notifications |
| Nearby devices (Bluetooth) | Connect/disconnect alerts and the battery level of earbuds | Location. `BLUETOOTH_SCAN` is declared `neverForLocation` |
| Phone | Knowing whether a call is ringing, active or ended | Reading your call log, contacts or phone number |
| Notifications (post) and foreground service | The optional keep-alive service, which shows a persistent notification | Anything else |
| Run at startup | Restoring the island after a reboot | - |
| Modify system settings, Do Not Disturb access | Brightness, rotation and DND tiles in the control deck, only when you add them | - |
| Vibration | Haptic feedback | - |

Not requested: Internet, location, camera, microphone, contacts, call log, SMS, files,
"query all packages".

### Notification access, in detail

Android's notification listener gives an app every notification on the device. Lagoon uses
that access to:

1. Find active media sessions (album art, title, playback state) and send transport commands.
2. Show notifications from apps on your allowlist on the island. Apps you have not allowed
   are ignored: their content is not parsed and not kept.
3. Mirror the dialer's call notification (caller name and its answer/decline/hang-up buttons).
4. Read Clock apps' timer and stopwatch notifications.
5. Keep the island process alive: the system binds the listener, so no foreground service
   is needed in Standard mode.

Revoke it at any time: Settings > Notifications > Notification access (or Special app
access) > Lagoon. The island disappears immediately; nothing is retained.

### Accessibility service (Enhanced mode only, off by default)

The optional Enhanced mode runs an accessibility service so that the island can be drawn
above the status bar and on the lock screen, the whole pill can receive taps, and the
control deck can take a screenshot, lock the screen or open recents. The service is
configured with `canRetrieveWindowContent="false"`: it cannot read the text or layout of
other apps. The only event it listens to is "window state changed", from which it reads the
package name of the foreground app in order to apply your per-app visibility rules.

It is enabled only after an in-app explanation and your explicit consent, and never
automatically. Revoke it at any time: Settings > Accessibility > Lagoon > Off. The island
then falls back to Standard mode.

The accessibility service is included in the FOSS build (GitHub, F-Droid). The Google Play
build includes it only once Google has approved the required declaration.

## Children

Lagoon is not directed at children and has no content rating concerns; it displays whatever
your other apps already show you.

## Changes

This file is versioned with the source code. Any change to what Lagoon accesses will be
visible in the git history and in the permission list of the app itself.

## Contact

Open an issue on the GitHub repository or email the address listed on the project's
GitHub profile.
