# 📸 NEXPAD Android — Compose Preview Screenshot Testing Audit

**Audit Date:** 2026-10-02  
**Target Environment:** Android Studio Rabbit 2 (2026.2.2) | AGP 9.3.1 | Kotlin 2.4.20 | Compose BOM 2026.08.00  
**Overall Validation Status:** **PASSED (45 / 45 Tests — 100% Pixel Match)**

---

## 1. Executive Summary

This audit establishes host-side visual regression testing for NEXPAD Android using Google's **Compose Preview Screenshot Testing Tool**. All visual controller components—face buttons, directional pads, analog joysticks, shoulder bumpers, analog triggers, system buttons, macro paddles, touchpads, modal popups & dialogs, full screen UI cards, typography scaling, and adaptive layout form factors—now have deterministic, host-rendered golden master reference images.

```
Total Test Cases:       45
Errors:                 0
Failures:               0
Skipped:                0
Success Rate:           100%
Validation Run Time:    ~3.8 seconds
```

---

## 2. Infrastructure & Configuration

### Build Configuration Changes

1. **`gradle.properties`**:
   ```properties
   android.experimental.enableScreenshotTest=true
   ```

2. **`gradle/libs.versions.toml`**:
   ```toml
   [versions]
   screenshot = "0.0.1-alpha16"

   [libraries]
   screenshot-validation-api = { group = "com.android.tools.screenshot", name = "screenshot-validation-api", version.ref = "screenshot" }

   [plugins]
   screenshot = { id = "com.android.compose.screenshot", version.ref = "screenshot" }
   ```

3. **`app/build.gradle.kts`**:
   ```kotlin
   plugins {
       alias(libs.plugins.screenshot)
   }

   android {
       experimentalProperties["android.experimental.enableScreenshotTest"] = true
   }

   dependencies {
       screenshotTestImplementation(libs.screenshot.validation.api)
       screenshotTestImplementation(libs.androidx.compose.ui.tooling)
   }
   ```

---

## 3. Test Suites & Component Coverage

All screenshot tests live in the dedicated source set:
`app/src/screenshotTest/kotlin/com/sanket/tools/nexpad/screenshot/`

### 3.1. FaceButtonsScreenshotTest (9 Tests)
* **`realisticButtonsXboxPreview`**: Standard ABXY cluster in Xbox layout with high-contrast acrylic bevels.
* **`realisticButtonsPlayStationPreview`**: Cross, Circle, Square, and Triangle symbols with authentic PlayStation glyph geometry.
* **`liquidButtonsPreview`**: Meniscus wave fluid-fill buttons with underwater text lighting.
* **`facetButtonsPreview`**: 45° rotated diamond gem buttons with counter-rotated typography.
* **`flipButtonsPreview`**: 3D card-flip face buttons with neon backplates.
* **`rippleButtonsPreview`**: Triple concentric pulsating kinetic ring waves.
* **`orbitButtonsPreview`**: Dual counter-rotating orbital dashed rings with spring kinematics.
* **`capsulesButtonsPreview`**: Pill-shaped horizontal and vertical capsule buttons.
* **`eclipseButtonsPreview`**: Corona-glow sliding eclipse disc buttons.

### 3.2. DPadScreenshotTest (7 Tests)
* **`realisticDPadPreview`**: Traditional cross directional pad with center pivot recess.
* **`discDPadPreview`**: 8-way directional ribbed concave disc D-Pad.
* **`railsDPadPreview`**: Industrial elevated parallel rail tracks with neon indicator conduits.
* **`lensDPadPreview`**: Convex glass bubble lens D-Pad with specular highlight arc.
* **`fourLensesDPadPreview`**: Quad discrete circular lens nodes with directional neon glyphs.
* **`metaballsDPadPreview`**: Organic fluid metaball junctions with central glow core.
* **`capsulesDPadPreview`**: Dual interlocking capsule pills with neon border accents.

### 3.3. BumpersTriggersScreenshotTest (4 Tests / 14 Variants)
* **`realisticBumpersPreview`**: L1/R1 tactile curved shoulder bumpers.
* **`realisticTriggersPreview`**: L2/R2 analog plunge triggers with resistance travel.
* **`bumpersShowcasePreview`**: Multi-variant matrix (`Arc`, `Led`, `Peek`, `Ribbed`, `Underglow`, `Tube`, `Flip`).
* **`triggersShowcasePreview`**: Multi-variant matrix (`Dial`, `LiquidOrb`, `VuSlabs`, `Target`, `Slider`, `Needle`, `TestTube`, `Bloom`).

