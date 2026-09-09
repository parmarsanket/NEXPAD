# NEXPAD Controller Layout Metrics & Ergonomics Specification

## 1. Architectural Overview

NEXPAD virtual gamepad layouts are defined using normalized coordinates:
$$(x_{ratio}, y_{ratio}) \in [0.0, 1.0] \times [0.0, 1.0]$$
where $(0, 0)$ is the top-left corner and $(1, 1)$ is the bottom-right corner of the active viewport.

In runtime rendering (`HudEditorScreen.kt` and `GamepadScreen.kt`), each control is placed using **center-anchored layout modifiers**:
```kotlin
val x = (position.xRatio * screenWidthPx - placeable.width / 2f).roundToInt()
val y = (position.yRatio * screenHeightPx - placeable.height / 2f).roundToInt()
```
This ensures that the $(x_{ratio}, y_{ratio})$ coordinates consistently define the **exact visual and physical center** of each control element.

---

## 2. Aspect Ratio Distortion & Mathematical Compensation

### The Problem
Modern mobile devices feature widescreen and ultrawide displays (16:9, 19.5:9, 20:9, 21:9).
On a reference 20:9 display ($2400 \times 1080\text{ px}$):
- $1\%\text{ of width } (\Delta x = 0.01) = 24.0\text{ physical pixels}$
- $1\%\text{ of height } (\Delta y = 0.01) = 10.8\text{ physical pixels}$
- Aspect ratio:
  $$\alpha = \frac{W_{screen}}{H_{screen}} = \frac{2400}{1080} \approx 2.2222$$

If button offsets are configured with $\Delta x = \Delta y$, the visual cluster is distorted by **222%** horizontally, turning square diamonds into wide flat ovals.

### The Mathematical Solution
To create a geometrically equilateral and isotropic shape (such as the 4-button ABXY diamond or an 8-way D-Pad), the ratio deltas MUST satisfy:
$$\Delta y = \alpha \cdot \Delta x$$
$$\Delta x = \frac{R_{dp}}{W_{dp}} = \frac{R_{px}}{W_{screen}}$$
$$\Delta y = \frac{R_{dp}}{H_{dp}} = \frac{R_{px}}{H_{screen}}$$
where $R_{dp}$ is the desired physical radius in density-independent pixels.

---

## 3. Component Base Dimension Matrix

Every controller element in NEXPAD has a calibrated base composable size:

| Control Identifier | Base Size (DP) | Standard Scale | Visual Diameter / Size (DP) | Visual Size on 2.75x ($2400 \times 1080$) |
| :--- | :--- | :--- | :--- | :--- |
| **Face Buttons (`A`, `B`, `X`, `Y`)** | $80 \times 80\text{ dp}$ | $0.82\text{f}$ | $65.6 \times 65.6\text{ dp}$ | $180.4 \times 180.4\text{ px}$ |
| **Left / Right Sticks (`LS`, `RS`)** | $150 \times 150\text{ dp}$ | $1.05\text{f}$ | $157.5 \times 157.5\text{ dp}$ | $433.1 \times 433.1\text{ px}$ |
| **D-Pad (`DPAD`)** | $140 \times 140\text{ dp}$ | $1.10\text{f}$ | $154.0 \times 154.0\text{ dp}$ | $423.5 \times 423.5\text{ px}$ |
| **Triggers (`LT`, `RT`)** | $100 \times 160\text{ dp}$ | $1.18\text{f}$ | $118.0 \times 188.8\text{ dp}$ | $324.5 \times 519.2\text{ px}$ |
| **Bumpers (`LB`, `RB`)** | $160 \times 60\text{ dp}$ | $0.95\text{f}$ | $152.0 \times 57.0\text{ dp}$ | $418.0 \times 156.8\text{ px}$ |
| **System Triad (`VIEW`, `MENU`, `SHARE`)** | $60 \times 60\text{ dp}$ | $0.70\text{f}$ | $42.0 \times 42.0\text{ dp}$ | $115.5 \times 115.5\text{ px}$ |
| **Xbox Guide Button (`XBOX`)** | $60 \times 60\text{ dp}$ | $1.15\text{f}$ | $69.0 \times 69.0\text{ dp}$ | $189.8 \times 189.8\text{ px}$ |
| **Macro Row (`M1`, `M2`, `M3`, `M4`)** | $80 \times 40\text{ dp}$ | $0.75\text{f}$ | $60.0 \times 30.0\text{ dp}$ | $165.0 \times 82.5\text{ px}$ |

---

## 4. Standard Elite Geometry Derivations

### 4.1. ABXY Face Button Diamond
- **Cluster Center**: $(C_x = 0.675\text{f}, C_y = 0.720\text{f})$
- **Cluster Radius $R_{dp}$**: $53.0\text{ dp}$ ($145.75\text{ px}$)
- **Offset deltas**:
  $$\Delta x = \frac{53.0}{872.73} = 0.06073\text{f}$$
  $$\Delta y = \frac{53.0}{392.73} = 0.13495\text{f}$$
