# NEXPAD — Universal HTML/CSS to Native Jetpack Compose Blueprint
## AI System Directive & Master Engineering Protocol

> [!IMPORTANT]
> **PURPOSE OF THIS BLUEPRINT**  
> This document is an **operational directive and instruction manual for AI agents**. It governs the exact process of translating **ANY HTML/CSS gamepad control design** into production-grade, resolution-independent **Jetpack Compose** components for the NEXPAD platform.  
> **This file is NOT for storing code dumps, component catalogs, or bloated Kotlin files.**  
> Follow these instructions step-by-step whenever given an HTML/CSS controller mock or component design request.

---

## Table of Contents
1. [Core AI Principles & Non-Negotiable Rules](#1-core-ai-principles--non-negotiable-rules)
2. [Step-by-Step HTML-to-Compose Translation Methodology](#2-step-by-step-html-to-compose-translation-methodology)
3. [The Universal CSS-to-Compose Property Mapping Matrix](#3-the-universal-css-to-compose-property-mapping-matrix)
4. [Kinematic Physics & Touch Interaction Engines](#4-kinematic-physics--touch-interaction-engines)
5. [The Universal 7-Layer Display List Pipeline](#5-the-universal-7-layer-display-list-pipeline)
6. [Bespoke Outer Aura Architecture (`.drawBehind`)](#6-bespoke-outer-aura-architecture-drawbehind)
7. [Developer & Verification Tooling Pipeline](#7-developer--verification-tooling-pipeline)
8. [The Mandatory 6-Step End-to-End Wiring Pipeline](#8-the-mandatory-6-step-end-to-end-wiring-pipeline)
9. [Canonical Implementation Pattern Templates](#9-canonical-implementation-pattern-templates)
10. [Pre-Commit Quality Assurance Checklist](#10-pre-commit-quality-assurance-checklist)

---

## 1. Core AI Principles & Non-Negotiable Rules

Whenever converting HTML/CSS into native Compose or implementing controller features, you MUST remember and enforce these seven hard rules:

### Rule 1: Zero `@Suppress` and Zero `@SuppressLint` (Hardest Constraint)
- **Zero tolerance**: There must be **0 `@Suppress`** and **0 `@SuppressLint`** across all of `app/src/`.
- Never bypass linter warnings or compiler diagnostics by adding suppression annotations. Fix the underlying issue (e.g. correct types, proper lifecycle calls, modern Android SDK API gates).

### Rule 2: Non-Destructive Component Creation
- **Never overwrite, replace, or delete existing working components** unless explicitly commanded by the user.
- If the user provides an HTML mock for an existing category (e.g., a new Guide/Home button or a new Trigger), create a **brand-new dedicated file** (e.g. `OrbitHomeButton.kt`, `VUTrigger.kt`).
- Keep baseline implementations (like `RealisticSystemButton.kt` with its 4-button suite: Back, Guide, Start, Share) fully intact.

### Rule 3: Unified Composite Cluster Rule
- Multi-element controls (such as a D-Pad with 4 directional arms or an analog joystick with socket base + thumb cap) must **NEVER be split into separate disconnected button files**.
- Always encapsulate composite controls into a single unified Composable mapped to the parent key (e.g., `ControlKey.DPAD`, `ControlKey.LS`).

### Rule 4: Resolution Independence & Pure Vector Rendering
- **No static raster bitmaps**: Never use PNGs, JPEGs, or web assets for controller faces.
- Render all iconography, shapes, and textures directly in native Compose via `Canvas`, `DrawScope`, and mathematical `Path` geometry.
- Scale all dimensions using `.dp.toPx()` and `LocalDensity.current` so components scale identically across any screen density (MDPI through XXXHDPI).

### Rule 5: Zero Hitbox Distortion
- The visual press movement (`Modifier.offset`, `graphicsLayer`) and outer aura (`Modifier.drawBehind`) must **NEVER modify or expand the component's touch hitbox**.
- Gesture detection (`detectTapGestures`, `pointerInput`) must be anchored to the stable root bounding box.
- Multi-directional controls must enforce strict shaft-box filtering: corners outside active arms must be non-clickable voids.

### Rule 6: Bespoke Outer Aura Standard
- Every component must feature a thematic outer aura rendered in Layer 0 via `Modifier.drawBehind { ... }`.
- Outer gradients must strictly fade to `Color.Transparent` before the component boundary ends to guarantee zero layout boundary clipping.
- The aura must be dynamically modulated by interaction state (`rgbBloomAlpha`, stick deflection, analog pull depth) and strictly gated behind `if (isRgbEnabled)`.

### Rule 7: Zero Compiler Warnings & Type Safety
- Watch for redundant type conversions (e.g. calling `.toDouble()` on an already double value), unused imports, and deprecated Compose APIs.
- The build must compile cleanly with `./gradlew compileDebugKotlin testDebugUnitTest`.

---

## 2. Step-by-Step HTML-to-Compose Translation Methodology

Follow this step-by-step algorithm when converting any input HTML/CSS file into native Compose:

```
 ┌────────────────────────────────────────────────────────┐
 │ 1. Ingest HTML/CSS & Deconstruct DOM Hierarchy          │
 └───────────────────────────┬────────────────────────────┘
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ 2. Adapt Dimensions: Desktop px → Mobile Controller dp │
 └───────────────────────────┬────────────────────────────┘
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ 3. Classify Visual Art Style (Optical, Metal, Neon...) │
 └───────────────────────────┬────────────────────────────┘
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ 4. Extract Mathematical Tokens: Gradients, Radii, Timings│
 └───────────────────────────┬────────────────────────────┘
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ 5. Select Kinematic Engine (Spring, 3D Rocker, Polar)  │
 └───────────────────────────┬────────────────────────────┘
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ 6. Assemble Composable via 7-Layer Display List        │
 └────────────────────────────────────────────────────────┘
```

### Stage 1: Ingest & Deconstruct HTML Structure
1. **Container & Shell**: Identify the main wrapper (e.g. `.lx`, `.btn`, `.pad`) and its border-radius, background, and drop-shadows.
2. **Body & Actuator**: Identify the moving element (`.lx-body`, `.thumb`, `.cross`).
3. **Pseudo-Elements**: Inspect `::before` and `::after` tags. These almost always represent:
   - Inset shadow wells (`::before` with inset box-shadow)
   - Optical glass crescent highlights or specular edge sheens (`::after` with border-top/gradient)
4. **Active/Pressed States**: Inspect `.pressed`, `:active`, and JavaScript event listeners to determine which CSS properties mutate on touch (e.g. `transform: translateY(2px) scale(0.95)`, `box-shadow` depth collapse, color shift).
5. **SVG Geometry**: If an SVG is present, inspect `viewBox`, `cx`, `cy`, `r`, `pathLength`, `stroke-dasharray`, and `d` path strings:
   - Calculate exact angular spans: $\theta_{\text{dash}} = \frac{\text{dash}}{\text{pathLength}} \times 360^\circ$.
   - Calculate gap spans: $\theta_{\text{gap}} = \frac{\text{gap}}{\text{pathLength}} \times 360^\circ$.

### Stage 2: Dimension Adaptation (Desktop px $\to$ Mobile dp)
HTML mocks are often designed on large desktop canvases ($96\text{px} \dots 200\text{px}$). Always translate dimensions into ergonomic mobile gamepad proportions:

| Controller Component | Target Mobile Controller Size | Typical Sizing Rationale |
|---|---|---|
| **Action Buttons (ABXY)** | $60\text{dp} \dots 80\text{dp}$ circle | Standard thumb reach; default is $80\text{dp}$ |
| **D-Pad Cluster** | $150\text{dp} \times 150\text{dp} \dots 160\text{dp} \times 160\text{dp}$ | Full 4-arm directional cross |
| **Analog Joystick** | $140\text{dp} \times 140\text{dp} \dots 150\text{dp} \times 150\text{dp}$ | Base socket ($140\text{dp}$) with $70\text{dp}$ thumb cap |
| **Shoulder Bumpers (LB/RB)**| $154\text{dp} \times 48\text{dp} \dots 56\text{dp}$ | Horizontal asymmetric or curved pill contour |
| **Analog Triggers (LT/RT)** | $92\text{dp} \times 92\text{dp} \dots 100\text{dp} \times 92\text{dp}$ | Compact mobile vertical clearance below bumpers |
| **System Buttons (View/Menu)**| $60\text{dp} \times 60\text{dp}$ | Standard secondary navigation button |
| **Guide / Home Nexus** | $74\text{dp} \times 74\text{dp}$ | Prominent central console nexus |

### Stage 3: Visual Style Classification
Identify which of the 5 universal styles the HTML represents and apply its native formula:
- **Style A: Optical Lens / Acrylic Dome**:
  Multi-stop radial gradient with off-center zenith (`Offset(0.50f, 0.55f)`), bottom undercut shadow, emissive neon lens ring, top specular crescent arc.
- **Style B: Brushed Metal / Chamfered Industrial**:
  Conic gradient highlights, chamfered rim stroke with dual-tone gradient, linear textures.
- **Style C: Retro Mechanical Arcade**:
  Solid offset drop shadows without blur, concave dish cap gradient, chunky 3–5dp border bevels.
- **Style D: Cyberpunk Neon / Wireframe**:
  Multi-tier emissive bloom (atmospheric aura $\to$ core bloom $\to$ laser filament core).
- **Style E: Glassmorphism / Frosted Acrylic**:
  Translucent fill (`White.copy(0.08f)`), specular diagonal border gradient, soft inner sub-surface diffusion.

---

## 3. The Universal CSS-to-Compose Property Mapping Matrix

| CSS Property Pattern | Native Jetpack Compose Equivalent | Implementation Formula & Nuance |
|---|---|---|
| `radial-gradient(circle at X% Y%, c1, c2, c3)` | `Brush.radialGradient(...)` | `center = Offset(w * X/100f, h * Y/100f), radius = R`<br>Anchors light source highlight zenith |
| `linear-gradient(direction, c1, c2)` | `Brush.verticalGradient(...)` or `horizontalGradient` | Invert Y-coordinates if CSS flows upward; set explicit `startY` and `endY` |
| `box-shadow: 0 4px 8px rgba(0,0,0,0.5)` | `Modifier.shadow(elevation, shape, ambientColor, spotColor)` | Native Android elevation shadow with RGB tinting support |
| `box-shadow: inset 0 -6px 9px rgba(0,0,0,0.7)` | `Canvas: drawRect(Brush.verticalGradient(...))` | Rendered over bottom $30\%\dots 40\%$ of height: `startY = h * 0.62f, endY = h` |
| `border: Npx solid var(--color)` | `Modifier.border(...)` or `Canvas: drawCircle / drawRoundRect` | Use `Stroke(width = N.dp.toPx())` inside Canvas for sub-pixel precision |
| `filter: drop-shadow(0 0 6px var(--glow))` | Dual-pass Canvas stroke rendering | Pass 1: `Stroke(4.dp)`, alpha $0.30 \to 0.60$ (halo bloom)<br>Pass 2: `Stroke(2.dp)`, alpha $0.70 \to 1.00$ (core line) |
| `transform: scale(s) translateY(y)` | `Modifier.graphicsLayer { scaleX = s; scaleY = s }.offset { IntOffset(0, y) }` | Plunging actuator travel; preserves hitbox bounds on outer Box |
| `transform: perspective(420px) rotateX(...) rotateY(...)` | `Modifier.graphicsLayer { rotationX = rx; rotationY = ry; cameraDistance = 8f * density }` | Physical 3D ball-joint rocker pivot tilt without squishing |
| `transition: transform 0.08s ease` | `animateFloatAsState(target, tween(80, easing = FastOutSlowInEasing))` | Snappy mechanical snap |
| `transition: transform 0.4s cubic-bezier(.3, 1.5, .5, 1)` | `animateFloatAsState(target, spring(dampingRatio = 0.52f, stiffness = 380f))` | Underdamped elastic spring with physical overshoot bounce |
| `SVG stroke-dasharray="dash gap"` | `Canvas: drawArc(...)` or `PathEffect.dashPathEffect` | Decompose to degrees: $\theta = (\text{dash} / \text{pathLength}) \times 360^\circ$ |

---

## 4. Kinematic Physics & Touch Interaction Engines

Controller components do not behave like standard mobile UI buttons. Select the exact kinematic engine for the archetype:

### 1. Damped Harmonic Spring (Plunging Buttons, Bumpers, Plungers)
Models physical button travel with spring resistance and tactile bottoming out:
```kotlin
val scaleAnim by animateFloatAsState(
    targetValue = if (isPressed) 0.95f else 1.0f,
    animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
    label = "btn_scale"
)
val pressOffsetYAnim by animateFloatAsState(
    targetValue = if (isPressed) 2f else 0f,
    animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
    label = "btn_offset"
)
```

### 2. Underdamped Spring with Overshoot Bounce (Cyclic Orbit / Rotary Buttons)
Translates CSS `cubic-bezier(.3, 1.5, .5, 1)` for rotating dials, orbit rings, and ratchet switches:
```kotlin
val rotationAnim by animateFloatAsState(
    targetValue = if (isPressed) 120f else 0f,
    animationSpec = spring(dampingRatio = 0.52f, stiffness = 380f),
    label = "rot_overshoot"
)
```

### 3. 3D Rocker Pivot (Directional Pads & Discs)
Cross D-Pads and directional discs are mounted on a central pivot ball. When pressed, the cross does **NOT shrink**; it tilts in 3D space:
```kotlin
Modifier.graphicsLayer {
    rotationX = rxAnim // e.g. +8f on UP, -8f on DOWN
    rotationY = ryAnim // e.g. +8f on RIGHT, -8f on LEFT
    cameraDistance = 8f * density // Replicates perspective: 420px
}
```

### 4. 2D Polar Clamping & Momentum Coasting (Analog Sticks)
Joysticks require boundary clamping and smooth recentering:
```kotlin
val dist = hypot(dx, dy)
val (clampedX, clampedY) = if (dist > maxRadius) {
    val angle = atan2(dy, dx)
    Pair(cos(angle) * maxRadius, sin(angle) * maxRadius)
} else {
    Pair(dx, dy)
}
// Deadzone filtering:
val normX = if (dist < deadzone) 0f else (clampedX / maxRadius).coerceIn(-1f, 1f)
val normY = if (dist < deadzone) 0f else (-clampedY / maxRadius).coerceIn(-1f, 1f)
```

### 5. Multi-Touch Hitbox & Cardinal Shaft Box Rules
- **Tactile Tap Pointer**: Use `detectTapGestures` with `onVibrate()` on down-press and `viewModel.updateButton(key, isPressed)` state updates.
- **Cardinal Shaft Filtering**: For cross D-pads, touches in diagonal corners outside physical arm rectangles must evaluate to non-clickable voids. Never trigger 2 cardinal buttons on a single diagonal tap unless the component is an explicit 8-way disc.

---

## 5. The Universal 7-Layer Display List Pipeline

Always structure Canvas rendering calls in this strict 7-layer order:

```
  Layer 6: Top Specular Glass Crescent / Lens Sheen Reflection
     ↑
  Layer 5: Tactical Typography / Center Vector Glyphs
     ↑
  Layer 4: Recessed Optical Window / Aperture Well
     ↑
  Layer 3: Dynamic Fill / Progress / Needle / Orbit / Fluid Layer
     ↑
  Layer 2: Tactile Knurling, Laser Ticks, Grips, or Ribs
     ↑
  Layer 1: Component Chassis (Acrylic Dome / Anodized Metal)
     ↑
  Layer 0: .drawBehind { ... } Bespoke Kinetic Outer Aura
```

- **Layer 0 (Aura)**: Drawn on the root Box via `.drawBehind`. Does not affect layout or hitboxes.
- **Layer 1 (Chassis)**: The base shape gradient + recessed bottom undercut shadow.
- **Layer 2 (Textures)**: Background knurling, tick marks, or radial track lines.
- **Layer 3 (Dynamic)**: Reactive active fills, surging fluid, audio meter slabs, or rotating orbit rings.
- **Layer 4 (Aperture)**: Optical windows or indicator wells.
- **Layer 5 (Glyphs)**: Crisp labels or normalized $24 \times 24$ vector icons (`drawSystemIcon`).
- **Layer 6 (Specular)**: Top $180^\circ$ glass crescent highlight and ambient sheen oval.

---

## 6. Bespoke Outer Aura Architecture (`.drawBehind`)

Generic drop shadows are insufficient for authentic physical controllers. Every component requires an outer aura matching its visual theme.

### Why `.drawBehind`?
1. **Zero Layout Thrash**: Executes directly on the graphics render layer behind the component without triggering separate measure or layout passes.
2. **Hitbox Isolation**: Drawing outside the bounding box via `.drawBehind` does not distort the touch target hitbox.
3. **120 FPS Kinetic Reactivity**: Draw passes have direct read access to dynamic animated state variables (`rgbBloomAlpha`, `rotationAnim`, `travelProgress`).
4. **Zero Clipping Guarantee**: The outermost gradient color stop must always be `Color.Transparent` at $0.65\times \dots 1.0\times$ component size.

### Canonical Aura Pattern:
```kotlin
.drawBehind {
    if (isRgbEnabled) {
        val auraRadius = size.minDimension * 0.85f
        
        // 1. Broad ambient corona
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    auraColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.35f)),
                    auraColor.copy(alpha = rgbBloomAlpha * 0.15f),
                    Color.Transparent
                ),
                center = center,
                radius = auraRadius
            ),
            radius = auraRadius,
            center = center
        )

        // 2. Thematic signature (e.g. telemetry rings, pips, laser slits, or orbital tracks)
        // Draw bespoke kinetic shapes here...
    }
}
```

---

## 7. Developer & Verification Tooling Pipeline

Use these specific tools during the translation and verification workflow:

### Tool 1: Headless Chrome Automation (HTML Ground Truth)
Capture exact ground-truth screenshots of the HTML file in both **Idle** and **Pressed/Active** states:
```powershell
$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
& $chrome --headless --disable-gpu --screenshot="html_idle.png" --window-size=500,500 "file:///path/to/component.html"
```

### Tool 2: Python (Pillow) Side-by-Side Visual Diff & Audit
Use a concise Python script to crop, align, and generate side-by-side comparison cards:
```python
from PIL import Image, ImageDraw

def create_audit_card(html_png, native_png, output_card):
    im_html = Image.open(html_png).resize((300, 300))
    im_native = Image.open(native_png).resize((300, 300))
    card = Image.new("RGBA", (660, 380), (16, 17, 19, 255))
    draw = ImageDraw.Draw(card)
    draw.text((36, 40), "HTML Ground Truth", fill=(0, 229, 255))
    draw.text((360, 40), "Native Jetpack Compose", fill=(0, 255, 128))
    card.paste(im_html, (30, 70), im_html)
    card.paste(im_native, (350, 70), im_native)
    card.save(output_card, "PNG")
```

### Tool 3: Automated Kotlin JVM Unit Testing
Validate hitboxes, cardinal deadzones, diagonal void rejections, and registry bindings using JVM unit tests:
```powershell
.\gradlew testDebugUnitTest
```

### Tool 4: Continuous Compilation & Quality Audit
Validate Kotlin compilation and verify zero suppressions:
```powershell
# 1. Compile & run unit tests
.\gradlew compileDebugKotlin testDebugUnitTest

# 2. Verify zero @Suppress or @SuppressLint annotations
powershell -Command "Get-ChildItem -Path 'app\src' -Recurse -Filter '*.kt' | Select-String -Pattern '@Suppress|@SuppressLint'"
```

---

## 8. The Mandatory 6-Step End-to-End Wiring Pipeline

> [!CAUTION]
> **Why components go missing in Button Studio**:  
> Registering a component in `NativeComponentRegistry.kt` connects the runtime rendering engine, but **Button Studio will NOT display it in the grid** unless it is also registered in `DefaultComponents.kt` (`ALL_PRESETS`).  
> Button Studio (`ButtonStudioScreen.kt`) and HUD Editor (`HudEditorViewModel.kt`) query `ComponentRegistry.installedComponents`, which loads presets directly from `DefaultComponents.ALL_PRESETS`.  
> **Every new component variant MUST complete all 6 steps below.**

```
Step 1: Interactive Composable (app/src/main/java/.../ui/components/controller/<Name>.kt)
  │
Step 2: Static Studio Preview (app/src/main/java/.../Static<Name> composable)
  │
Step 3: OOP Runtime Registry (app/src/main/java/.../runtime/registry/NativeComponentRegistry.kt)
  │
Step 4: Default Presets Catalog (app/src/main/java/.../runtime/registry/DefaultComponents.kt)
  │
Step 5: Unit Test Coverage (app/src/test/java/.../NativeComponentRegistryTest.kt)
  │
Step 6: Git Commit & Clean Status (git add ... ; git commit -m "...")
```

### Step 1: Create Interactive Composable
- **Location**: `app/src/main/java/com/sanket/tools/nexpad/ui/components/controller/<Name>.kt`
- Canonical parameters: `key: String`, `isConnected: Boolean`, `onVibrate: () -> Unit`, `viewModel: GamepadViewModel`, `isRgbEnabled: Boolean`, `modifier: Modifier = Modifier`, `displayLabel: String? = null`.
- Implement kinematics, 7-layer display list, and bespoke `.drawBehind` aura.

### Step 2: Create Static Studio Preview Composable
- **Location**: Alongside the interactive component or in `ui/studio/components/`.
- Must have **100% visual parity** with the idle state of the interactive component.
- Zero pointer input and zero mutable state to ensure 60 FPS smooth scrolling in Button Studio grid.

### Step 3: Register in Native Element Variant Registry
- **Location**: `app/src/main/java/com/sanket/tools/nexpad/runtime/registry/NativeComponentRegistry.kt`
- Declare `object <Name>Variant : BaseNativeVariant("builtin.<id>", ControlKey.<KEY>, "<Display Name>", <seedCode>)`.
- Implement `RenderInteractive` calling the interactive composable.
- Implement `RenderStaticPreview` calling the static preview composable.
- Register `register(<Name>Variant)` in `DefaultNativeFamily.init`.

### Step 4: Register in Button Studio Catalog & Presets (CRITICAL)
- **Location**: `app/src/main/java/com/sanket/tools/nexpad/runtime/registry/DefaultComponents.kt`
- Define `val DEFAULT_<NAME> = NxpComponentDef(...)` with manifest ID `"builtin.<id>"`, category, default control, and dimensions.
- Append `DEFAULT_<NAME>` to `val ALL_PRESETS = listOf(...)`.

### Step 5: Add Unit Test Coverage
- **Location**: `app/src/test/java/com/sanket/tools/nexpad/NativeComponentRegistryTest.kt`
- Verify variant resolution, seed code, and built-in registration via unit tests.

### Step 6: Verify Build & Clean Git Commit
- Run `./gradlew compileDebugKotlin testDebugUnitTest`.
- Verify zero `@Suppress` / `@SuppressLint`.
- Commit changes cleanly to git with a descriptive message.

---

## 9. Canonical Implementation Pattern Templates

Use these minimal, clean architectural templates when writing new components:

### Template A: Interactive Controller Button
```kotlin
package com.sanket.tools.nexpad.ui.components.controller

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sanket.tools.nexpad.viewmodel.GamepadViewModel

@Composable
fun CanonicalButton(
    key: String,
    isConnected: Boolean,
    onVibrate: () -> Unit,
    viewModel: GamepadViewModel,
    isRgbEnabled: Boolean,
    modifier: Modifier = Modifier,
    displayLabel: String? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "btn_scale"
    )
    val pressOffsetYAnim by animateFloatAsState(
        targetValue = if (isPressed) 2f else 0f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "btn_offset"
    )
    val rgbBloomAlpha by animateFloatAsState(
        targetValue = if (isPressed) 1.0f else 0.70f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 440f),
        label = "btn_bloom"
    )

    val currentOnVibrate by rememberUpdatedState(onVibrate)
    val currentViewModel by rememberUpdatedState(viewModel)
    val glowColor = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .offset { IntOffset(0, pressOffsetYAnim.dp.roundToPx()) }
            // Layer 0: Bespoke Kinetic Aura
            .drawBehind {
                if (isRgbEnabled) {
                    val auraR = size.minDimension * 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rgbBloomAlpha * (if (isPressed) 0.65f else 0.35f)),
                                glowColor.copy(alpha = rgbBloomAlpha * 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = auraR
                        ),
                        radius = auraR,
                        center = center
                    )
                }
            }
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                ambientColor = if (isRgbEnabled) glowColor else Color.Black,
                spotColor = if (isRgbEnabled) glowColor else Color.Black
            )
            .clip(CircleShape)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        currentOnVibrate()
                        isPressed = true
                        currentViewModel.updateButton(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        currentViewModel.updateButton(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Layers 1 through 6: Chassis, textures, fills, glyphs, specular crescent
        }
        Text(text = displayLabel ?: key, color = glowColor)
    }
}
```

### Template B: Static Studio Preview
```kotlin
@Composable
fun StaticCanonicalButton(
    modifier: Modifier = Modifier,
    isRgbEnabled: Boolean = true
) {
    val glowColor = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .size(80.dp)
            .drawBehind {
                if (isRgbEnabled) {
                    val auraR = size.minDimension * 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.35f), glowColor.copy(alpha = 0.15f), Color.Transparent),
                            center = center,
                            radius = auraR
                        ),
                        radius = auraR,
                        center = center
                    )
                }
            }
            .shadow(6.dp, CircleShape, ambientColor = glowColor, spotColor = glowColor)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Idle canvas drawing matching interactive idle state exactly
    }
}
```

### Template C: Registry & Preset Declarations
```kotlin
// In NativeComponentRegistry.kt:
object CanonicalVariant : BaseNativeVariant("builtin.<id>", ControlKey.<KEY>, "<Name>", <seedCode>) {
    override val isBaselineDefault: Boolean = false

    @Composable
    override fun RenderInteractive(context: NativeRenderContext) {
        CanonicalButton(
            key = controlKey.key,
            isConnected = context.isConnected,
            onVibrate = context.onVibrate,
            viewModel = context.viewModel,
            isRgbEnabled = context.isRgbEnabled,
            modifier = context.modifier
        )
    }

    @Composable
    override fun RenderStaticPreview(context: NativePreviewContext) {
        StaticCanonicalButton(modifier = context.modifier, isRgbEnabled = true)
    }
}

// In DefaultComponents.kt:
val DEFAULT_CANONICAL = NxpComponentDef(
    manifest = NxpManifest(
        id = "builtin.<id>",
        name = "<Name>",
        author = "NEXPAD Core",
        version = "1.0.0",
        category = NxprcCategory.<CATEGORY>.id,
        defaultControl = ControlKey.<KEY>.key,
        description = "<Short visual signature description>"
    ),
    geometry = NxpGeometry(type = "Circle"),
    visual = NxpVisual(fillColor = "#181818", opacity = 0.95f, borderColor = "#00F0FF", borderWidth = 2f, glowColor = "#00F0FF", glowRadius = 14f),
    pressed = NxpPressedState(scale = 0.95f, fillColor = "#00F0FF", borderColor = "#FFFFFF"),
    label = NxpLabel(text = "<KEY>", color = "#00F0FF", pressedColor = "#FFFFFF", fontSize = 16f),
    size = NxpSize(widthDp = 80, heightDp = 80)
)
```

---

## 10. Pre-Commit Quality Assurance Checklist

Before completing any controller task, verify every item:

- [ ] **1. Dedicated Component File**: Did you create a new file instead of overwriting existing components?
- [ ] **2. Full Mobile Sizing**: Are dimensions adjusted for smartphone controller reach?
- [ ] **3. Resolution Independence**: Are all graphics rendered via Canvas `DrawScope` without static raster images?
- [ ] **4. Bespoke Kinetic Aura**: Is `.drawBehind` implemented with radial falloff to `Color.Transparent` and gated by `isRgbEnabled`?
- [ ] **5. Stable Hitbox**: Are touch gestures isolated on the root container without offset distortion?
- [ ] **6. Static Preview Included**: Is there a non-interactive `Static<Name>` composable for Button Studio?
- [ ] **7. Dual Registration**: Is the component registered in BOTH `NativeComponentRegistry.kt` AND `DefaultComponents.kt` (`ALL_PRESETS`)?
- [ ] **8. Zero Suppressions**: Strictly **0 `@Suppress` and 0 `@SuppressLint`** across all modified and newly created files.
- [ ] **9. Clean Test Pass**: Did `./gradlew compileDebugKotlin testDebugUnitTest` pass with 100% success?
- [ ] **10. Git Status**: Is the git workspace clean and committed with a descriptive message?
