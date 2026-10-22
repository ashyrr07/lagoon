# Cutting a release

Three channels, one source: GitHub Releases (signed FOSS APK, built by CI from a tag),
Google Play (`play` flavor bundle, uploaded by hand) and F-Droid (built and signed by
F-Droid's own servers from the tag). Everything keys off the git tag and the version in
`app/build.gradle.kts`.

## 0. One-time setup: the release keystore (human, never in git)

```bash
keytool -genkeypair -v \
  -keystore lagoon-release.jks -alias lagoon \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Lagoon, O=Lagoon, C=US"
```

- Store `lagoon-release.jks` **outside the repository** (a password manager attachment plus
  an offline copy). Losing it means GitHub/F-Droid users can never update in place; `.gitignore`
  blocks `*.jks`, `*.keystore` and `keystore.properties` as a safety net.
- Local signing: create `<repo>/keystore.properties` (gitignored):

  ```properties
  storeFile=/Users/you/secure/lagoon-release.jks
  storePassword=...
  keyAlias=lagoon
  keyPassword=...
  ```

  `app/build.gradle.kts` picks it up automatically; without the file release builds are
  unsigned and still compile.
- GitHub Actions secrets (repository Settings > Secrets and variables > Actions):

  | Secret | Value |
  |---|---|
  | `KEYSTORE_BASE64` | `base64 -i lagoon-release.jks \| pbcopy` (macOS) / `base64 -w0 lagoon-release.jks` (Linux) |
  | `KEYSTORE_PASSWORD` | keystore password |
  | `KEY_ALIAS` | `lagoon` |
  | `KEY_PASSWORD` | key password |

  The workflow works without them and then publishes an unsigned APK with a warning, which
  is what forks get.
- **Google Play App Signing**: Play re-signs the `play` bundle with a Google-held key; the
  key above becomes the *upload key*. Enrol in Play App Signing when creating the app (it is
  mandatory for new apps). Keep the upload key the same as the FOSS key or a separate one;
  separate is slightly safer (an upload key can be reset through Play support, the FOSS key
  cannot).

## 1. Version bump

1. `app/build.gradle.kts`: `versionCode += 1`, `versionName = "X.Y.Z"` (semver; a `-beta.N`
   suffix makes the GitHub Release a pre-release).
2. Write `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500
   characters, plain text; F-Droid and Play both show it).
3. Update `README.md` feature table if something moved from planned to done.
4. Commit: `Release X.Y.Z`.

## 2. Pre-release checks

- `./gradlew testFossReleaseUnitTest lintFossRelease assembleFossRelease bundlePlayRelease`
  locally with `keystore.properties` present, then install the signed FOSS APK on the Samsung
  and the Pixel (the two must-pass devices) and run the real-device checklist from the plan:
  cutout alignment, status-bar swipe and tap band, dialer and Clock parsing, earbuds alert,
  keep-alive overnight, battery use < 1 %/day.
- `grep -c INTERNET app/build/intermediates/merged_manifest/fossRelease/*/AndroidManifest.xml`
  prints `0`.
- `docs/privacy.md` and `docs/play-declarations.md` still match the manifest.

## 3. Tag and let CI build

```bash
git tag -a vX.Y.Z -m "Lagoon X.Y.Z"
git push origin main vX.Y.Z
```

`.github/workflows/release.yml` then:

1. Checks the tag equals `versionName` (fails otherwise).
2. Restores the keystore from secrets (or warns and builds unsigned).
3. Runs unit tests, `assembleFossRelease` and `bundlePlayRelease`.
4. Fails if either merged manifest contains `android.permission.INTERNET`.
5. Verifies the APK signature with `apksigner`.
6. Uploads `lagoon-X.Y.Z-foss.apk`, `lagoon-X.Y.Z-foss-mapping.txt` and `SHA256SUMS.txt`
   to a GitHub Release whose body is the changelog entry for that `versionCode`.
7. Keeps `lagoon-X.Y.Z-play.aab` and both `mapping.txt` files as a 90-day workflow artefact
   (not public: the bundle is for Play only, and Play wants the mapping file uploaded
   alongside it for its crash deobfuscation).

Users verify downloads with `sha256sum -c SHA256SUMS.txt`. Keep every `mapping.txt`
forever (download the artefact and store it next to the keystore); without it a crash from
that version cannot be read.

## 4. Google Play

New personal developer accounts must pass a **closed test with at least 12 testers opted in
for 14 consecutive days** before they can apply for production access. Plan it in:

1. Play Console > Create app: name Lagoon, app, free, default language en-US. Accept the
   developer programme policies.
2. App content: paste every section from `docs/play-declarations.md` (privacy policy URL,
   Data safety, ads, target audience, content rating, foreground service declaration; the
   accessibility declaration only once the `play` manifest declares the service).
3. Store listing: title, short and full description from `fastlane/metadata/android/en-US/`,
   icon 512x512, feature graphic 1024x500, at least 2 phone screenshots (ideally the 8 listed
   in `images/README.md`).
4. Testing > Closed testing > create a track, upload the `play` bundle from the workflow
   artefact, upload the matching `lagoon-X.Y.Z-play-mapping.txt` under App bundle explorer >
   Downloads > mapping file (or at upload time).
5. Add testers (email list or Google Group), share the opt-in link, and keep **12 or more
   opted-in testers for 14 days without a gap**. A tester who leaves resets nothing but the
   count must never drop below 12. Fix bugs with new closed-test releases during the window;
   that does not restart it.
6. After 14 days: Dashboard > *Apply for production access*, answer the questionnaire
   (what was tested, what feedback came, what changed). Approval usually takes a few days.
7. Production: promote the closed-test release or upload a new bundle; roll out to 100 %
   (staged rollout is pointless for a utility with no server).
8. Play policy reminders: target SDK 36 is required for new apps and updates from
   31 Aug 2026 (`targetSdk = 36` is set); declarations are re-reviewed when the manifest's
   permission set changes; a video is required for the foreground-service and accessibility
   declarations.

## 5. F-Droid

F-Droid builds from source on its own servers and signs with its own key, so the APK from
F-Droid and the one from GitHub are different signatures and cannot update each other
(users must pick one source). Reproducible builds would unify them; that is a later task.

1. Make sure a tag exists and the `foss` flavor builds with `./gradlew assembleFossRelease`
   from a clean clone with no `keystore.properties`.
2. Fork https://gitlab.com/fdroid/fdroiddata and add `metadata/com.lagoon.island.yml`:

   ```yaml
   Categories:
     - System
   License: GPL-3.0-or-later
   SourceCode: https://github.com/ashyrr07/lagoon
   IssueTracker: https://github.com/ashyrr07/lagoon/issues
   Changelog: https://github.com/ashyrr07/lagoon/releases

   AutoName: Lagoon

   RepoType: git
   Repo: https://github.com/ashyrr07/lagoon.git

   Builds:
     - versionName: X.Y.Z
       versionCode: N
       commit: vX.Y.Z
       subdir: app
       gradle:
         - foss

   AutoUpdateMode: Version
   UpdateCheckMode: Tags
   CurrentVersion: X.Y.Z
   CurrentVersionCode: N
   ```

   The description, screenshots and changelogs are read from
   `fastlane/metadata/android/` in the tagged commit, so nothing is duplicated.
3. `fdroid lint com.lagoon.island` and `fdroid build com.lagoon.island:N` in the fdroidserver
   docker image, then open the merge request. Expect the reviewers to check for non-free
   dependencies (there are none: Hilt, Compose, Coil, kotlinx are all Apache-2.0) and for the
   absence of the Play dependency-metadata blob (`dependenciesInfo { includeInApk = false }`
   is set).
4. After the first merge, F-Droid picks up later releases from tags automatically
   (`AutoUpdateMode: Version`), as long as `versionCode`/`versionName` are bumped in
   `app/build.gradle.kts` in the tagged commit.

## 6. After the release

- Announce in the GitHub Release (CI generates the notes; edit them if needed).
- Close the milestone, open the next one.
- Store the `mapping.txt` files and the workflow run URL with the keystore backup.
