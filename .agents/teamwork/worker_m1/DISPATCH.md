# Dispatch: Worker M1 (Core Domain & Build Infrastructure)

## Objective
Implement Milestone M1 (Core Domain & Build Infrastructure) for "The Hostel-Gains Dynamic Optimizer" strictly on Drive D: leaving Drive C: completely untouched.

## Mandatory Inputs (Read Before Coding)
1. `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (authoritative source of truth)
2. `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md` (architecture, feature inventory, contracts, code layout)
3. `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_1\handoff.md` (exact Gradle 9.6.0, AGP 9.4.0, Kotlin 2.4.10, SDK 35 settings, and Drive D: zero-touch configuration)
4. `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md` (exact domain models, V-Taper PPL 7-day schedule, CompensatoryEngine math, and test specifications)

## Scope of M1
1. **Root Build Configuration (`F1`)**:
   - `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `local.properties`.
   - Setup Gradle wrapper using Gradle 9.6.0 (pre-cached in `D:\.gradle\wrapper\dists\gradle-9.6.0-bin\42k10rwplmzkhuboz9kdazi7s\gradle-9.6.0\`).
   - `gradle.properties`: `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8 -Djava.io.tmpdir=D:\\temp`
   - `local.properties`: `sdk.dir=D:\\Android\\sdk`
   - Ensure `D:\temp` and `D:\.android` exist (`New-Item -ItemType Directory -Path "D:\temp", "D:\.android" -Force`).
2. **App Module Configuration (`F1`)**:
   - `app/build.gradle.kts`: AGP 9.4.0, Kotlin Compose plugin 2.4.10, compileSdk 35, minSdk 26, targetSdk 35.
   - Compose BOM 2026.09.00, OkHttp 5.3.0, JUnit 4.13.2.
   - `app/src/main/AndroidManifest.xml`: standard package `com.hostelgains.app` with `android:theme="@android:style/Theme.Material.NoActionBar"`.
3. **Core Domain Models (`F2`)**:
   - `MealNutrition`, `FoodItemNutrition`, `MacroNutrients`.
   - `Exercise`, `ExerciseCategory`, `MuscleGroup`, `WorkoutDayPlan`, `DayOfWeek`.
   - `StepStatus`, `StepAlertResult`.
4. **V-Taper PPL Framework Database (`F3`)**:
   - `VTaperWorkoutRepository`: Days 1 through 7 with full exercise models, baseline sets, rep ranges, rest timers, and form cues per R3:
     - Day 1: Pull A (Wide-Grip Lat Pulldowns 4x8-12, Seated Cable Rows 3x8-12, Machine Rear Delt Flyes 4x10-15, DB Hammer Curls 3x8-12).
     - Day 2: Push A (Incline DB Press 4x8-12, DB Lateral Raises 5x10-15, Incline Push-ups 3xFailure, Tricep Rope Pushdowns 3x8-12).
     - Day 3: Legs & Core (Goblet Squats 4x8-12, BW Squats 2xFailure, Lying Leg Curls 3x8-12, Plank 3x60s).
     - Day 4: Pull B (Lat thickness horizontal pulls, biceps focus).
     - Day 5: Push B (Lateral delts cable focus, triceps focus).
     - Day 6: Weak-Point Pump (FST-7 Lat Pulldowns 7x14 45s rest, Cable Lateral Raises 5x12, Face Pulls 3x15).
     - Day 7: Active Rest (5,000 - 10,000 steps only).
5. **Dynamic Compensatory Workout Engine (`F4`)**:
   - `CompensatoryEngine` implementation:
     - Carb Trigger: When meal carbs >= 60g, dynamically adds +1 set to Lat Pulldowns and Squats.
     - Protein Deficit Trigger: When meal protein < 20g (e.g. vegetarian dal and rice meals), automatically caps all exercises at max 3 sets.
     - Safety Precedence: Protein Deficit Cap acts as an absolute physiological ceiling (`min(3, baselineSets + carbAdjustment)`).
     - `AdjustmentAudit` notes explaining why sets were modified.
6. **Step Counter Sync & 6 PM Alert Engine (`F5`)**:
   - `StepAlertEvaluator` evaluating `steps < 5000` at or after `18:00`, prompting 10,000 steps after dinner.
7. **Automated Unit Tests**:
   - `CompensatoryEngineTest`
   - `StepCounterSyncTest`
   - `VTaperFrameworkStructureTest`
   - Run tests:
     ```powershell
     $env:GRADLE_USER_HOME = "D:\.gradle"
     $env:ANDROID_USER_HOME = "D:\.android"
     $env:ANDROID_HOME = "D:\Android\sdk"
     $env:ANDROID_SDK_ROOT = "D:\Android\sdk"
     Set-Location "D:\projects\hostel_gains"
     .\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
     ```
   - Verify 100% pass rate and zero writes to Drive C:.

## Write Ownership
You own:
- `D:\projects\hostel_gains\settings.gradle.kts`
- `D:\projects\hostel_gains\build.gradle.kts`
- `D:\projects\hostel_gains\gradle.properties`
- `D:\projects\hostel_gains\local.properties`
- `D:\projects\hostel_gains\gradle\`
- `D:\projects\hostel_gains\gradlew.bat`
- `D:\projects\hostel_gains\app\build.gradle.kts`
- `D:\projects\hostel_gains\app\src\main\AndroidManifest.xml`
- `D:\projects\hostel_gains\app\src\main\java\com\hostelgains\domain\`
- `D:\projects\hostel_gains\app\src\test\java\com\hostelgains\domain\`
Do NOT touch other agent working directories.

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Deliverable
Write your completion report to `D:\projects\hostel_gains\.agents\teamwork\worker_m1\handoff.md`.
Include test commands and output. Send a message to orchestrator when complete.


## 2026-11-10T02:00:47Z
[Message] timestamp=2026-11-10T02:00:47Z sender=369d8cfd-670f-4689-a71e-4ac9454c73da priority=MESSAGE_PRIORITY_HIGH content=You are Worker M1 for "The Hostel-Gains Dynamic Optimizer".
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\worker_m1

Read:
- D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md (authoritative source of truth)
- D:\projects\hostel_gains\.agents\teamwork\PROJECT.md
- D:\projects\hostel_gains\.agents\teamwork\worker_m1\DISPATCH.md
- D:\projects\hostel_gains\.agents\teamwork\explorer_survey_1\handoff.md
- D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md

Your task is to implement Milestone M1 (Core Domain & Build Infrastructure):
1. Create root Gradle build files (settings.gradle.kts, build.gradle.kts, gradle.properties, local.properties) and Gradle wrapper.
2. Configure app module (app/build.gradle.kts, AndroidManifest.xml) with AGP 9.4.0, Kotlin Compose plugin 2.4.10, compileSdk 35, OkHttp 5.3.0, JUnit 4.13.2.
3. CRITICAL CONSTRAINT: Strictly use Drive D: with zero writes to Drive C:. Pass --gradle-user-home D:\.gradle and set GRADLE_USER_HOME=D:\.gradle, ANDROID_USER_HOME=D:\.android, sdk.dir=D:\\Android\\sdk. Ensure D:\temp and D:\.android exist.
4. Implement Core Domain Models, V-Taper PPL 7-day Framework database (VTaperWorkoutRepository), Dynamic Compensatory Workout Engine (CompensatoryEngine), and Step Alert Evaluator (StepAlertEvaluator).
5. Implement unit tests (CompensatoryEngineTest, StepCounterSyncTest, VTaperFrameworkStructureTest).
6. Run unit tests via `.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest` and verify 100% pass rate.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Document your work and test output in D:\projects\hostel_gains\.agents\teamwork\worker_m1\handoff.md and send a completion message back.
