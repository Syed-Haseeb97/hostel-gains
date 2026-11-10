# Dispatch: Worker M2 (AI Nutrition Parsing & Macro Logging)

## Objective
Implement Milestone M2 (AI Nutrition Parsing & Macro Logging) for "The Hostel-Gains Dynamic Optimizer" strictly on Drive D: leaving Drive C: completely untouched.

## Mandatory Inputs (Read Before Coding)
1. `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (authoritative source of truth)
2. `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md` (architecture, feature inventory, contracts)
3. `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md` (exact prompt, schema, and fixture data for Indian mess meals)
4. Existing contracts:
   - `app/src/main/java/com/hostelgains/domain/nutrition/NutritionModels.kt`
   - `app/src/main/java/com/hostelgains/domain/nutrition/NutritionParser.kt`

## Scope of M2
1. **F6: `FixtureNutritionParser` (`com.hostelgains.domain.nutrition.FixtureNutritionParser`)**:
   - Production-ready deterministic offline parser implementing `NutritionParser`.
   - Pre-calibrated lookup fixtures for authentic Indian university hostel mess meals:
     - `"mutton, 4 dry rotis, eggs"`: ~48g protein, ~68g carbs, ~23g fats, ~670 kcal (`isHighCarb = true`, `isLowProtein = false`).
     - `"dal and rice"`: ~11g protein, ~72g carbs, ~5g fats, ~380 kcal (`isHighCarb = true`, `isLowProtein = true`).
     - `"paneer bhurji and 3 parathas"`: ~28g protein, ~65g carbs, ~26g fats, ~610 kcal (`isHighCarb = true`, `isLowProtein = false`).
     - `"4 boiled egg whites and 2 rotis"`: ~18g protein, ~34g carbs, ~1g fats, ~220 kcal (`isHighCarb = false`, `isLowProtein = false`).
     - `"chai and 2 samosas"`: ~6g protein, ~62g carbs, ~22g fats, ~470 kcal (`isHighCarb = true`, `isLowProtein = true`).
   - Robust tokenizer fallback for arbitrary meal strings containing Indian food tokens (roti, dal, rice, egg, mutton, chicken, paneer, paratha, milk, curd).
2. **F7: `GeminiNutritionParser` (`com.hostelgains.domain.nutrition.GeminiNutritionParser`)**:
   - Implements `NutritionParser` using OkHttp client to call Google Gemini REST API (`https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey`).
   - Uses system prompt and JSON schema enforcing exact food items, macro totals, and boolean triggers.
   - If `GEMINI_API_KEY` is not set or network fails, automatically falls back to `FixtureNutritionParser` so users never get an unhandled crash.
3. **F8: `MealRepository` (`com.hostelgains.data.nutrition.MealRepository`)**:
   - Interface and `DefaultMealRepository`:
     - `fun logMeal(meal: MealNutrition)`
     - `fun deleteMeal(mealId: String)`
     - `fun getMealsForDate(dateIso: String): StateFlow<List<MealNutrition>>`
     - `fun getDailySummary(dateIso: String): StateFlow<DailyMacroSummary>`
   - Dynamically aggregates daily protein, carbs, fats, calories upon logging each meal.
   - Evaluates cumulative triggers:
     - `hasHighCarbTrigger`: any meal has `isHighCarb == true` OR daily carbs >= 200g.
     - `hasProteinDeficitTrigger`: any main meal has `isLowProtein == true` OR (after 2+ meals) daily protein < 50g.
4. **Automated Unit Tests**:
   - `app/src/test/java/com/hostelgains/data/nutrition/NutritionParserTest.kt`:
     - Verifies parsing of all Indian mess meals via `FixtureNutritionParser`.
     - Verifies token fallback.
     - Verifies `GeminiNutritionParser` fallback when offline.
   - `app/src/test/java/com/hostelgains/data/nutrition/DailyNutritionAggregationTest.kt`:
     - Verifies sequential meal logging dynamically updates daily totals.
     - Verifies cumulative trigger state transitions.
5. **Test Execution**:
   - Run tests:
     ```powershell
     $env:GRADLE_USER_HOME = "D:\.gradle"
     $env:ANDROID_USER_HOME = "D:\.android"
     $env:ANDROID_HOME = "D:\Android\sdk"
     $env:ANDROID_SDK_ROOT = "D:\Android\sdk"
     Set-Location "D:\projects\hostel_gains"
     .\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
     ```
   - Confirm 100% pass rate and zero writes to Drive C:.

## Write Ownership
You own:
- `app/src/main/java/com/hostelgains/domain/nutrition/FixtureNutritionParser.kt`
- `app/src/main/java/com/hostelgains/domain/nutrition/GeminiNutritionParser.kt`
- `app/src/main/java/com/hostelgains/data/nutrition/MealRepository.kt`
- `app/src/test/java/com/hostelgains/data/nutrition/NutritionParserTest.kt`
- `app/src/test/java/com/hostelgains/data/nutrition/DailyNutritionAggregationTest.kt`
Do NOT touch other agent working directories.

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Deliverable
Write your report to `D:\projects\hostel_gains\.agents\teamwork\worker_m2\handoff.md`.
Send a completion message back to orchestrator.

## 2026-11-10T02:44:49Z
Received from orchestrator (369d8cfd-670f-4689-a71e-4ac9454c73da):
You are Worker M2 for Milestone M2 (AI Nutrition Parsing & Macro Logging) of "The Hostel-Gains Dynamic Optimizer".
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\worker_m2

Read:
- D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md (authoritative source of truth)
- D:\projects\hostel_gains\.agents\teamwork\PROJECT.md
- D:\projects\hostel_gains\.agents\teamwork\worker_m2\DISPATCH.md
- D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md

Your task is to implement:
1. `FixtureNutritionParser`: deterministic offline Indian hostel mess food parser (mutton, dry rotis, eggs, dal, rice, paneer, paratha, samosa) with token fallback.
2. `GeminiNutritionParser`: live REST API client using OkHttp to call Gemini API with graceful fallback to FixtureNutritionParser.
3. `MealRepository`: stateful repository aggregating logged meals into DailyMacroSummary with reactive StateFlows and cumulative trigger flags.
4. Unit tests: `NutritionParserTest` and `DailyNutritionAggregationTest`.
5. Run `./gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest` and verify all tests pass on Drive D: with zero writes to Drive C:.

