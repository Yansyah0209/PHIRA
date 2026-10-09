# Install PHIRA on Android

Requires Android 10 or newer. This is an early-access testing APK.

1. Open https://github.com/Yansyah0209/PHIRA/actions/workflows/android.yml and sign in to GitHub.
2. Select the latest successful run on **main** (green check). Avoid downloading a failed or in-progress run.
3. Scroll to **Artifacts**, then tap **PHIRA-debug-apk**. GitHub downloads a ZIP, not an APK.
4. Extract the ZIP in your phone's Files app. Open **PHIRA-early-access.apk**. Older runs contain **app-debug.apk** instead.
5. If Android asks, allow **Install unknown apps** for the browser or Files app opening the APK. Install and open PHIRA.
6. Allow Camera permission. Start with **Portrait**, keep a face visible, and follow the framing prompt. Tap the shutter to save a photo.

## Using the app

- **Portrait / Group / Object** select the live subject detector. Object mode may require internet once for a Google Play services model download.
- Open camera settings to choose **Phi**, **Thirds**, **Center**, or **Free** framing and flash.
- Auto capture is optional and off by default. Hold a fresh, aligned frame for 1.4 seconds; move out of alignment before another automatic shot.
- Photos are saved in **Pictures/PHIRA** and should appear in your Gallery.
- The photo button opens your latest shot. To choose another image, open settings and choose **Choose a photo to review**.
- Photo review offers Portrait, Group, and Object modes and a **Share photo** button.
- **Done** or Android Back returns from review to the camera.
- The score measures geometric framing alignment, not artistic quality.

## If installation fails

- No Artifacts: confirm the run succeeded and you are signed in. Expired artifacts need a new build. **Run workflow** is available to repository maintainers.
- ZIP will not install: extract it first and open the APK inside.
- App not installed / conflicting package: debug builds from separate CI machines can have different signing keys. Back up anything needed before uninstalling the old app, then install the new APK. Photos already saved to Pictures/PHIRA remain in shared storage. App preferences will reset.
- Camera denied: open Android Settings → Apps → PHIRA → Permissions → Camera, then reopen PHIRA.
- Object analysis unavailable: connect to the internet and use a device with supported Google Play services. Portrait uses a bundled model.

The APK does not require Android Studio or USB debugging. This build is for trying the app; a production release needs a persistent release signing key and real-phone validation.
