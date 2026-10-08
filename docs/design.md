# Design direction

PHIRA borrows CYVRA's simple controls, strong typography, monochrome contrast, and clear information hierarchy. It translates those into a warm white camera tool rather than a desktop dashboard.

| Token | Value | Purpose |
|---|---|---|
| Paper | #FAF9F6 | Interface background |
| Ink | #252522 | Text and shutter |
| Muted | #777770 | Secondary labels |
| Line | #E3E2DC | Fine dividers |
| Sage | #648273 | Aligned state |

The original supplied raster logo is preserved in `assets/phira-logo-original.png`. `ic_phira.xml` is a scalable interpretation of its upright stem and open curved ring, adapted to the light interface; it is not an exact vector tracing of the raster. There are no ornamental gradients, artificial charts, decorative AI badges, or arbitrary confidence scores. The camera image is the center of the interface. Guide lines are deliberately faint. One instruction appears below the viewfinder. Scores are labeled **framing alignment**.

Screen sequence: permission introduction → camera → settings or photo review → return to camera. Camera controls include real shutter, zoom, focus, flash, and camera switch. Settings only expose implemented features. A user can disable the grid and automatic capture.

UI layout uses safe drawing insets and sizes the 3:4 viewfinder to the available space. Before release, validate small screens, 200% font scaling, translations, TalkBack order, contrast over bright camera scenes, and OEM camera controls. English is the current in-app language; Indonesian localization is a planned milestone.
