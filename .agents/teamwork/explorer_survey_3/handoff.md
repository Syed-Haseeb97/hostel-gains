# Handoff Report: Domain Logic, AI Nutrition Parsing & Dynamic Workout Engine

**Author**: Explorer Survey 3 (Domain Logic & Engine Explorer)  
**Date**: 2026-11-10T01:55:00Z  
**Target Path**: `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md`  

---

## 1. Observation

Direct observations from the project specification, dispatch requirements, and domain constraints:

1. **System & Working Environment**:
   - Project directory: `D:\projects\hostel_gains` (`ORIGINAL_REQUEST.md:7`).
   - Disk constraint: "All project workspaces, code repositories, temporary files, package installations, virtual environments, builds, and artifacts must be placed on Drive D: (specifically under `D:\projects\` or `D:\`). Drive C: is strictly off-limits and untouched" (`ORIGINAL_REQUEST.md:36`, user global rules).
   - Integrity mode: `development` (`ORIGINAL_REQUEST.md:8`).

2. **Mess Menu Input & AI Macronutrient Calculation (R2 & Acceptance Criteria)**:
   - Natural language mess meal inputs: e.g., `"mutton, 4 dry rotis, eggs"`, `"dal and rice"`, `"paneer bhurji and 3 parathas"`, `"chole bhature"` (`ORIGINAL_REQUEST.md:17, 41`).
   - Must calculate protein, carbohydrates, fats, and total calories per meal and daily totals (`ORIGINAL_REQUEST.md:18, 42`).
   - "Automated unit tests verify that natural language meal descriptions... are parsed into protein, carbohydrates, fats, and calories via AI API integration" (`ORIGINAL_REQUEST.md:41`).
   - "Define deterministic offline mocking/fixtures so automated unit tests verify parsing without requiring live API keys or network during test runs" (`DISPATCH.md:11`).

3. **V-Taper PPL Framework Specification (R3 & Acceptance Criteria)**:
   - 6 training days + Day 7 active rest (`ORIGINAL_REQUEST.md:21-29`):
     - **Day 1 (Pull A)**: Wide-Grip Lat Pulldowns (4 sets, 8-12 reps), Seated Cable Rows (3 sets, 8-12 reps), Machine Rear Delt Flyes (4 sets, 10-15 reps), Dumbbell Hammer Curls (3 sets, 8-12 reps).
     - **Day 2 (Push A)**: Incline Dumbbell Press (4 sets, 8-12 reps), Dumbbell Lateral Raises (5 sets, 10-15 reps), Incline Push-ups (3 sets to failure), Tricep Rope Pushdowns (3 sets, 8-12 reps).
     - **Day 3 (Legs & Core)**: Goblet Squats (4 sets, 8-12 reps), Bodyweight Squats (2 sets to failure), Lying Leg Curls (3 sets, 8-12 reps), Plank (3 sets, 60 sec).
     - **Day 4 (Pull B)**: Lat thickness (horizontal pulls) and biceps focus.
     - **Day 5 (Push B)**: Lateral delts (cable focus) and triceps focus.
     - **Day 6 (Weak-Point Pump)**: Lat Pulldowns (7 sets of 14 reps, 45s rest), Cable Lateral Raises (5 sets of 12 reps), Face Pulls (3 sets of 15 reps).
     - **Day 7 (Active Rest)**: 5,000 - 10,000 steps only.

4. **Dynamic Compensatory Workout Engine (R3 & Acceptance Criteria)**:
   - **Carb Trigger**: "When a high-carb meal is logged, dynamically add 1 extra set to Lat Pulldowns and Squats in the evening session" (`ORIGINAL_REQUEST.md:31, 45`).
   - **Protein Deficit Trigger**: "When low protein intake is logged (e.g., vegetarian dal and rice meals), automatically cap all exercises at a maximum of 3 sets" (`ORIGINAL_REQUEST.md:32, 46`).
   - **Step Counter Sync**: "Monitor daily steps and trigger a UI alert if the user is under 5,000 steps by 6 PM, prompting 10,000 steps after dinner to stay on track for body recomposition" (`ORIGINAL_REQUEST.md:33, 47`).

---

## 2. Logic Chain

From the observations above, the domain logic and engine architecture are derived step-by-step:

