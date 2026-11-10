# Milestone M2 (AI Nutrition Parsing & Macro Logging) Adversarial Challenge Report

**Author**: Challenger 2 (Empirical Challenger)  
**Date**: 2026-11-10T15:30:00Z  
**Verdict**: **REQUEST_CHANGES**

---

## 1. Observation

### 1.1 Test Suite Execution Results
- Executed command strictly on Drive D::
  ```powershell
  $env:GRADLE_USER_HOME = "D:\.gradle"
  $env:ANDROID_USER_HOME = "D:\.android"
  $env:ANDROID_HOME = "D:\Android\sdk"
  $env:ANDROID_SDK_ROOT = "D:\Android\sdk"
  Set-Location "D:\projects\hostel_gains"
  .\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest --info
  ```
- Build execution result:
  - Exit code: `1` (BUILD FAILED)
  - `211 tests completed, 7 failed`
  - XML Reports located in `D:\projects\hostel_gains\app\build\test-results\testDebugUnitTest\`:
    - `TEST-com.hostelgains.data.nutrition.ChallengerM2AdversarialStressTest.xml`: 37 tests, 1 failure (36 passed)
    - `TEST-com.hostelgains.domain.challenger.M2AdversarialStressTest.xml`: 22 tests, 6 failures (16 passed)
    - `TEST-com.hostelgains.data.nutrition.DailyNutritionAggregationTest.xml`: 9 tests, 0 failures (9 passed)
    - `TEST-com.hostelgains.data.nutrition.NutritionParserTest.xml`: 12 tests, 0 failures (12 passed)
    - `TEST-com.hostelgains.domain.ChallengerM1AdversarialStressTest.xml`: 27 tests, 0 failures (27 passed)
    - `TEST-com.hostelgains.domain.CompensatoryEngineTest.xml`: 5 tests, 0 failures (5 passed)
    - `TEST-com.hostelgains.domain.StepCounterSyncTest.xml`: 5 tests, 0 failures (5 passed)
    - `TEST-com.hostelgains.domain.VTaperFrameworkStructureTest.xml`: 10 tests, 0 failures (10 passed)
    - `TEST-com.hostelgains.e2e.Tier1FeatureCoverageTest.xml`: 25 tests, 0 failures (25 passed)
    - `TEST-com.hostelgains.e2e.Tier2BoundaryCornerCaseTest.xml`: 25 tests, 0 failures (25 passed)
    - `TEST-com.hostelgains.e2e.Tier3CombinationsTest.xml`: 6 tests, 0 failures (6 passed)
    - `TEST-com.hostelgains.e2e.Tier4HostelScenariosTest.xml`: 5 tests, 0 failures (5 passed)

### 1.2 Verbatim Test Failures & Stack Traces

1. **Failure 1**: Multi-token combination in `ChallengerM2AdversarialStressTest.kt:209`:
   ```
   testcase: testMassiveQuantities_multiTokenCombination_accumulatesAllStaples
   java.lang.AssertionError: expected:<55.5> but was:<8.5>
       at org.junit.Assert.fail(Assert.java:89)
       at org.junit.Assert.failNotEquals(Assert.java:835)
       at org.junit.Assert.assertEquals(Assert.java:577)
       at org.junit.Assert.assertEquals(Assert.java:701)
       at com.hostelgains.data.nutrition.ChallengerM2AdversarialStressTest$testMassiveQuantities_multiTokenCombination_accumulatesAllStaples$1.invokeSuspend(ChallengerM2AdversarialStressTest.kt:223)
   ```

2. **Failure 2**: Cumulative protein deficit recovery in `M2AdversarialStressTest.kt:580`:
   ```
   testcase: testCumulativeProteinDeficit_triggersWhen2MealsTotalUnder50g
   java.lang.AssertionError: Total protein reaches 59g (>= 50g), deficit must resolve
       at org.junit.Assert.fail(Assert.java:89)
       at org.junit.Assert.assertTrue(Assert.java:42)
       at org.junit.Assert.assertFalse(Assert.java:65)
       at com.hostelgains.domain.challenger.M2AdversarialStressTest.testCumulativeProteinDeficit_triggersWhen2MealsTotalUnder50g(M2AdversarialStressTest.kt:580)
   ```

3. **Failures 3-6**: Multi-item meals with Hindi food terms intercepted by Dal & Rice preconfiguration:
   ```
   testcase: testComplexMultiItem_allSixHindiItemsCombined
   java.lang.AssertionError: Must recognize all 6 distinct food items expected:<6> but was:<2>
       at com.hostelgains.domain.challenger.M2AdversarialStressTest.testComplexMultiItem_allSixHindiItemsCombined(M2AdversarialStressTest.kt:122)

   testcase: testPunctuationStressAndMixedCasing
   java.lang.AssertionError: expected:<6> but was:<2>
       at com.hostelgains.domain.challenger.M2AdversarialStressTest.testPunctuationStressAndMixedCasing(M2AdversarialStressTest.kt:149)

   testcase: testSlashSeparatedDenseTokens
   java.lang.AssertionError: expected:<3> but was:<2>
       at com.hostelgains.domain.challenger.M2AdversarialStressTest.testSlashSeparatedDenseTokens(M2AdversarialStressTest.kt:162)

   testcase: testEnglishWordCountsInHindiQueries
   java.lang.AssertionError: expected:<3> but was:<2>
       at com.hostelgains.domain.challenger.M2AdversarialStressTest.testEnglishWordCountsInHindiQueries(M2AdversarialStressTest.kt:173)

   testcase: testGeminiParser_unreachableHost_gracefullyFallsBack
   java.lang.AssertionError: expected:<3> but was:<2>
       at com.hostelgains.domain.challenger.M2AdversarialStressTest.testGeminiParser_unreachableHost_gracefullyFallsBack(M2AdversarialStressTest.kt:498)
   ```

### 1.3 Drive C: Integrity Verification
- Baseline check before run:
  - `C:\Users\ADMIN\.gradle`: `11/3/2026 9:50:53 PM`
  - `C:\Users\ADMIN\.android`: `10/24/2026 6:46:57 PM`
- Post-run check:
  - `C:\Users\ADMIN\.gradle`: `11/3/2026 9:50:53 PM` (unchanged)
  - `C:\Users\ADMIN\.android`: `10/24/2026 6:46:57 PM` (unchanged)
  - Zero bytes written to Drive C:.

---

## 2. Logic Chain

### 2.1 Bug 1: Multi-Item Meal Hijacking by `getPreconfiguredMeal` in `FixtureNutritionParser.kt`
1. In `app/src/main/java/com/hostelgains/domain/nutrition/FixtureNutritionParser.kt:61-79`:
   ```kotlin
   // Dal & Rice
   (normalized.contains("dal") || normalized.contains("daal")) && (normalized.contains("rice") || normalized.contains("chawal")) &&
           !normalized.contains("mutton") && !normalized.contains("chicken") && !normalized.contains("paneer") -> {
       MealNutrition(
           rawQuery = originalQuery,
           mealName = "Hostel Mess Lunch: Dal & Rice",
           proteinGrams = 8.5f,
           carbsGrams = 60.0f,
           fatsGrams = 4.5f,
           calories = 315,
           items = listOf(
               FoodItemNutrition("Hostel Dal", "1 katori (150g)", 5.0f, 15.0f, 4.0f, 115),
               FoodItemNutrition("White Rice", "1 plate cooked", 3.5f, 45.0f, 0.5f, 200)
           ),
           isHighCarb = true,
           isLowProtein = true,
           dietaryNotes = "Carb-dense with protein deficit; triggers compensatory volume ceiling"
       )
   }
   ```
2. When a student logs `"5 rotis, 4 boiled eggs, 2 katori dal, 1 plate rice"`, the string satisfies `contains("dal") && contains("rice")` and does not contain `"mutton"`, `"chicken"`, or `"paneer"`.
3. The parser prematurely matches the "Dal & Rice" static fixture, discarding the `"5 rotis"` and `"4 boiled eggs"`.
4. As observed in §1.2 Failure 1, the calculated protein is `8.5g` instead of the genuine `55.5g` (an 85% undercount).
5. Because `8.5g < 20.0g`, this sets `isLowProtein = true`, which propagates to `CompensatoryEngine` and incorrectly caps the user's evening workout at 3 sets despite a high-protein 55.5g muscle-building meal.

### 2.2 Bug 2: Permanent Protein Deficit Latch in `DailyMacroSummary.calculate`
1. In `app/src/main/java/com/hostelgains/domain/nutrition/NutritionModels.kt:78`:
   ```kotlin
   val hasProteinDeficit = meals.any { it.isLowProtein || it.proteinGrams < 20.0f } || (mealCount >= 2 && totalProtein < 50.0f)
   ```
2. The clause `meals.any { it.isLowProtein || it.proteinGrams < 20.0f }` acts as a permanent one-way latch.
3. If a student logs a morning chai or a light mid-afternoon snack with protein < 20g, `meals.any` evaluates to `true`.
4. As observed in §1.2 Failure 2, even when the student subsequently logs heavy protein meals reaching 59g (or 150g), `hasProteinDeficit` remains permanently `true`.
5. The compensatory engine therefore continues to penalize the user with a 3-set cap for the rest of the day, with no mechanism to clear the deficit once daily protein needs are satisfied.

### 2.3 Bug 3: Preposition "of" Breaks Heuristic Count Extractor
1. In `app/src/main/java/com/hostelgains/domain/nutrition/FixtureNutritionParser.kt:221, 233`:
   ```kotlin
   val count = extractCount("(\\d+|one|two|three)?\\s*(?:plate|bowl)?s?\\s*(?:rice|chawal|biryani|pulao)", defaultCount = 1)
   ```
2. If the user writes `"10 plates of biryani"` or `"3 bowls of dal"`, the literal token `"of"` prevents the regex starting at the digit from matching.
3. The regex fallback matches the bare token `"biryani"`, causing `extractCount` to default to `1`.
4. A meal of 10 plates of biryani is parsed as 1 plate (350 kcal and 10g protein instead of 3,500 kcal and 100g protein), causing a 10x macro discrepancy.

---

## 3. Caveats
- No live Gemini network calls were executed against Google's servers; simulated OkHttp network responses (HTTP 200, 429, 500, corrupt JSON, markdown codeblocks) were used to test `GeminiNutritionParser` and `SimpleJsonParser` offline with 100% repeatability.
- Implementation files in `app/src/main/` were strictly NOT modified by Challenger 2, adhering to review-only constraints.

---

## 4. Conclusion
Milestone M2 (AI Nutrition Parsing & Macro Logging) is **NOT READY FOR APPROVAL**.
The test suite fails with **7 test failures** out of 211 tests. Three critical defects must be addressed by the Worker:
1. Fix `FixtureNutritionParser.getPreconfiguredMeal` so that meals containing additional staple tokens (eggs, rotis, dahi, etc.) fall through to the multi-token heuristic tokenizer rather than getting truncated into the 2-item 8.5g protein Dal & Rice fixture.
2. Fix `DailyMacroSummary.calculate` so that cumulative protein deficit resolves when total daily protein reaches or exceeds 50g across meals, rather than permanently latching on any single snack.
3. Update regex patterns in `FixtureNutritionParser` to permit optional prepositions (e.g., `(?:of\\s*)?`).

**Verdict: REQUEST_CHANGES**

---

## 5. Verification Method
To independently reproduce the failures:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
Set-Location "D:\projects\hostel_gains"
.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
```
Inspect reports in `D:\projects\hostel_gains\app\build\test-results\testDebugUnitTest\`.
Drive C: cleanliness check:
```powershell
Get-Item "C:\Users\ADMIN\.gradle", "C:\Users\ADMIN\.android" | Select-Object FullName, LastWriteTime
```
Invalidation condition: Build must exit with code 0 and 0 failing tests across all suites.
