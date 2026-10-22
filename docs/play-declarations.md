# Google Play Console declarations

Ready-to-paste text for the Play Console (App content, Data safety, Permissions
declarations). Keep this file in sync with the manifest; reviewers compare the text with
the uploaded bundle. Where a field has a character limit it is noted.

Upload the `play` flavor bundle (`bundlePlayRelease`). It declares **no accessibility
service** until the accessibility declaration below is approved; after approval, move the
service entry from `app/src/foss/AndroidManifest.xml` into `app/src/play/AndroidManifest.xml`
and resubmit.

---

## 1. Privacy policy URL

```
https://github.com/ashyrr07/lagoon/blob/main/docs/privacy.md
```

Use the raw GitHub URL or a GitHub Pages URL if the reviewer wants a page without the
GitHub chrome; both are acceptable.

---

## 2. Data safety

Answer the questionnaire exactly like this. Lagoon has no INTERNET permission, so every
"collect or share" answer is "No".

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable (no data is transmitted) |
| Do you provide a way for users to request that their data is deleted? | Not applicable (no data is collected). If the form insists, answer "Yes" and link to privacy.md, which explains that uninstalling removes all local settings. |
| Does your app use any third-party SDKs that collect data? | No |

Data types: tick nothing. Resulting store listing: "No data shared with third parties", "No
data collected".

Justification text if a reviewer asks (there is no field for it; use the appeal / contact
form):

> Lagoon does not declare the INTERNET permission. The app has no network stack, no
> analytics, no crash reporting and no accounts. Notification content and media metadata
> are displayed on an overlay and held in memory only while shown. Settings are stored in
> app-private storage with allowBackup=false.

---

## 3. App access

> All functionality is available without login. The app has no accounts.

Select "All or some functionality is restricted" = **No**.

---

## 4. Ads

> No ads. The app does not contain ads and does not include any advertising SDK.

---

## 5. Content rating (IARC questionnaire)

Category: **Utility, productivity, communication or other**. Answer "No" to every content
question (violence, sexuality, language, controlled substances, gambling, user interaction,
sharing location, purchases). Expected rating: Everyone / PEGI 3.

---

## 6. Target audience and content

Target age group: **18 and over** only (Lagoon is not designed for children and must not
be enrolled in the Families programme, which would prohibit the notification listener and
accessibility use cases). Answer "No" to "Could your app unintentionally appeal to
children?".

---

## 7. News app / COVID-19 / Government / Financial / Health

All **No**.

---

## 8. Notification listener (no Play form, but reviewers read the listing)

There is no dedicated declaration for `BIND_NOTIFICATION_LISTENER_SERVICE`, but the app
must show an in-app disclosure before sending the user to the system grant screen, and the
listing must explain the use. The in-app disclosure (onboarding, "Notification access"
screen) reads:

> **Lagoon needs Notification access**
>
> Lagoon reads your notifications to show them on the island: what is playing, who is
> calling, running timers, and messages from the apps you choose. Nothing is stored or sent
> anywhere; Lagoon has no internet access.
>
> On the next screen, turn on "Lagoon" under Notification access.
>
> [ Continue ]  [ Not now ]

Store listing paragraph (already in `fastlane/metadata/android/en-US/full_description.txt`):

> Notification access is used only to show your notifications and media on the island.

---

## 9. Foreground service: `FOREGROUND_SERVICE_SPECIAL_USE`

> **Not needed for version 0.1.0.** The optional keep-alive service was removed on 2026-10-23 (it showed a permanent "Lagoon is active" notification). The app declares no `FOREGROUND_SERVICE*` permission, so skip this form. Keep the text below for when the service comes back.


Play Console > App content > **Foreground service permissions**. Lagoon uses one foreground
service type, `specialUse`, for the optional keep-alive service.

**Task the foreground service performs** (pick "Other / special use"):

> Keeps an always-on-screen overlay (a "Dynamic Island" that wraps the camera cutout)
> responsive on devices whose power management repeatedly unbinds the app's
> NotificationListenerService. The service is optional, off by default, started only from
> the app's own Activity or after BOOT_COMPLETED when the user has enabled it, and shows a
> persistent notification the user can tap to stop it.

**Why no other foreground service type fits:**

> The island is a system-wide overlay driven by notification and media-session events. It
> is not media playback (the app plays nothing), not a connected device session, not
> location, not a phone call, not data sync and not a short service. The closest category,
> `mediaPlayback`, would be a misdeclaration because the app controls other apps' sessions
> and does not own one.

**Demo video**: record the flow Settings app > Health > "Keep-alive service" toggle ON >
persistent notification appears > island keeps working after swiping the app from recents >
toggle OFF > notification disappears. 30 to 60 seconds, no narration needed, upload as an
unlisted YouTube link.

Manifest property (added with the service in M6; the value must match this declaration):

```xml
<service android:name=".service.KeepAliveService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Keeps the Dynamic Island overlay alive on devices that unbind the notification listener. Optional, user-enabled, user-stoppable." />
</service>
```

---

## 10. Accessibility service declaration (`play` flavor, only when Enhanced mode is added)

Play Console > App content > **Accessibility API usage** (the "AccessibilityService API"
declaration). Submit this only with a bundle that actually declares the service.