### 2.1 AI Nutrition Parsing Architecture & Offline Fixture Strategy
- **Step 1: Indian Hostel Mess Nutrition Realities**:
  Hostel mess meals differ systematically from standard western nutrition databases:
  - Higher cooking oil/fat usage in gravies (dalda, refined vegetable oil, mustard oil).
  - High carbohydrate density (white rice, wheat rotis, potatoes).
  - Common vegetarian meals (e.g., "dal and rice") provide inadequate protein density (~8-12g protein per serving with 60-75g carbs), creating a protein deficit.
  - Non-vegetarian meals (e.g., "mutton, 4 dry rotis, eggs") provide high protein (~45-55g) along with high carbs from the rotis (~68-75g).
- **Step 2: AI Parsing Contract & Structured JSON Schema**:
  To guarantee deterministic parsing from Gemini, we define a strict schema using `application/json` output mode.
  ```kotlin
  data class FoodItemNutrition(
      val name: String,
      val quantityDescription: String,
      val proteinGrams: Float,
      val carbsGrams: Float,
      val fatsGrams: Float,
      val calories: Int
  )

  data class MealNutrition(
      val rawQuery: String,
      val mealName: String,
      val proteinGrams: Float,
      val carbsGrams: Float,
      val fatsGrams: Float,
      val calories: Int,
      val items: List<FoodItemNutrition>,
      val isHighCarb: Boolean,
      val isLowProtein: Boolean,
      val dietaryNotes: String
  )
  ```
- **Step 3: Prompt Engineering**:
  The system prompt enforces strict calculation rules:
  ```text
  You are an expert sports nutritionist specialized in Indian university hostel mess diets.
  Analyze the student's logged meal and estimate exact macronutrients:
  - 1 standard dry roti/chapati: ~17g carbs, ~3g protein, ~0.5g fat, ~80 kcal.
  - 1 katori/bowl hostel dal (150g): ~15g carbs, ~5g protein, ~4g fat, ~115 kcal.
  - 1 plate hostel white rice (150g cooked): ~45g carbs, ~3.5g protein, ~0.5g fat, ~200 kcal.
  - 1 whole boiled egg: ~0.5g carbs, ~6g protein, ~5g fat, ~75 kcal.
  - 1 serving hostel mutton curry (150g with gravy): ~6g carbs, ~24g protein, ~18g fat, ~280 kcal.
  - 1 serving paneer bhurji: ~8g carbs, ~18g protein, ~22g fat, ~300 kcal.
  Thresholds:
  - isHighCarb = true if total carbohydrates >= 60g.
  - isLowProtein = true if total protein < 20g (or if protein provides < 15% of meal calories in a main meal).
  Output strictly valid JSON matching the schema.
  ```
- **Step 4: Offline Deterministic Fixture Parser (Zero Network / Zero API Key Dependency)**:
  To allow automated unit tests to run 100% reliably in CI/offline on Drive D:, we create an abstraction:
  ```kotlin
  interface NutritionParser {
      suspend fun parseMeal(description: String): Result<MealNutrition>
  }
  ```
  - `GeminiNutritionParser`: Invokes Gemini REST API (`POST https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey`).
  - `FixtureNutritionParser`: Implements exact lookup matching and heuristic token parsing:
    - Pre-calibrated fixtures for `"mutton, 4 dry rotis, eggs"`, `"dal and rice"`, `"paneer bhurji and 3 parathas"`, `"4 boiled egg whites and 2 rotis"`.
    - Token-based fallback engine parsing arbitrary Indian meal text offline by summing recognized items.
    - Deterministic, zero-network, sub-millisecond execution for all automated unit tests.

---

### 2.2 V-Taper PPL Framework Full 7-Day Specification
The V-Taper aesthetic prioritizes shoulder width (lateral deltoid), upper back / lat width, and a narrow waist, supported by solid leg development.

