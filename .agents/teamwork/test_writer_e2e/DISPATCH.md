# Dispatch: E2E Test Writer (Requirement-Driven Opaque-Box Test Suite)

## Objective
Design and implement the complete, requirement-driven, opaque-box E2E test suite for "The Hostel-Gains Dynamic Optimizer", create `TEST_INFRA.md`, and publish `TEST_READY.md`.

## Mandatory Inputs (Read Before Coding)
1. `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (authoritative source of truth)
2. `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md` (architecture, feature inventory, contracts)
3. Working directory: `D:\projects\hostel_gains\.agents\teamwork\test_writer_e2e`

## Scope of Work
1. **`TEST_INFRA.md`**: Create at `D:\projects\hostel_gains\TEST_INFRA.md` and mirror at `D:\projects\hostel_gains\.agents\teamwork\TEST_INFRA.md` detailing:
   - Test philosophy (Opaque-box, requirement-driven, zero reliance on internal private methods).
   - Feature Inventory mapping all features (F1 to F20) across Tiers 1-4.
   - Test architecture and execution commands strictly on Drive D: with `GRADLE_USER_HOME=D:\.gradle`.
2. **Comprehensive 4-Tier Test Suite Implementation**:
   - Location: `D:\projects\hostel_gains\app\src\test\java\com\hostelgains\e2e\`
   - **Tier 1 - Feature Coverage (>=5 per feature)**:
     - `Tier1NutritionParsingTest`: 5+ tests verifying parsing of mutton, rotis, eggs, dal, rice, paneer, and tea with macro calculations.
     - `Tier1WorkoutEngineTest`: 5+ tests verifying baseline sets/reps for Days 1 to 7.
     - `Tier1CompensatoryCarbTest`: 5+ tests verifying +1 set added to Lat Pulldowns and Squats when high carb meal logged.
     - `Tier1CompensatoryProteinTest`: 5+ tests verifying max 3 sets cap across all exercises when low protein meal logged.
     - `Tier1StepAlertTest`: 5+ tests verifying step tracking and alert logic.
   - **Tier 2 - Boundary & Corner Cases (>=5 per feature)**:
     - Boundary carbs: 59g carbs (no trigger) vs 60g carbs (trigger active).
     - Boundary protein: 19.9g protein (trigger active) vs 20.0g protein (no trigger).
     - Boundary time: 17:59 (no alert) vs 18:00 (alert active).
     - Boundary steps: 4,999 steps (alert active) vs 5,000 steps (no alert).
     - Zero calories / empty meal description / special characters handling.
   - **Tier 3 - Cross-Feature Combinations (Pairwise)**:
     - `Tier3CombinationsTest`: High-Carb + Low-Protein simultaneous trigger (verifying Protein Deficit Cap acts as an absolute physiological ceiling: `min(3, baseline + 1)`).
     - Multiple meal aggregation throughout the day (Breakfast + Lunch + Dinner cumulative macros).
     - Step deficit + Carb surplus workout session.
     - Day 6 Weak-Point Pump (7x14 Lat Pulldowns) under Protein Deficit Cap (strictly capped at 3 sets).
   - **Tier 4 - Real-World Application Scenarios**:
     - `Tier4HostelScenariosTest`:
       1. Indian hostel vegetarian day: Dal and rice lunch -> protein deficit cap enforced on evening Push A workout.
       2. Hostel Sunday feast: Mutton curry + 4 rotis + eggs -> carb bonus (+1 set to Lat Pulldowns/Squats) with high protein.
       3. Exam crunch day: Student sedentary at desk with 2,400 steps at 6:30 PM -> 10,000-step evening walk alert triggered.
       4. Recomposition day: Day 7 Active Rest -> verifying 5,000 to 10,000 step target and 0 resistance sets.
       5. High-volume pump day: Day 6 FST-7 Lat Pulldowns under normal vs low-protein conditions.
3. **Publish `TEST_READY.md`**:
   - Create `D:\projects\hostel_gains\TEST_READY.md` and mirror at `D:\projects\hostel_gains\.agents\teamwork\TEST_READY.md` summarizing runner commands, pass/fail semantics, and exact test counts per tier.

## Write Ownership
You own:
- `D:\projects\hostel_gains\TEST_INFRA.md`
- `D:\projects\hostel_gains\TEST_READY.md`
- `D:\projects\hostel_gains\app\src\test\java\com\hostelgains\e2e\`
Do NOT touch other agent working directories.

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Deliverable
Write your completion report to `D:\projects\hostel_gains\.agents\teamwork\test_writer_e2e\handoff.md`.
Send a message to orchestrator when `TEST_READY.md` is published.


## 2026-11-10T02:00:55Z
[Message] timestamp=2026-11-10T02:00:55Z sender=369d8cfd-670f-4689-a71e-4ac9454c73da priority=MESSAGE_PRIORITY_HIGH content=You are the E2E Test Writer for "The Hostel-Gains Dynamic Optimizer".
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\test_writer_e2e

Read:
- D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md (authoritative source of truth)
- D:\projects\hostel_gains\.agents\teamwork\PROJECT.md
- D:\projects\hostel_gains\.agents\teamwork\test_writer_e2e\DISPATCH.md
- D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md

Your task is to design and implement the complete, requirement-driven, opaque-box E2E test suite across Tiers 1-4:
1. Create TEST_INFRA.md at D:\projects\hostel_gains\TEST_INFRA.md (and mirror at D:\projects\hostel_gains\.agents\teamwork\TEST_INFRA.md).
2. Implement 4-tier comprehensive unit/E2E test suites in `app/src/test/java/com/hostelgains/e2e/`:
   - Tier 1: Feature Coverage (>=5 tests per feature: nutrition parsing, workout engine, carb bonus, protein cap, step alert).
   - Tier 2: Boundary & Corner Cases (>=5 tests per feature: 59g vs 60g carbs, 19.9g vs 20g protein, 17:59 vs 18:00, 4,999 vs 5,000 steps, empty/invalid meals).
   - Tier 3: Cross-Feature Combinations (Pairwise: high-carb + low-protein simultaneous trigger with protein cap precedence, multi-meal accumulation, step deficit + carb surplus).
   - Tier 4: Real-World Application Scenarios (Realistic hostel scenarios: Indian vegetarian dal-rice day, Sunday mutton feast, exam crunch sedentary day, active rest day).
3. Ensure all tests strictly target Drive D: and rely on public domain interfaces defined in PROJECT.md.
4. When test suite is complete, create TEST_READY.md at D:\projects\hostel_gains\TEST_READY.md (and mirror at D:\projects\hostel_gains\.agents\teamwork\TEST_READY.md).

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Document your work in D:\projects\hostel_gains\.agents\teamwork\test_writer_e2e\handoff.md and send a completion message back.
