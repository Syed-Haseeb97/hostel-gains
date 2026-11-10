# Dispatch: Sub-Orchestrator Milestone M1 (Core Domain & Build Infra)

## Role & Scope
You are the Sub-Orchestrator for Milestone M1 (Core Domain & Build Infrastructure) of "The Hostel-Gains Dynamic Optimizer".
Your working directory is: `D:\projects\hostel_gains\.agents\teamwork\sub_orch_m1`
Parent conversation ID: `369d8cfd-670f-4689-a71e-4ac9454c73da`

## Requirements
Read:
- `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (authoritative source of truth)
- `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`
- Survey handoffs:
  - `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_1\handoff.md` (Build setup, Drive D: isolation settings)
  - `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md` (Domain logic, models, V-Taper framework, CompensatoryEngine)

## Scope of M1
1. **F1: Build Infrastructure on Drive D:**:
   - Create root `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`.
   - Setup Gradle wrapper (using Gradle 9.6.0 pre-cached in `D:\.gradle\wrapper\dists`).
   - Create `app/build.gradle.kts` with compileSdk 35, minSdk 26, Jetpack Compose plugin 2.4.10, OkHttp 5.3.0, JUnit 4.13.2.
   - Enforce strict Drive D: isolation:
     `GRADLE_USER_HOME=D:\.gradle`, `ANDROID_USER_HOME=D:\.android`, `sdk.dir=D:\\Android\\sdk`, `java.io.tmpdir=D:\\temp`.
     Zero bytes to Drive C:.
2. **F2: Core Domain Models**:
   - `MealNutrition`, `FoodItemNutrition`, `MacroNutrients`.
   - `Exercise`, `ExerciseCategory`, `MuscleGroup`, `WorkoutDayPlan`, `DayOfWeek`.
   - `StepStatus`, `StepAlertResult`.
3. **F3: V-Taper PPL Framework Database**:
   - Days 1 to 7 fully populated with exercises, baseline sets, rep ranges, rest intervals, and biomechanical form notes.
   - Day 1: Pull A (Wide-Grip Lat Pulldowns 4x8-12, Seated Cable Rows 3x8-12, Machine Rear Delt Flyes 4x10-15, DB Hammer Curls 3x8-12).
   - Day 2: Push A (Incline DB Press 4x8-12, DB Lateral Raises 5x10-15, Incline Push-ups 3xFailure, Tricep Rope Pushdowns 3x8-12).
   - Day 3: Legs & Core (Goblet Squats 4x8-12, BW Squats 2xFailure, Lying Leg Curls 3x8-12, Plank 3x60s).
   - Day 4: Pull B (Lat thickness horizontal pulls, biceps focus).
   - Day 5: Push B (Lateral delts cable focus, triceps focus).
   - Day 6: Weak-Point Pump (FST-7 Lat Pulldowns 7x14 45s rest, Cable Lateral Raises 5x12, Face Pulls 3x15).
   - Day 7: Active Rest (5,000 - 10,000 steps only).
4. **F4: Dynamic Compensatory Workout Engine**:
   - `CompensatoryEngine` implementation:
     - Carb Trigger: When meal carbs >= 60g, adds +1 set to Lat Pulldowns and Squats.
     - Protein Deficit Trigger: When meal protein < 20g, caps all exercises at max 3 sets.
     - Conflict Precedence: Protein Deficit Cap is an absolute physiological ceiling (`min(3, baseline + carbBonus)`).
5. **F5: Step Counter Sync & 6 PM Alert Engine**:
   - `StepAlertEvaluator` evaluating `steps < 5000` at or after `18:00`.
6. **M1 Automated Unit Tests**:
   - `CompensatoryEngineTest`, `StepCounterSyncTest`, `VTaperFrameworkStructureTest`.
   - Run tests headlessly strictly on Drive D: via `.\gradlew.bat testDebugUnitTest` and verify 100% pass.

Follow the sub-orchestrator iteration loop: Explorer -> Worker -> Reviewer -> Challenger -> Auditor -> Gate.
When gate passes, write your `handoff.md` and send a message back.
