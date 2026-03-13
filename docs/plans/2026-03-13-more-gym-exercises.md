# Exercise Library Expansion Plan (Gym Isolation & Machines)

**Goal**: Expand the exercise library with 12 new classic gym exercises covering all muscle groups, introducing machine and cable work.

## 1. Data Layer (`ExerciseProvider.kt`)
Add 12 new `Exercise` objects:
- `dumbbell_fly`, `cable_crossover`
- `lat_pulldown`, `face_pull`
- `front_raise`
- `hammer_curl`, `triceps_pushdown`
- `leg_press`, `leg_extension`, `leg_curl`, `romanian_deadlift`
- `hanging_leg_raise`

## 2. Localization (`ResourceUtils.kt`)
Add EN and ZH strings for:
- 12 new exercise names and descriptions.
- 3 new specific target muscles: `muscle_rear_delts` (Rear Deltoids), `muscle_front_delts` (Front Deltoids), `muscle_hamstrings` (Hamstrings).

## 3. Asset Generation (`generate_gifs.py`)
Update the Python script to render 12 new procedural stickman animations matching the existing #FF5722 / #969696 style. 
- Build specialized kinematics for machines like Leg Press, Lat Pulldown, and Cables.
- Copy generated GIFs to both Android `assets` and KMP `composeResources`.

## 4. Verification
Compile iOS and Android endpoints to ensure the new static assets and strings resolve correctly without crashing.