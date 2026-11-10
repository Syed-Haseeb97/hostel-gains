# Test Infrastructure & Strategy: The Hostel-Gains Dynamic Optimizer

## 1. Test Philosophy & Architecture

### 1.1 Opaque-Box, Requirement-Driven Testing
The test suite for "The Hostel-Gains Dynamic Optimizer" is strictly designed as an **opaque-box, requirement-driven E2E test harness**.
- **Specification Authority**: All test assertions, boundary limits, and expected outputs are derived exclusively from `ORIGINAL_REQUEST.md` (authoritative source of truth) and `PROJECT.md` interface contracts.
- **Contract Adherence**: Tests interact only with public domain interfaces (`NutritionParser`, `CompensatoryEngine`, `StepAlertEvaluator`, `VTaperWorkoutRepository`, `DailyMacroSummary`). No internal private methods, reflection, or implementation details are accessed or assumed.
- **Progressive Testability & Zero Facade Guarantee**: Tests exercise authentic business logic, dynamic set calculations, macro parsing algorithms, and alert rules. No facade tests, mocking shortcuts that bypass logic, or hardcoded pass assertions are permitted.
- **Deterministic Offline Execution**: To ensure 100% CI reproducibility and fast headless execution (<2 seconds), offline fixture parsers (`FixtureNutritionParser`) and deterministic clock injectors are used for automated test runs without network latency or external API key dependencies.

### 1.2 Platform & Storage Isolation (Drive D: Enforcement)
All test runs strictly execute on Drive D: with zero writes to Drive C:.
- Gradle user home: `D:\.gradle`
- Android user home: `D:\.android`
- Android SDK root: `D:\Android\sdk`
- JVM temp directory: `D:\temp`
- Project root: `D:\projects\hostel_gains`