| Day | Name | Focus | Exercises | Baseline Sets & Reps | Form Notes |
|---|---|---|---|---|---|
| **Day 1** | **Pull A** | Lat Width & Rear Delts | 1. Wide-Grip Lat Pulldowns<br>2. Seated Cable Rows<br>3. Machine Rear Delt Flyes<br>4. Dumbbell Hammer Curls | 4 sets x 8-12 reps<br>3 sets x 8-12 reps<br>4 sets x 10-15 reps<br>3 sets x 8-12 reps | Pull to upper chest, drive elbows down, 2s eccentric.<br>Chest upright, pull to belly button, squeeze scaps.<br>Lead with elbows, keep pinkies rotated slightly out.<br>Neutral grip, pin elbows, control descent. |
| **Day 2** | **Push A** | Upper Chest & Lateral Delts | 1. Incline Dumbbell Press<br>2. Dumbbell Lateral Raises<br>3. Incline Push-ups<br>4. Tricep Rope Pushdowns | 4 sets x 8-12 reps<br>5 sets x 10-15 reps<br>3 sets to failure<br>3 sets x 8-12 reps | 30-degree incline, full pec stretch, tuck elbows 45°.<br>Slight torso lean, raise in scapular plane, lead with elbows.<br>Hands on bench, brace core, explosive push.<br>Flare rope at bottom, squeeze horseshoe tricep. |
| **Day 3** | **Legs & Core** | Quad Sweep & Core Stability | 1. Goblet Squats<br>2. Bodyweight Squats<br>3. Lying Leg Curls<br>4. Plank | 4 sets x 8-12 reps<br>2 sets to failure<br>3 sets x 8-12 reps<br>3 sets x 60 sec | Dumbbell at chest, hit parallel, drive through midfoot.<br>Continuous cadence, full depth, quad burnout.<br>Dorsiflex toes, 3-second eccentric lowering.<br>Hollow-body position, squeeze glutes and transverse abdominis. |
| **Day 4** | **Pull B** | Lat Thickness & Biceps Focus | 1. Chest-Supported DB Rows<br>2. Neutral-Grip Lat Pulldowns<br>3. Incline Dumbbell Curls<br>4. Preacher Curls<br>5. Cable Face Pulls | 4 sets x 8-12 reps<br>3 sets x 8-12 reps<br>3 sets x 10-12 reps<br>3 sets x 10-12 reps<br>3 sets x 12-15 reps | Squeeze rhomboids and mid-traps, full stretch at bottom.<br>Neutral grip engages lower lats, drive elbows down.<br>60-degree bench, full bicep stretch, supinate at top.<br>Elbows fixed on pad, strict isolation, no swinging.<br>Rope to bridge of nose, external shoulder rotation. |
| **Day 5** | **Push B** | Cable Delts & Triceps Focus | 1. Standing Overhead DB Press<br>2. Cable Lateral Raises<br>3. Flat Dumbbell Press<br>4. Overhead Cable Tricep Extension<br>5. Bodyweight Dips | 4 sets x 8-12 reps<br>5 sets x 12-15 reps<br>3 sets x 8-12 reps<br>3 sets x 10-12 reps<br>3 sets to failure | Vertical press, brace core, do not hyperextend spine.<br>Cable behind back, constant tension across deltoid.<br>Controlled descent, squeeze pecs together at top.<br>Keep elbows high and stationary, stretch tricep long head.<br>Upright torso to emphasize triceps over chest. |
| **Day 6** | **Weak-Point Pump** | V-Taper Specialization & Metabolic Stress | 1. Lat Pulldowns (FST-7 Protocol)<br>2. Cable Lateral Raises<br>3. Cable Face Pulls | **7 sets x 14 reps** (45s rest)<br>**5 sets x 12 reps** (45s rest)<br>**3 sets x 15 reps** (60s rest) | Fascia stretch protocol. Strict weight, intense pump.<br>Continuous tension on side delts, maximum width focus.<br>High rep posture and rotator cuff endurance. |
| **Day 7** | **Active Rest** | Recovery & Step Optimization | Active Recovery Walking & Mobility | **5,000 - 10,000 steps only** | Zero resistance training. Focus on walking, hydration, foam rolling, and joint decompression. |

---

### 2.3 Dynamic Compensatory Engine: Rules & Conflict Resolution
The engine dynamically recalculates the active workout plan based on nutrition and step metrics:

1. **Carb Trigger Evaluation**:
   - **Condition**: A logged meal satisfies `meal.isHighCarb || meal.carbsGrams >= 60.0f`, or cumulative daily carbohydrates exceed 200g.
   - **Target Exercises**: Lat Pulldowns and Squats (identified by `exercise.isCompensatoryEligibleCarb == true` or name matching `Lat Pulldown` and `Squat`).
   - **Transformation**: `adjustedSets = baselineSets + 1`.
   - **Physiological Justification**: Glycogen surplus provides immediate substrate for high-volume glycolytic output in the primary V-Taper hypertrophy drivers (lats and quads).

