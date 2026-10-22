# Keeping Lagoon alive on every brand

Lagoon's island lives inside the notification listener service, which Android itself binds
and rebinds. On stock Android that is enough. Several manufacturers add their own task
killers on top of Android's, and those can unbind the listener or freeze the process, after
which the island disappears until you open the app again.

This page lists the steps per brand. The in-app Health screen shows the same steps for the
phone it is running on and reports when the listener was last disconnected. Menu names
change between software versions; search Settings for the quoted words if a path does not
match.

If the island still dies after the steps below, turn on **Health > Keep-alive service**. It
starts a foreground service with a persistent notification, which every OEM treats as "do
not kill".

## First, on every phone

1. **Battery optimisation off.** Settings > Apps > Lagoon > Battery (or App battery usage) >
   *Unrestricted*. Lagoon does not request the exemption itself; the Health screen links to
   this screen after the listener was seen disconnecting.
2. **Do not remove Lagoon from the recents screen** on phones that kill swiped apps, or lock
   it in recents where the launcher offers that (Xiaomi, Oppo, Vivo, Huawei).
3. **Android 14+: "Pause app activity if unused"** (Settings > Apps > Lagoon) should be off,
   otherwise permissions are revoked after a few months without opening the settings app.
4. **Sideloaded builds (GitHub, F-Droid) on Android 13+:** if the notification access or
   accessibility toggle is greyed out, open Settings > Apps > Lagoon > three-dot menu >
   *Allow restricted settings*, then try again.
5. **Listener stuck?** Toggling Notification access off and on again (Settings >
   Notifications > Notification access > Lagoon) forces the system to rebind the service.

## Samsung (One UI)

- Settings > Battery > *Background usage limits* > turn **off** "Put unused apps to sleep".
- Same screen > *Never sleeping apps* > Add > Lagoon.
- Check *Sleeping apps* and *Deep sleeping apps*: remove Lagoon if it is listed.
- Settings > Apps > Lagoon > Battery > **Unrestricted**.
- Settings > Device care > Battery > More battery settings > *Adaptive battery* off is
  optional; it mostly affects apps you rarely open.
- Samsung's Good Guardians / Device care "auto optimisation" can close apps at night: in
  Device care > Auto optimisation, turn off "Close apps to free up memory" or exclude Lagoon.

## Google Pixel and other stock Android (Nothing, Motorola, Sony, Fairphone)

- Settings > Apps > Lagoon > App battery usage > **Unrestricted**.
- Motorola: Settings > Battery > *Background restriction* must not list Lagoon;
  *Adaptive battery* can stay on.
- Sony: Settings > Battery > *STAMINA mode* > exclude Lagoon, or keep STAMINA off.
- Nothing OS: Settings > Apps > Lagoon > Battery > Unrestricted is all that is needed.

## Xiaomi, Redmi, POCO (MIUI / HyperOS)

- Settings > Apps > Manage apps > Lagoon > **Autostart** on.
- Same screen > *Battery saver* > **No restrictions**.
- Same screen > *Other permissions* > "Display pop-up windows while running in the
  background" on (needed for the overlay to show while another app is in front).
- Recents: long-press Lagoon's card > **Lock**.
- Settings > Battery & performance > *Battery saver* off, or exclude Lagoon under
  *App battery saver*.
- Security app > *Boost speed* > settings: turn off "Lock screen cleanup" / add Lagoon to
  the exceptions.
- Developer options > **MIUI optimisation** should stay **on**; turning it off breaks
  overlays and notification access on several versions.

## Huawei and Honor (EMUI / HarmonyOS / MagicOS)

- Settings > Apps > Apps > Lagoon > Battery (or App launch) > turn **off** "Manage
  automatically" and turn on all three: *Auto-launch*, *Secondary launch*, *Run in
  background*.
- Settings > Battery > *App launch* (older versions): same three switches.
- Phone Manager (Optimizer) > *Lock screen cleanup* (or "Protected apps"): protect Lagoon.
- Settings > Battery > *Power-intensive prompt*: ignore prompts about Lagoon.
- Recents: pull Lagoon's card down to lock it.

## Oppo, Realme, OnePlus (ColorOS / Realme UI / OxygenOS 12+)

- Settings > Apps > App management > Lagoon > *Battery usage* > **Allow background
  activity**, **Allow auto launch**, and *Allow foreground activity*; set the mode to
  *Unrestricted* / "Don't optimise".
- Settings > Battery > *More settings* (or *App battery management*) > *Optimise battery
  use* > Lagoon > Don't optimise.
- Settings > Battery > *Sleep standby optimisation* off.
- Recents: tap the three-dot / lock icon on Lagoon's card > Lock.
- Settings > Apps > *Auto launch* / *Startup manager*: Lagoon on.
- OnePlus (OxygenOS 11 and older): Settings > Battery > Battery optimisation > Lagoon >
  Don't optimise; Settings > Apps > Lagoon > *Advanced* > "Enable auto-launch" and
  "Allow background activity".

## Vivo and iQOO (Funtouch OS / OriginOS)

- i Manager > *App manager* > *Autostart manager*: Lagoon on.
- Settings > Battery > *Background power consumption management* (or *High background
  power consumption*): allow Lagoon.
- Settings > Apps > Lagoon > Battery > *Allow background activity* / *High background
  power consumption* on.
- Recents: pull Lagoon's card down to lock it (padlock icon).

## Asus (ZenUI)

- Mobile Manager (PowerMaster) > *Auto-start manager*: Lagoon allowed.
- PowerMaster > *Settings* > turn off "Clean up in suspend" and "Auto-deny apps from
  auto starting".
- Settings > Battery > Battery optimisation > Lagoon > Don't optimise.

## Lenovo and others not listed

Start with "First, on every phone". Then look for *Autostart*, *Background activity*,
*App lock in recents* and *Battery optimisation* in Settings. The community-maintained
guide at https://dontkillmyapp.com has steps for brands not covered here; if you find a
working recipe for your phone, open a pull request against this file.

## How Lagoon itself helps

- The island is hosted by the notification listener, which the system rebinds after it is
  killed; no foreground service is required in normal operation.
- `RECEIVE_BOOT_COMPLETED` restores the overlay after a reboot.
- The Health screen records listener disconnects and offers the battery-optimisation screen
  and the brand guide only after a disconnect was observed, to keep onboarding short.
- The optional keep-alive foreground service (`specialUse`) is the last resort and is
  started only from the app's own Activity or the boot receiver, as Android 15 requires.
