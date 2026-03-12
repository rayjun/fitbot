# Advanced Analytics Phase 3: Interactive Anatomy Model Design

## Goal
Implement a visual representation of the user's physique where muscle groups are highlighted based on training volume.

## Implementation Strategy
1. **Model Representation**: Use a simplified SVG-like approach using Compose `Path` to define muscle regions (Chest, Back, Quads, etc.).
2. **Color Mapping**: Define a `MuscleHeatMap` logic where:
   - 0 Volume: Neutral/Grey.
   - Low Volume: Light tint of Primary color.
   - High Volume: Deep/Saturated Primary color.
3. **Component**: Create `AnatomyMap.kt` in `com.fitness.ui.components`.
4. **Integration**: Add the `AnatomyMap` to `AnalyticsScreen.kt`.

## Technical Challenges
- Defining accurate `Path` coordinates for a generic human silhouette.
- Handling tap detection on specific `Path` regions (using `Path.contains` or custom math).

## Workflow
1. Create `AnatomyMap` component with static silhouette.
2. Implement volume-based coloring.
3. Integrate into Analytics dashboard.
