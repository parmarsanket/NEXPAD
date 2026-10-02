# 📸 NEXPAD Android — Compose Preview Screenshot Testing Audit

**Audit Date:** 2026-10-02  
**Target Environment:** Android Studio Rabbit 2 (2026.2.2) | AGP 9.3.1 | Kotlin 2.4.20 | Compose BOM 2026.08.00  
**Overall Validation Status:** **PASSED (30 / 30 Tests — 100% Pixel Match)**

---

## 1. Executive Summary

This audit establishes host-side visual regression testing for NEXPAD Android using Google's **Compose Preview Screenshot Testing Tool**. All visual controller components—face buttons, directional pads, analog joysticks, shoulder bumpers, analog triggers, system buttons, macro paddles, and touchpads—now have deterministic, host-rendered golden master reference images.

```
Total Test Cases:       30
Errors:                 0
Failures:               0
Skipped:                0
Success Rate:           100%
Validation Run Time:    ~1.3 seconds
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

---

## 4. Storage & Output Artifacts

* **Golden Reference Images:**
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
