# Architecture

## Runtime

`MainActivity` owns the Compose shell, permission recovery, saved preferences, and Photo Picker. A camera screen creates one `CameraSession`; leaving the screen removes the analyzer, unbinds the controller, and closes detectors. The screen listens for lifecycle changes to stop sensor updates and reset capture eligibility when paused.

`LifecycleCameraController` supplies camera use cases. `PreviewView` uses `FILL_CENTER`; `MlKitAnalyzer` with `COORDINATE_SYSTEM_VIEW_REFERENCED` transforms ML Kit detections to preview coordinates, including rotation/crop/front-camera mirroring. Only then does the adapter normalize bounds. Never normalize raw sensor coordinates directly against the view.

Portrait/Group use a bundled face detector. Object uses the ML Kit primary-object detector. Mode changes increment a generation token so delayed results from the previous detector cannot replace current observations. UI state changes occur on the main executor; model inference is asynchronous through ML Kit.

`CompositionEngine` accepts normalized observations and device roll. It produces a target, a geometric alignment score, and a single instruction. The target uses the nearest horizontal anchor, with upper-third/upper-phi portrait placement; Group uses the center of the union of detected faces. This is explicitly a face-framing heuristic. It is not full-body analysis or a learned aesthetic model.

`CaptureGate` requires an uninterrupted ready interval. UI capture eligibility additionally requires recent analysis (<800 ms), active lifecycle, an initialized camera, no capture in progress, no detector error, and a 4-second cooldown. It resets on mode/grid/front-camera changes and when returning from pause. Flash/zoom changes and scene motion still need device QA before production use.

JPEGs are written via scoped-storage MediaStore; Android 10+ needs no broad storage permission. Review decodes at a maximum dimension of 1600px off the main thread, honors image orientation through ImageDecoder, and uses a separate face detector. No image is uploaded by application code.

## Module boundaries

- `core`: deterministic, pure Kotlin geometry and temporal gate. No Android dependencies.
- `app`: platform adapters and Compose UI. CameraSession is a screen-scoped owner, not a singleton.

The next architecture increment should extract UI state to a ViewModel and use an injectable camera/detector interface before expanding modes. Do not introduce a backend until an actual product need requires one.

## Primary implementation references

- https://developer.android.com/media/camera/camerax/mlkitanalyzer
- https://developer.android.com/media/camera/camerax/transform-output
- https://developers.google.com/ml-kit/vision/face-detection/android
- https://developers.google.com/ml-kit/vision/object-detection/android