Execution command:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
Set-Location "D:\projects\hostel_gains"
.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
```

---

## 2. Feature Inventory & Test Tier Mapping

| Feature ID | Description | Primary Domain Contract | Tier 1 (Coverage) | Tier 2 (Boundary) | Tier 3 (Cross-Feature) | Tier 4 (Scenario) |
|---|---|---|---|---|---|---|
| **F1** | Strict Drive D: Build Setup | Build environment / Gradle | Validated by runner | Zero C: writes | Isolated execution | Multi-test run |
| **F2** | Core Domain Models | `com.hostelgains.domain.model.*` | Model integrity | Zero/empty fields | Multi-model aggregation | Realistic daily state |
| **F3** | V-Taper PPL Framework Database | `VTaperWorkoutRepository` | Days 1 to 7 base plans | Boundary reps/sets | Cross-day transitions | Day 6/7 protocols |
| **F4** | Dynamic Compensatory Workout Engine | `CompensatoryEngine` | Carb bonus, Protein cap | 59g vs 60g, 19.9 vs 20g | Carb + Protein conflict | Dal-rice & mutton feast |
| **F5** | Step Counter Sync & 6 PM Alert Engine | `StepAlertEvaluator` | Sub-5k step alert | 17:59 vs 18:00, 4999 vs 5000 | Step deficit + Carb surplus | Exam crunch sedentary day |
| **F6** | AI Nutrition Parsing Interface & Fixtures | `NutritionParser`, `FixtureNutritionParser` | Indian hostel meals parsing | Empty/noisy queries | Multi-item meal parsing | Full day meal logging |
| **F8** | Daily Macronutrient Aggregator | `DailyMacroSummary`, Repository | Macro summing | 0-macro meals | Multi-meal accumulation | Full 24-hr hostel cycle |

---

## 3. Four-Tier Test Suite Specification

### Tier 1: Feature Coverage (>=5 Tests Per Feature)
Location: `app/src/test/java/com/hostelgains/e2e/Tier1FeatureCoverageTest.kt`
- **Feature A: Nutrition Parsing (`Tier1NutritionParsingTest`)**:
  1. `testMuttonCurryRotisEggsParsing`: Validates non-veg hostel dinner parsing (`"mutton, 4 dry rotis, eggs"`) computes high protein (>=40g) and carbs (>=60g).
  2. `testDalAndRiceParsing`: Validates vegetarian mess lunch (`"dal and rice"`) computes low protein (<20g) and high carbs (>=60g).
  3. `testPaneerBhurjiAndParathasParsing`: Validates high-protein vegetarian breakfast (`"paneer bhurji and 3 parathas"`).
  4. `testBoiledEggsAndRotisParsing`: Validates fitness meal (`"4 boiled egg whites and 2 rotis"`).
  5. `testChaiAndSamosaParsing`: Validates hostel snack parsing (`"chai and 2 samosas"`).
- **Feature B: V-Taper PPL Framework Database (`Tier1WorkoutFrameworkTest`)**:
  1. `testDay1PullASchedule`: Verifies Wide-Grip Lat Pulldowns (4x8-12), Seated Cable Rows (3x8-12), Machine Rear Delt Flyes (4x10-15), Dumbbell Hammer Curls (3x8-12).
  2. `testDay2PushASchedule`: Verifies Incline DB Press (4x8-12), Lateral Raises (5x10-15), Incline Push-ups (3xFailure), Tricep Rope Pushdowns (3x8-12).
  3. `testDay3LegsCoreSchedule`: Verifies Goblet Squats (4x8-12), BW Squats (2xFailure), Leg Curls (3x8-12), Plank (3x60s).
  4. `testDay6WeakPointPumpSchedule`: Verifies FST-7 Lat Pulldowns (7 sets x 14 reps, 45s rest), Cable Lateral Raises (5x12), Face Pulls (3x15).
  5. `testDay7ActiveRestSchedule`: Verifies Day 7 active recovery (5,000 to 10,000 steps, zero resistance exercises).
- **Feature C: Dynamic Carb Bonus Trigger (`Tier1CarbBonusTest`)**:
  1. `testCarbBonusAddsSetToLatPulldownsOnPullA`: High carb meal adds +1 set to Lat Pulldowns (4 -> 5 sets).
  2. `testCarbBonusDoesNotAddSetToOtherPullExercises`: Rows, rear delts, curls remain unchanged.
  3. `testCarbBonusAddsSetToSquatsOnLegDay`: High carb meal adds +1 set to Goblet Squats (4 -> 5) and Bodyweight Squats (2 -> 3).
  4. `testCarbBonusAddsSetToLatPulldownsOnDay6Pump`: Lat Pulldowns on Day 6 increased from 7 to 8 sets.
  5. `testCarbBonusOnPushDayDoesNotAlterPushExercises`: Exercises without compensatory tag remain at baseline.
- **Feature D: Dynamic Protein Deficit Cap (`Tier1ProteinCapTest`)**:
  1. `testProteinCapRestricts4SetExercisesTo3`: Exercises with baseline 4 sets (e.g. Lat Pulldowns, Incline DB Press) capped at 3 sets.
  2. `testProteinCapRestricts5SetExercisesTo3`: DB Lateral Raises (baseline 5 sets) capped at 3 sets.
  3. `testProteinCapRestricts7SetExercisesTo3`: Day 6 FST-7 Lat Pulldowns (baseline 7 sets) strictly capped at 3 sets.
  4. `testProteinCapLeaves3SetExercisesAt3`: Seated Cable Rows (baseline 3 sets) remains 3 sets.
  5. `testProteinCapLeaves2SetExercisesAt2`: Bodyweight Squats (baseline 2 sets) remains 2 sets.
- **Feature E: Step Counter Sync & 6 PM Alert (`Tier1StepAlertTest`)**:
  1. `testSub5kStepsAt6PmTriggersAlert`: 4,500 steps at 18:00 triggers alert.
  2. `test5kStepsOrMoreAt6PmSuppressesAlert`: 5,000 steps at 18:00 does not trigger alert.
  3. `testSub5kStepsBefore6PmSuppressesAlert`: 3,000 steps at 17:55 does not trigger alert.
  4. `testSub5kStepsLateEveningMaintainsAlert`: 3,800 steps at 20:30 maintains alert.
  5. `testAlertMessageSpecifies10kStepsPrompt`: Evaluates alert recommendation contains prompt for 10,000 steps after dinner.

---

### Tier 2: Boundary & Corner Cases (>=5 Tests Per Feature)
Location: `app/src/test/java/com/hostelgains/e2e/Tier2BoundaryCornerCaseTest.kt`
- **Boundary A: Carbohydrate Trigger (59.0g vs 60.0g)**:
  1. `testCarbs59GramsDoesNotTriggerBonus`: Exactly 59.0g carbs leaves Lat Pulldowns at 4 sets.
  2. `testCarbs59Point9GramsDoesNotTriggerBonus`: 59.9g carbs does not trigger carb bonus.
  3. `testCarbs60GramsTriggersBonus`: Exactly 60.0g carbs triggers +1 set bonus.
  4. `testCarbs60Point1GramsTriggersBonus`: 60.1g carbs triggers +1 set bonus.
  5. `testCarbsExtremeZeroGrams`: 0.0g carbs handles safely without exceptions.
- **Boundary B: Protein Deficit Trigger (19.9g vs 20.0g)**:
  1. `testProtein19Point9GramsTriggersCap`: 19.9g protein triggers low-protein deficit cap (caps at 3).
  2. `testProtein20Point0GramsDoesNotTriggerCap`: Exactly 20.0g protein does not trigger cap (preserves 4+ sets).
  3. `testProtein20Point1GramsDoesNotTriggerCap`: 20.1g protein does not trigger cap.
  4. `testProtein0GramsTriggersCap`: Exactly 0.0g protein safely triggers deficit cap.
  5. `testNegativeProteinHandledSafely`: Negative or corrupted inputs clamped to 0.
- **Boundary C: Time Boundary (17:59 vs 18:00)**:
  1. `testTime1759DoesNotTriggerAlert`: At 17:59 with 4,000 steps, alert is false.
  2. `testTime1800TriggersAlert`: At 18:00 with 4,000 steps, alert is true.
  3. `testTime1801TriggersAlert`: At 18:01 with 4,000 steps, alert is true.
  4. `testTime2359TriggersAlert`: At 23:59 with 4,000 steps, alert is true.
  5. `testTime0000DoesNotTriggerAlert`: At 00:00 (midnight start of next day), alert is false.
- **Boundary D: Step Count Boundary (4,999 vs 5,000 Steps)**:
  1. `testSteps4999At1800TriggersAlert`: 4,999 steps at 18:00 triggers alert.
  2. `testSteps5000At1800SuppressesAlert`: Exactly 5,000 steps at 18:00 suppresses alert.
  3. `testSteps5001At1800SuppressesAlert`: 5,001 steps at 18:00 suppresses alert.
  4. `testSteps0At1800TriggersAlert`: 0 steps at 18:00 triggers alert with proper formatting.
  5. `testStepsNegativeHandledSafely`: Negative steps clamped to 0.
- **Boundary E: Meal Text Input Edge Cases**:
  1. `testEmptyMealDescriptionThrowsOrReturnsEmpty`: Empty string `""` handled gracefully.
  2. `testWhitespaceOnlyMealDescription`: `"   \t\n "` returns empty or zero macros.
  3. `testSpecialCharactersMealDescription`: `"!@#$%^&*()_+{}[]:;<>?"` handled without crash.
  4. `testExtremelyLongMealDescription`: 5,000+ characters parsed without buffer overflow.
  5. `testUnrecognizedFoodTokens`: Unrecognized text returns zero macros or safe fallback.

---

### Tier 3: Cross-Feature Combinations (Pairwise & Multi-System)
Location: `app/src/test/java/com/hostelgains/e2e/Tier3CrossFeatureCombinationsTest.kt`
- **Combination 1: High-Carb + Low-Protein Conflict Resolution (Precedence Rule)**:
  - Scenario: Meal with 75g carbs (Carb Bonus trigger) and 12g protein (Protein Deficit trigger) (e.g. `"dal and rice"`).
  - Test: Verifies that **Protein Deficit Cap overrides Carb Bonus** (`min(3, baseline + 1)`).
  - Result: Wide-Grip Lat Pulldowns: 4 baseline + 1 carb bonus = 5 -> capped at **3 sets**.
- **Combination 2: Multi-Meal Daily Accumulation**:
  - Scenario: Breakfast (Paneer paratha), Lunch (Dal and rice), Dinner (Mutton and rotis).
  - Test: Verifies that daily cumulative macros update sequentially and triggers accurately reflect the combined daily intake.
- **Combination 3: Step Deficit + Carb Surplus Evening Synergy**:
  - Scenario: User logs high-carb mess dinner (85g carbs) at 7:00 PM with only 3,200 steps.
  - Test: Workout engine yields +1 set for evening session while step evaluator simultaneously triggers 10,000-step walk recommendation after dinner.
- **Combination 4: Day 6 Weak-Point Pump Under Severe Protein Deficit**:
  - Scenario: Student trains Day 6 (Weak-Point Pump: 7x14 Lat Pulldowns) on a low-protein day (<20g).
  - Test: Massive volume reduction from 7 sets down to 3 sets to prevent catabolic overtraining without amino acid pool.
- **Combination 5: Day 7 Active Rest With High Carb Intake**:
  - Scenario: User eats a high-carb feast (120g carbs) on Day 7 (Active Rest).
  - Test: Active rest retains 0 resistance sets (does not add phantom sets), but step target remains 10,000 steps.

---

### Tier 4: Real-World Application Scenarios
Location: `app/src/test/java/com/hostelgains/e2e/Tier4RealWorldScenariosTest.kt`
- **Scenario 1: Typical Indian Hostel Vegetarian Day (Dal-Rice Catabolic Risk)**:
  - Student eats breakfast of chai + 2 biscuits (25g carbs, 2g protein) and hostel mess dal-rice lunch (72g carbs, 11g protein).
  - Evening Push A session dynamically caps Incline DB Press (4 -> 3 sets) and DB Lateral Raises (5 -> 3 sets).
- **Scenario 2: Sunday Hostel Non-Veg Feast (Hypertrophy Surplus)**:
  - Student logs Sunday special mess dinner: `"mutton, 4 dry rotis, eggs"` (68g carbs, 48g protein).
  - Evening Pull A session receives carb bonus (+1 set to Lat Pulldowns: 4 -> 5 sets) with full volume preserved across all other movements.
- **Scenario 3: Exam Crunch Desk Day (Sedentary Deficit)**:
  - Student studies for midterms all day at desk. At 6:30 PM (18:30), step count is only 2,150.
  - UI warning triggers: `"Step Deficit Alert"` recommending a 10,000-step walk after dinner.
- **Scenario 4: High-Volume V-Taper Specialization Day (FST-7 Optimization)**:
  - Student performs Day 6 Weak-Point Pump with optimal nutrition (mutton + eggs, 52g protein, 65g carbs).
  - FST-7 Lat Pulldowns receives carb surplus expansion to 8 sets with 45s rest periods.
- **Scenario 5: Post-Workout Walk & Recovery Synchronization**:
  - Student logs 4,200 steps at 6:00 PM (alert triggered). Takes recommended post-dinner walk adding 6,100 steps (total 10,300 steps).
  - Re-evaluating at 9:30 PM (21:30) confirms alert clears and daily goal is achieved.

---

## 4. Test Verification & CI Pass Criteria

A test run is considered **PASSED** if and only if:
1. 100% of all test methods across Tiers 1 through 4 pass without failure or error.
2. Gradle exits with return code `0`.
3. Total test count across all 4 tiers is >= 40 tests.
4. Drive C: modification timestamps remain completely unchanged (zero bytes written to Drive C:).
5. All test outputs are verifiable and documented in `TEST_READY.md`.
