# PHIRA

**A little guidance. Your own eye.**

Native Android photography assistant by Muhammad Kika Febriyansyah. A restrained, warm-white interface, the supplied φ identity, and real camera guidance. This repository contains a working implementation to build and test, not a web mockup.

## Start here

1. Install Android Studio and open this repository as a Gradle project.
2. Use JDK 17; install Android SDK 35 when prompted. Let Gradle sync finish.
3. Connect an Android 10+ phone with USB debugging enabled. Select `app`, then Run.
4. Allow camera access. Aim at a face, choose a grid, and follow the framing prompt. Tap the shutter; photographs are saved in **Pictures/PHIRA**.

On Windows, clone first:

```powershell
git clone https://github.com/Yansyah0209/PHIRA.git
cd PHIRA
.\gradlew.bat :core:test :app:assembleDebug
```

For a build without installing Android Studio, open the repository's **Actions → Android** workflow. A successful run provides **PHIRA-debug-apk**; download and unzip it, then install `app-debug.apk` on your phone. This is a debug testing build, not a store release.

## Implemented

- CameraX native preview, tap-to-focus, pinch zoom, zoom controls, front/back switch, flash, JPEG capture to MediaStore.
- ML Kit face detection for Portrait/Group; primary-object detection for Object mode.
- Phi grid (0.382/0.618), thirds, center, and free framing. Detection markers share the preview coordinate system through CameraX ML Kit Analyzer.
- Geometric framing alignment and one instruction at a time. Device roll guidance using the gravity sensor, when available.
- Opt-in auto capture after 1.4 seconds of uninterrupted readiness, freshness checks, and a cooldown. Manual capture remains available.
- Photo Picker and portrait-photo review, including EXIF-aware decoding and bounded image size.
- Camera permission recovery, visible failures, lifecycle cleanup, saved grid/auto preferences.
- Android build/lint pipeline, JVM unit tests, and an emulator smoke/visual workflow.

## Boundaries of this release

Version **0.1.0 is an early-access foundation**, not a validated commercial release. The score measures alignment with a grid, not aesthetic quality. Portrait markers surround faces, not full bodies; framing checks cannot guarantee hands/feet are inside the photograph. Group guidance frames the union of detected faces. The object detector returns a primary object and does not understand scene intent. Roll is a device-level estimate, not a scene-horizon detector. Lighting, pose coaching, background distraction analysis, learned aesthetics, golden spiral, subscriptions and production signing are not implemented.

The portrait face model is bundled. Object detection may require an initial Google Play services model download, so object mode can be unavailable on first use or devices without supported services. No app backend, accounts, analytics, advertising, or photo-upload flow is configured. Review currently supports faces, regardless of the selected live-camera mode.

## Structure

```text
app/       Android UI, camera, ML Kit adapter, level sensor, photo review
core/      Android-independent composition engine and capture gate
docs/      Architecture, design decisions, device QA, release milestones
assets/    Original supplied logo and wrapper provenance
```

## Verification

```bash
./gradlew :core:test :app:assembleDebug :app:lintDebug
```

See [device QA](docs/device-qa.md) before publishing. Do not claim model accuracy, novelty, patentability, or commercial readiness from a passing build. Release decisions need real-device and user evaluation.