2. **Protein Deficit Trigger Evaluation**:
   - **Condition**: A logged meal satisfies `meal.isLowProtein || meal.proteinGrams < 20.0f` (e.g., typical vegetarian hostel dal-rice meals), or cumulative daily protein is critically low (< 50g by evening).
   - **Target Exercises**: **All exercises** in the session.
   - **Transformation**: `adjustedSets = min(currentSets, 3)`.
   - **Physiological Justification**: Without sufficient amino acid availability (specifically leucine threshold to trigger mTOR and muscle protein synthesis), high-volume resistance training causes net muscle catabolism and blunted recovery. Capping volume at 3 sets preserves motor unit recruitment without causing unrecoverable myofibrillar damage.

3. **Conflict Resolution & Precedence (High-Carb + Low-Protein Meals)**:
   - Example: Meal `"dal and rice"` contains ~60g carbs (Trigger: Carb Surplus) and ~10g protein (Trigger: Protein Deficit).
   - **Engine Precedence Rule**: **The Protein Deficit Cap acts as an absolute physiological ceiling**.
   - **Formula**:
     $$\text{effectiveSets} = \begin{cases} 
     \min(3, \text{baselineSets} + 1) & \text{if CarbTrigger active and exercise is Lat Pulldown or Squat} \\
     \min(3, \text{baselineSets}) & \text{if CarbTrigger not active on exercise}
     \end{cases}$$
   - **Concrete Example**:
     - Wide-Grip Lat Pulldowns (baseline: 4 sets):
       - Carb Trigger adds 1 set $\rightarrow 5$ sets.
       - Protein Deficit Cap restricts to $\max 3$ sets $\rightarrow \min(3, 5) = \mathbf{3\text{ sets}}$.
     - Bodyweight Squats (baseline: 2 sets):
       - Carb Trigger adds 1 set $\rightarrow 3$ sets.
       - Protein Deficit Cap allows up to 3 sets $\rightarrow \min(3, 3) = \mathbf{3\text{ sets}}$.
   - **Audit Trail**: Every modified exercise includes an `AdjustmentAudit` object describing why the change occurred (e.g., `["+1 set (Carb Surplus)", "Capped at 3 sets (Protein Deficit)"]`), which is displayed in the UI.

4. **Step Counter Sync & Evening Alert**:
   - **Condition**: `currentSteps < 5000` AND `currentTime >= 18:00` (6:00 PM).
   - **Engine Output**:
     - `isAlertTriggered = true`
     - `alertSeverity = WARNING`
     - `title = "Step Deficit Alert (Sub-5,000 at 6:00 PM)"`
     - `recommendation = "You have logged only $steps steps today. To stay on track for body recomposition and insulin sensitivity, take a 10,000-step walk after your hostel dinner."`
   - **Architecture**: Employs an injectable `ClockProvider` interface to ensure 100% deterministic unit testing of boundary conditions (e.g., 17:59 vs 18:00, 4,999 steps vs 5,000 steps).

---

### 2.4 StateFlow, Repository & Room DB Architecture

```
+---------------------------------------------------------------------------------------+
|                                    PRESENTATION LAYER                                 |
|                                                                                       |
|  MealLogViewModel                  WorkoutViewModel               StepTrackerViewModel|
|  - StateFlow<MealLogUiState>       - StateFlow<WorkoutUiState>    - StateFlow<StepUi> |
+---------------------------------------------------------------------------------------+
                                           |
                                           v
+---------------------------------------------------------------------------------------+
|                                      DOMAIN LAYER                                     |
|                                                                                       |
|  ParseMealNutritionUseCase       CompensatoryEngine           EvaluateStepAlertUseCase|
|  - Validates & coordinates       - CarbTrigger (+1 set)       - 5k steps / 6pm check  |
|  - Calculates macro totals       - ProteinDeficitCap (<=3)    - Active walking prompt |
|                                                                                       |
|  Domain Models: Meal, MacroNutrients, WorkoutDayPlan, Exercise, StepStatus            |
+---------------------------------------------------------------------------------------+
                                           |
                                           v
+---------------------------------------------------------------------------------------+
|                                      DATA LAYER                                       |
|                                                                                       |
|  MealRepository                   WorkoutRepository            StepRepository         |
|         |                                 |                           |               |
|         +---------------------------------+---------------------------+               |
|         |                                                             |               |
|         v                                                             v               |
|  Room DB (HostelGainsDatabase)                                 NutritionParser        |
|  - MealDao (MealEntity)                                        - GeminiNutritionParser|
|  - DailySummaryDao (DailyMacroSummaryEntity)                   - FixtureNutritionParser|
|  - StepLogDao (StepLogEntity)                                                         |
+---------------------------------------------------------------------------------------+
```