### 3.4. JoysticksScreenshotTest (6 Tests / 12 Thumbsticks)
* **`realisticJoysticksPreview`**: Left and Right dual-texture rubber dome thumbsticks.
* **`fluxJoysticksPreview`**: High-performance dual magnetic flux rings with neon cyan/magenta styling.
* **`orbJoysticksPreview`**: Glowing central energy orb with planetary orbit tracks.
* **`compassJoysticksPreview`**: Multi-axis tactical navigation dial with crosshair bearings.
* **`gyroJoysticksPreview`**: Dual gimbal concentric gyroscope rings.
* **`spotlightJoysticksPreview`**: Directional parabolic spotlight beam with neon accent rings.

### 3.5. SystemControlsScreenshotTest (4 Tests)
* **`systemButtonsPreview`**: Guide (⨂), Start (☰), Back (⧉), and Share (⇪) system buttons.
* **`orbitHomeButtonPreview`**: 74dp oversized focal circle with 3-segment dashed orbital tracks (RGB Enabled vs Monochrome).
* **`macroButtonsPreview`**: M1, M2, M3, M4 back-paddle micro-switches.
* **`touchPadPreview`**: Dual LTP and RTP center trackpads with textured touch surface.

### 3.6. PopupsAndDialogsScreenshotTest (3 Tests)
* **`confirmDialogPreview`**: NEXPAD glassmorphism confirmation modal dialog with warning accent and destructive/confirm action buttons.
* **`inputDialogPreview`**: Profile renaming and text input modal with outlined text field and cyber-styled buttons.
* **`buttonPaletteDialogPreview`**: HUD layout button skin selector palette grid showcasing face button skin options.

### 3.7. ScreensUiScreenshotTest (5 Tests)
* **`deviceHeroCardSearchingPreview`**: Connection screen hero radar card in active searching state with pulsating beacon indicator.
* **`deviceHeroCardConnectedPreview`**: Connection screen hero card in active connected state displaying transport badge, low-latency telemetry (0.4ms), and battery status.
* **`layoutProfileCardPreview`**: Custom controller layout preset card displaying layout metadata, thumbnail container, and quick-action menu.
* **`vShapedCarouselPanelPreview`**: 3D angled carousel panel preview showcasing depth styling and active preset indicator.
* **`hudTopBarPreview`**: HUD layout editor header toolbar with action controls, profile dropdown, and save/exit buttons.

### 3.8. FontAndTypographyScreenshotTest (3 Tests)
* **`standardTypographyPreview`**: System typography hierarchy rendered at standard 1.0x font scaling across headings, labels, telemetry counters, and captions.
* **`accessibilityLargeFontTypographyPreview`**: Full typography verification under high-accessibility **1.5x font scale**, ensuring zero clipping, text overlapping, or container overflow.
* **`controllerLabelsComparisonPreview`**: Side-by-side comparison of Xbox (A, B, X, Y) and PlayStation (✕, ○, □, △) label sets and custom glyph alignments.

### 3.9. AdaptiveLayoutScreenshotTest (4 Tests)
* **`compactPhonePortraitPreview`**: Responsive layout rendered on **Compact Phone Portrait** ($360 \times 740\,\text{dp}$) verifying stacked ergonomics.
* **`mediumPhoneLandscapePreview`**: Ultra-wide **Medium Phone Landscape** ($840 \times 390\,\text{dp}$) gamepad layout with dual thumb ergonomics and shoulder button spacing.
* **`expandedTabletLandscapePreview`**: Large screen **Expanded Tablet Landscape** ($1200 \times 800\,\text{dp}$) split-pane layout with expanded control clusters.
* **`foldableUnfoldedSquarePreview`**: Book-style foldable **Foldable Unfolded** ($680 \times 800\,\text{dp}$) verifying adaptive edge anchoring.

---

## 4. Storage & Output Artifacts

* **Golden Reference Images (45 files):**
  `app/src/screenshotTestDebug/reference/com/sanket/tools/nexpad/screenshot/`
* **Interactive HTML Validation Report:**
  `app/build/reports/screenshotTest/preview/debug/index.html`

---

## 5. Developer Workflow

### In Android Studio Rabbit 2 (IDE Integration)
* **Run Single Preview:** Click the green gutter run icon next to any `@PreviewTest` in the editor.
* **Inspect Visual Diff:** Open the **Run** panel and click on the **Screenshot** tab to view Reference, Actual, and Diff side-by-side with zoom and pan.
* **Update Reference Images:** Right-click the test file or click the gutter icon and select **Add/Update Reference Images**.

### From Command Line / Terminal
* **Regenerate Golden Reference Images:**
  ```powershell
  .\gradlew.bat updateDebugScreenshotTest
  ```
* **Verify / Run Regression Validation:**
  ```powershell
  .\gradlew.bat validateDebugScreenshotTest
  ```
