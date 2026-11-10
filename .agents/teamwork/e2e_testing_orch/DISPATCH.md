# Dispatch: E2E Testing Track Orchestrator

## Role & Scope
You are the E2E Testing Track Orchestrator for "The Hostel-Gains Dynamic Optimizer".
Your working directory is: `D:\projects\hostel_gains\.agents\teamwork\e2e_testing_orch`
Parent conversation ID: `369d8cfd-670f-4689-a71e-4ac9454c73da`

## Requirements
Read:
- `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (authoritative source of truth)
- `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`

## Mission
Design and implement a comprehensive, requirement-driven, opaque-box test suite covering all features in `PROJECT.md § Feature Inventory` across 4 Tiers:
1. **Tier 1 - Feature Coverage (>=5 per feature)**: Isolated happy-path tests for nutrition parsing, dynamic set modifiers, step alerts, 3D kinematics, and UI state models.
2. **Tier 2 - Boundary & Corner Cases (>=5 per feature)**: Boundary conditions (e.g. 59g carbs vs 60g carbs, 19g protein vs 20g protein, 4,999 steps at 17:59 vs 18:00, zero carbs, empty meals, extreme sets).
3. **Tier 3 - Cross-Feature Combinations (Pairwise)**: Interaction between high-carb + low-protein triggers (precedence rule verification), multiple meals accumulated over the day, workout day transitions, step deficit + carb surplus combinations.
4. **Tier 4 - Real-World Application Scenarios**: Realistic hostel student day-in-the-life simulations:
   - Scenario 1: Vegetarian hostel student eating dal-rice for lunch, skipping breakfast, working out in the evening (protein cap test).
   - Scenario 2: High-carb feast day (mutton curry + 4 dry rotis + eggs + rice) before evening Pull A workout (carb bonus test).
   - Scenario 3: Mixed day: dal-rice lunch (triggers carb bonus and protein cap simultaneously -> verifying protein cap safety ceiling).
   - Scenario 4: Evening 6 PM step deficit test (<5,000 steps triggers 10,000 walk recommendation).
   - Scenario 5: Day 6 Weak-Point Pump (FST-7 7x14 Lat Pulldowns) with protein deficit capping at 3 sets.

Create:
1. `D:\projects\hostel_gains\.agents\teamwork\TEST_INFRA.md` following the project template.
2. The executable test cases and test runner (using Kotlin/JUnit headlessly configured strictly on Drive D: with `GRADLE_USER_HOME=D:\.gradle`).
3. Publish `D:\projects\hostel_gains\TEST_READY.md` (and mirror at `D:\projects\hostel_gains\.agents\teamwork\TEST_READY.md`) when complete.

Update your `progress.md` periodically for liveness.
Send a completion message back when `TEST_READY.md` is published.
