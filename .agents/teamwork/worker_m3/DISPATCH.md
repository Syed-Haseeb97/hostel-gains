# Dispatch: Worker M3 (3D Kinematic Engine & Dynamic Exercise Rebalancer)

## Objective
Implement Milestone M3:
1. Port 28 exercise routines and `solve(p, mv)` forward kinematics from `D:\extracted_fitness_planner\src\components\moves.ts` into Kotlin (`app/src/main/java/com/hostelgains/ui/kinematics/`).
2. Implement Native Compose 3D Canvas Mesh Engine (`MannequinCanvas`, 4x4 matrix projection, depth sorting, touch orbit, speed controls) based on `Mannequin.tsx` and `ExerciseViewer.tsx`.
3. Copy/bundle offline 3D web assets from `D:\extracted_fitness_planner\` into `app/src/main/assets/3d_viewer/` for offline WebView fallback.
4. Implement `Hero3D` (interactive 3D Olympic barbell) and `Macro3DBars` (3D isometric macro breakdown).
5. Implement the Dynamic Exercise Rebalancer Subsystem:
   - `WorkoutRebalancer` interface and data models in `app/src/main/java/com/hostelgains/domain/rebalance/`
   - `OfflineWorkoutRebalancer`: deterministic synergistic redistribution matrix across all 6 PPL workout days (Back/Lats -> Rows/Towel Rows; Chest/Shoulders -> Push-ups; Legs -> Bodyweight Squats; Hamstrings/Core -> Bodyweight hinges/Plank)
   - `GeminiWorkoutRebalancer`: AI structured JSON volume redistribution client with fallback
   - Precedence harmonization with `CompensatoryEngine` (strict 3-set protein-deficit ceiling)
6. Write unit tests:
   - `app/src/test/java/com/hostelgains/ui/kinematics/KinematicsSolverTest.kt`
   - `app/src/test/java/com/hostelgains/domain/rebalance/WorkoutRebalancerTest.kt`

## Mandatory Inputs to Read First
- `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (READ FIRST)
- `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`
- Kinematics source files:
  - `D:\extracted_fitness_planner\src\components\moves.ts`
  - `D:\extracted_fitness_planner\src\components\Mannequin.tsx`
  - `D:\extracted_fitness_planner\src\components\Hero3D.tsx`
  - `D:\extracted_fitness_planner\src\components\Nutrition.tsx`

## Write Ownership
You exclusively own:
- `app/src/main/java/com/hostelgains/ui/kinematics/`
- `app/src/main/java/com/hostelgains/domain/rebalance/`
- `app/src/main/assets/3d_viewer/`
- `app/src/test/java/com/hostelgains/ui/kinematics/`
- `app/src/test/java/com/hostelgains/domain/rebalance/`

## Verification Command on Drive D:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
Set-Location "D:\projects\hostel_gains"
.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
```
Ensure all tests compile and pass cleanly on Drive D: without writing to Drive C:.
Write `handoff.md` upon completion.