#### Room Entities:
1. `MealEntity`:
   - `id: Long` (Primary Key, auto-generate)
   - `rawText: String`
   - `mealName: String`
   - `proteinGrams: Float`
   - `carbsGrams: Float`
   - `fatsGrams: Float`
   - `calories: Int`
   - `isHighCarb: Boolean`
   - `isLowProtein: Boolean`
   - `timestampMillis: Long`
   - `itemsJson: String`

2. `DailyMacroSummaryEntity`:
   - `dateIso: String` (Primary Key, e.g. "2026-11-10")
   - `totalProtein: Float`
   - `totalCarbs: Float`
   - `totalFats: Float`
   - `totalCalories: Int`
   - `hasHighCarbTrigger: Boolean`
   - `hasProteinDeficitTrigger: Boolean`
   - `mealCount: Int`

3. `StepLogEntity`:
   - `dateIso: String` (Primary Key)
   - `stepCount: Int`
   - `lastUpdatedMillis: Long`

---

## 3. Caveats

1. **Offline Test Execution vs Live Gemini API**:
   - Automated unit test suites MUST use `FixtureNutritionParser` by default. Live API testing requires a valid `GEMINI_API_KEY` and network access, which cannot be guaranteed in isolated build environments.
2. **Step Sensor Emulation**:
   - On Android emulators or headless CI environments, hardware step counters (`Sensor.TYPE_STEP_COUNTER`) may not produce hardware events. The `StepRepository` must provide a mockable or manual step input stream alongside sensor integration.
3. **Multi-Meal Aggregation Window**:
   - In hostel life, meals are spaced as Breakfast (8:00 AM), Lunch (1:00 PM), Snacks (5:00 PM), and Dinner (8:30 PM). Evening workouts typically occur between 6:00 PM and 8:00 PM. Triggers evaluate based on all meals logged prior to the workout session on that calendar date.

---

## 4. Conclusion

1. **AI Nutrition Parsing**:
   A dual-engine parser architecture (`GeminiNutritionParser` for live production with structured JSON schema; `FixtureNutritionParser` for offline unit tests with verified hostel mess fixtures) provides robust, deterministic parsing of natural language Indian hostel meals (`"mutton, 4 dry rotis, eggs"`, `"dal and rice"`, etc.) satisfying Acceptance Criteria 1 and 2.

2. **V-Taper PPL Framework**:
   All 6 training days and Day 7 active rest are rigorously modeled down to the exercise ID, target muscles, rep ranges, rest periods, and precise biomechanical form notes, fulfilling Acceptance Criteria 4.

3. **Dynamic Compensatory Workout Engine**:
   The engine accurately encodes:
   - High-Carb Trigger: $+1$ set to Lat Pulldowns and Squats.
   - Low-Protein Deficit Trigger: Caps all exercises at maximum 3 sets.
   - Clear conflict resolution: Safety cap of 3 sets overrides volume additions.
   - Step Counter Sync: Triggers an alert when steps $< 5,000$ at or after 6:00 PM recommending a 10,000-step walk.
   This fulfills Acceptance Criteria 3, 4, and 5.

4. **Architecture & Persistence**:
   A Clean Architecture pattern with Room DB, Repositories, Use Cases, ViewModels, and StateFlow delivers clean separation of concerns, complete testability, and reactive UI updates.

---

## 5. Verification Method

### 5.1 Automated Unit Test Specifications
The following test suites must be executed to independently verify all business logic:

