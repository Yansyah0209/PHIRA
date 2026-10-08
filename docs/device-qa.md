# Device QA and release milestones

## Current automated checks

- Unit tests: no-subject states, malformed observations, alignment, movement direction, clipping, group union, tilt gating, interrupted readiness, hold/latch behavior.
- Android debug compilation and lint.
- Optional emulator smoke workflow: grant permission, launch app, open settings, inspect UI screenshots. Emulated camera output does not validate real-world inference or optics.

## Real-device acceptance

Test a low/mid/high-tier phone from at least three vendors, Android 10 through the current supported Android release:

1. Grant/deny/revoke camera permissions; return from system settings; recover from camera-in-use and camera disconnect.
2. Back/front cameras, mirroring, clipping, portrait/group/object detections. Match markers to subjects across zoom and focus changes. Verify pan instructions physically move subjects toward anchors.
3. Check image orientation, preview-versus-capture crop, flash, storage-full failures, repeated captures and gallery visibility.
4. Background/resume, screen lock, Settings and Photo Picker transitions; no auto capture while paused or on stale detections.
5. Test interrupted alignment, moving subject, sensor absence, lens pointed up/down and model-download failures.
6. Review rotated, large, HEIC and unsupported images. No crash/OOM; visible retry path.
7. Record preview FPS, inference latency p50/p95, memory, thermal throttling and 15-minute battery draw. Measure with actual devices; do not invent benchmark results.
8. Test TalkBack, font scaling, small screen and control reachability. English strings currently need localization before broader Indonesian launch.

## Commercial roadmap

| Milestone | Required outcome |
|---|---|
| 0.1 Foundation | Native camera, geometric coaching, debug build, basic tests |
| 0.2 Reliability | Device matrix, detector/preview alignment validation, measured performance, localization |
| 0.3 Intelligence | Lighting and full-body framing, background guidance, explicit uncertainty, dataset evaluation |
| 0.4 Beta | User study comparing assisted/unassisted photographs; privacy documentation; feedback and crash policy |
| 1.0 Release | Signed AAB, secure signing ownership, Play listing/data disclosures, release QA and support process |
| Monetization | Validated user demand, genuine Play Billing entitlements, restore/refund flows; no fake paywall |

Patent/prior-art evaluation is separate from software delivery. Naming and branding rights also need separate validation; no availability claim is made by this implementation.