- **Generated Coordinates**:
  - `Y` (North): $(0.675\text{f}, 0.585\text{f})$
  - `X` (West): $(0.614\text{f}, 0.720\text{f})$
  - `B` (East): $(0.736\text{f}, 0.720\text{f})$
  - `A` (South): $(0.675\text{f}, 0.855\text{f})$
- **Clearance Verification**:
  - Adjacent button center-to-center distance: $R_{px} \cdot \sqrt{2} = 145.75 \times 1.4142 = 206.12\text{ px}$ ($75.0\text{ dp}$)
  - Button visual diameter: $180.4\text{ px}$ ($65.6\text{ dp}$)
  - **Edge-to-edge adjacent gap**: $206.12 - 180.40 = \mathbf{25.72\text{ px}} \approx \mathbf{9.35\text{ dp}}$
  - Opposite button center-to-center distance: $2 \cdot R_{px} = 291.50\text{ px}$ ($106.0\text{ dp}$)
  - **Opposite center gap**: $291.50 - 180.40 = \mathbf{111.10\text{ px}} \approx \mathbf{40.4\text{ dp}}$

### 4.2. Clearance to Surrounding Elements
- **Button B to Right Stick (`RS`)**:
  - Right edge of B: $(0.7357 \times 2400) + (180.4 / 2) = 1765.76 + 90.2 = 1855.96\text{ px}$
  - Left edge of RS: $(0.895 \times 2400) - (433.1 / 2) = 2148.0 - 216.55 = 1931.45\text{ px}$
  - **Clearance gap**: $1931.45 - 1855.96 = \mathbf{75.49\text{ px}} \approx \mathbf{27.45\text{ dp}}$ (Zero overlap, safe thumb clearance)
- **Left Stick (`LS`) to D-Pad (`DPAD`)**:
  - Right edge of LS: $(0.115 \times 2400) + (433.1 / 2) = 276.0 + 216.55 = 492.55\text{ px}$
  - Left edge of DPAD: $(0.320 \times 2400) - (423.5 / 2) = 768.0 - 211.75 = 556.25\text{ px}$
  - **Clearance gap**: $556.25 - 492.55 = \mathbf{63.70\text{ px}} \approx \mathbf{23.16\text{ dp}}$
- **D-Pad (`DPAD`) to Button X**:
  - Right edge of DPAD: $768.0 + 211.75 = 979.75\text{ px}$
  - Left edge of X: $(0.6143 \times 2400) - (180.4 / 2) = 1474.24 - 90.2 = 1384.04\text{ px}$
  - **Clearance gap**: $1384.04 - 979.75 = \mathbf{404.29\text{ px}} \approx \mathbf{147.02\text{ dp}}$
- **Center Macro Row (`M2`, `M4`, `M3`, `M1`)**:
  - Base width: $80\text{ dp} \times 0.75 \times 2.75 = 165.0\text{ px}$
  - Center spacing: $\Delta x = 0.080\text{f} \implies 0.080 \times 2400 = 192.0\text{ px}$
  - **Gap between adjacent macro buttons**: $192.0 - 165.0 = \mathbf{27.0\text{ px}} \approx \mathbf{9.82\text{ dp}}$

---

## 5. Algorithmic API (`LayoutMetrics.kt`)

Developers and AI agents creating new profiles should use the programmatic utilities in `LayoutMetrics`:

```kotlin
// Create a mathematically isotropic diamond cluster
val abxyCluster = LayoutMetrics.createDiamondCluster(
    centerX = 0.675f,
    centerY = 0.720f,
    radiusDp = 53.0f,
    scale = 0.82f
)

// Create an evenly spaced linear row
val macroRow = LayoutMetrics.createHorizontalRow(
    keys = listOf("M2", "M4", "M3", "M1"),
    centerY = 0.350f,
    centerX = 0.500f,
    spacingDp = 70.0f,
    scale = 0.75f
)

// Calculate distance and circular clearance between any two controls
val distanceDp = LayoutMetrics.distanceDp(pos1, pos2)
val clearanceDp = LayoutMetrics.circularClearanceDp("B", posB, "RS", posRS)
```

---

## 6. Layout Guidelines for AI & Contributors

1. **Center Anchoring**: Always think in center anchor coordinates $(x_{center}, y_{center})$.
2. **Aspect Ratio Awareness**: Always use `LayoutMetrics.dpToRatioDelta(distanceDp)` when placing elements relative to one another to preserve true geometric isotropy across varying screen aspect ratios.
3. **Minimum Clearance Constraints**:
   - Adjacent circular buttons: Minimum $8\text{ dp}$ clearance.
   - Thumbsticks to adjacent clusters: Minimum $20\text{ dp}$ clearance.
   - Triggers and Bumpers: Stack vertically with top-alignment for triggers and mid-height placement for bumpers.
4. **Non-Deletable Defaults**: Default profiles 1–5 MUST remain protected with `isDefault = true`.