1. **`NutritionParserTest` (`test/domain/NutritionParserTest.kt`)**:
   - `parseMeal_muttonRotisEggs_returnsAccurateHighProteinAndCarb`: Verifies `"mutton, 4 dry rotis, eggs"` returns $\ge 40$g protein, $\ge 60$g carbs, `isHighCarb = true`, `isLowProtein = false`.
   - `parseMeal_dalAndRice_returnsLowProteinAndHighCarb`: Verifies `"dal and rice"` returns $< 20$g protein, $\ge 60$g carbs, `isHighCarb = true`, `isLowProtein = true`.
   - `parseMeal_paneerAndParathas_returnsHighCarbModerateProtein`: Verifies `"paneer bhurji and 3 parathas"` returns $\ge 25$g protein, $\ge 60$g carbs.
   - `parseMeal_eggWhitesAndRotis_returnsBalanced`: Verifies `"4 boiled egg whites and 2 rotis"` returns `isHighCarb = false`, `isLowProtein = false`.

2. **`CompensatoryEngineTest` (`test/domain/CompensatoryEngineTest.kt`)**:
   - `evaluateWorkout_highCarbTrigger_addsOneSetToLatPulldownsAndSquats`:
     - Input: Day 1 (Pull A) + High-Carb Meal.
     - Verify: Wide-Grip Lat Pulldowns sets change from 4 to 5. Other exercises (Seated Cable Rows: 3, Rear Delt Flyes: 4, Hammer Curls: 3) remain unchanged.
     - Input: Day 3 (Legs & Core) + High-Carb Meal.
     - Verify: Goblet Squats sets change from 4 to 5; Bodyweight Squats change from 2 to 3.
   - `evaluateWorkout_proteinDeficitTrigger_capsAllExercisesAtThreeSets`:
     - Input: Day 1 (Pull A) + Low-Protein Meal (`"dal and rice"`).
     - Verify: Wide-Grip Lat Pulldowns capped at 3 (from 4); Machine Rear Delt Flyes capped at 3 (from 4); Seated Cable Rows remains 3; Hammer Curls remains 3.
     - Input: Day 6 (Weak-Point Pump) + Low-Protein Meal.
     - Verify: Lat Pulldowns capped at 3 (from 7); Cable Lateral Raises capped at 3 (from 5); Face Pulls remains 3.
   - `evaluateWorkout_combinedCarbAndProteinDeficit_enforcesThreeSetCap`:
     - Input: Meal with both high carbs ($\ge 60$g) and low protein ($< 20$g).
     - Verify: Wide-Grip Lat Pulldowns sets $= 3$ (not 5); Goblet Squats sets $= 3$ (not 5).
   - `evaluateWorkout_normalBalancedMeal_preservesBaselineSets`:
     - Input: Balanced meal without triggers.
     - Verify: All sets and reps exactly equal baseline.

3. **`StepCounterSyncTest` (`test/domain/StepCounterSyncTest.kt`)**:
   - `evaluateStepAlert_steps4000_time1759_returnsNoAlert`: Verifies no alert triggered before 6:00 PM.
   - `evaluateStepAlert_steps4999_time1800_triggersAlert`: Verifies alert triggers at 6:00 PM when steps $< 5,000$.
   - `evaluateStepAlert_steps5000_time1800_returnsNoAlert`: Verifies no alert triggered when steps $\ge 5,000$ at 6:00 PM.
   - `evaluateStepAlert_steps3500_time2030_triggersAlert`: Verifies alert persists after 6:00 PM if still under 5,000 steps.

4. **`VTaperFrameworkStructureTest` (`test/domain/VTaperFrameworkStructureTest.kt`)**:
   - Verifies Days 1 through 7 match all requirements in R3.
   - Verifies Day 6 FST-7 Lat Pulldown protocol: 7 sets x 14 reps, 45s rest.
   - Verifies Day 7 Active Rest step target: 5,000 to 10,000 steps.

5. **`DailyNutritionAggregationTest` (`test/data/DailyNutritionAggregationTest.kt`)**:
   - Verifies sequential logging of multiple meals updates daily totals, meal count, and cumulative flags correctly.

### 5.2 Verification Commands
Run unit tests via Gradle strictly on Drive D:
```powershell
$env:GRADLE_USER_HOME = "D:\gradle_user_home"
$env:ANDROID_USER_HOME = "D:\android_user_home"
Set-Location -Path "D:\projects\hostel_gains"
.\gradlew test --stacktrace
```
Verification passes when:
- 100% of unit tests pass.
- No network requests are made during unit test execution.
- No writes occur to Drive C:.