**Is your app an accessibility tool (designed primarily for users with disabilities)?**
**No.** Do not set `isAccessibilityTool="true"`; Lagoon is a utility app.

**Describe the core functionality that requires the AccessibilityService API** (free text,
keep it under ~1000 characters):

> Lagoon draws a "Dynamic Island" overlay around the camera cutout. Android draws the
> status bar above ordinary app overlays, so in the default mode the island cannot receive
> taps inside the status-bar strip and cannot appear on the lock screen. The optional
> "Enhanced mode" uses an AccessibilityService solely to (1) host the same overlay as a
> TYPE_ACCESSIBILITY_OVERLAY window so the whole pill is tappable and visible on the lock
> screen, (2) perform the global actions screenshot, lock screen and recents from the
> island's control deck when the user adds those tiles, and (3) receive
> TYPE_WINDOW_STATE_CHANGED events to learn the foreground app's package name for the
> user's per-app visibility rules. The service declares canRetrieveWindowContent="false"
> and does not request flagRetrieveInteractiveWindows: it cannot read screen content. It is
> off by default, enabled only after an in-app prominent disclosure with explicit consent,
> and the app is fully functional without it.

**Why the functionality cannot be achieved with other APIs:**

> No other public API lets an app window receive touches inside the status bar area, draw
> above the keyguard, or trigger the screenshot/lock/recents global actions.
> TYPE_APPLICATION_OVERLAY windows are z-ordered below the status bar on all Android
> versions from 8 to 17, and only accessibility overlays are trusted above it.

**Prominent disclosure in the app** (Enhanced mode screen; must be shown before the
Accessibility settings button is enabled and the text must be visible in the video):

> **Enhanced mode uses an accessibility service**
>
> Lagoon's accessibility service is used to:
> - draw the island above the status bar and on the lock screen, so the whole pill is
>   tappable;
> - take a screenshot, lock the screen or open recents from the control deck;
> - detect which app is in the foreground (package name only) for your per-app rules.
>
> It does **not** read, record or send what is on your screen. Lagoon has no internet
> access. This service is not an accessibility tool and Enhanced mode is optional:
> everything else works without it.
>
> [x] I understand and want to enable Enhanced mode
>
> [ Open Accessibility settings ]   (disabled until the checkbox is ticked)

**Video script** (upload as an unlisted YouTube link; 60 to 90 seconds, screen recording,
no cuts between the disclosure and the system settings screen):

1. Open Lagoon. Show the Home screen with the island working in Standard mode (play music
   from the Demo screen so the island animates). This proves the app works without the service.
2. Go to Settings > Enhanced mode. Hold on the disclosure text for 5 seconds so it is
   readable in the video.
3. Tick the consent checkbox; the "Open Accessibility settings" button becomes enabled.
4. Tap it. The system Accessibility settings open on Lagoon's entry. Show the system
   description string (it references the same purposes). Turn the service on and confirm the
   system dialog.
5. Return to Lagoon. Show the island now responds to a tap on its status-bar part and
   appears on the lock screen (lock the phone, wake it).
6. Open the control deck, tap the Screenshot tile: the system screenshot animation plays.
7. Go back to Settings > Accessibility, turn Lagoon off. Show the island still works in
   Standard mode.

Accessibility service config these claims rely on (`res/xml/accessibility_service_config.xml`,
added in M7):

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="false"
    android:canPerformGestures="false"
    android:isAccessibilityTool="false"
    android:description="@string/enhanced_service_description"
    android:settingsActivity="com.lagoon.island.ui.app.MainActivity" />
```

`enhanced_service_description`:

> Optional Enhanced mode for the Lagoon island: draws the island above the status bar and
> on the lock screen, performs screenshot / lock / recents from the control deck, and reads
> the foreground app's package name for per-app rules. Does not read screen content.

---

## 11. Permissions that need no form (for the record)

| Permission | Form? | Note |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | No | Overlay apps are an accepted category; the listing says what the overlay is for. |
| `READ_PHONE_STATE` | No | Not in the Call Log permission group. Used for ringing/off-hook/idle only. |
| `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` | No | `neverForLocation` flag set, so no location declaration. |
| `WRITE_SETTINGS`, `ACCESS_NOTIFICATION_POLICY` | No | Granted via system screens. |
| `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `VIBRATE` | No | |
| `QUERY_ALL_PACKAGES` | Not requested | `<queries>` for `MAIN/LAUNCHER` intents instead. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Not requested | Would need a form; the Health screen links to the system battery settings instead. |

---

## 12. Store listing fields

| Field | Value |
|---|---|
| App name (30) | Lagoon |
| Short description (80) | `fastlane/metadata/android/en-US/short_description.txt` |
| Full description (4000) | `fastlane/metadata/android/en-US/full_description.txt` |
| Category | Tools |
| Tags | Customization, Notifications |
| Contact email | project email |
| Website | GitHub repository URL |
| Icon 512x512, feature graphic 1024x500, screenshots | `fastlane/metadata/android/en-US/images/` |

---

## 13. Testing requirements for a new personal developer account

Before production is available the app must run a **closed test with at least 12 testers
opted in continuously for 14 days**, then you apply for production access and answer a short
questionnaire about the test. Plan the closed test on the same bundle you intend to ship;
declarations (sections 9 and 10) are reviewed at that stage too, so submit them with the
first closed-test release. Details in `docs/release.md`.
