# Dispatch: Worker M2 Gen 3 (Remediation)

## Objective
Apply the 3 precise, verified fixes to resolve the Milestone M2 Iteration 1 Gate failure and achieve 100% test pass rate across all 211 tests strictly on Drive D:.

## Mandatory Inputs to Read First
- `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (READ FIRST)
- `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`
- `D:\projects\hostel_gains\.agents\teamwork\auditor_m2_1\handoff.md` (Full Forensic Audit evidence)
- `D:\projects\hostel_gains\.agents\teamwork\challenger_m2_1\handoff.md` (Challenger report)

## Write Ownership
You exclusively own:
- `app/src/main/java/com/hostelgains/domain/nutrition/FixtureNutritionParser.kt`
- `app/src/main/java/com/hostelgains/domain/nutrition/NutritionModels.kt`

## Exact Fixes Required
1. In `FixtureNutritionParser.kt:61-79`:
   In `getPreconfiguredMeal` Dal & Rice rule, add exclusions so composite queries containing other items fall through to `parseHeuristic`:
   `!normalized.contains("egg") && !normalized.contains("roti") && !normalized.contains("phulka") && !normalized.contains("paratha") && !normalized.contains("dudh") && !normalized.contains("milk") && !normalized.contains("dahi") && !normalized.contains("curd") && !normalized.contains("chana") && !normalized.contains("chapati")`
2. In `FixtureNutritionParser.kt:221, 233`:
   Update quantity extraction regex to allow optional `"of"` preposition:
   `(?:of\s*)?` so queries like `"10 plates of biryani"` match count 10.
3. In `NutritionModels.kt:78`:
   In `DailyMacroSummary.calculate`:
   `val hasProteinDeficit = if (totalProtein >= 50.0f) false else (meals.any { it.isLowProtein || it.proteinGrams < 20.0f } || (mealCount >= 2 && totalProtein < 50.0f))`
4. Clean any Drive C: wrapper files if they exist:
   Remove-Item -Path "C:\Users\ADMIN\.gradle\wrapper" -Recurse -Force -ErrorAction SilentlyContinue

## Verification Command on Drive D:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
Set-Location "D:\projects\hostel_gains"
.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
```
Ensure all 211 tests pass with 0 failures and 0 Drive C: bytes written.
Write `handoff.md` upon completion.


## 2026-11-10T10:19:08Z
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

You are worker_m2_gen3 (implementation worker).
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\worker_m2_gen3

Task:
Fix the 3 isolated defects in Milestone M2 to achieve a 100% test pass rate across all 211 tests on Drive D: and ensure zero bytes on Drive C:.

Mandatory Inputs to Read First:
1. `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (READ FIRST)
2. `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`
3. Full Forensic Auditor Report: `D:\projects\hostel_gains\.agents\teamwork\auditor_m2_1\handoff.md`
4. Challenger 1 Report: `D:\projects\hostel_gains\.agents\teamwork\challenger_m2_1\handoff.md`
5. Challenger 2 Report: `D:\projects\hostel_gains\.agents\teamwork\challenger_m2_2\handoff.md`
6. `D:\projects\hostel_gains\.agents\teamwork\worker_m2_gen3\DISPATCH.md`

Files You Own Exclusively:
- `app/src/main/java/com/hostelgains/domain/nutrition/FixtureNutritionParser.kt`
- `app/src/main/java/com/hostelgains/domain/nutrition/NutritionModels.kt`

Exact Modifications:
1. In `FixtureNutritionParser.kt` `getPreconfiguredMeal` line 62 (Dal & Rice rule):
   Add exclusions so that when a query contains other staple tokens (e.g. eggs, rotis, phulkas, paneer, dahi, milk, chana, etc.), it does NOT greedily return the 2-item 8.5g protein fixture, but falls through to `parseHeuristic`:
   `!normalized.contains("egg") && !normalized.contains("roti") && !normalized.contains("phulka") && !normalized.contains("paratha") && !normalized.contains("dudh") && !normalized.contains("milk") && !normalized.contains("dahi") && !normalized.contains("curd") && !normalized.contains("chana") && !normalized.contains("chapati")`
2. In `FixtureNutritionParser.kt` lines 221 & 233 (`extractCount` regex):
   Make `"of"` optional: `(?:of\s*)?` so queries like `"10 plates of biryani"` match count 10.
3. In `NutritionModels.kt` line 78 (`DailyMacroSummary.calculate`):
   Ensure cumulative protein >= 50g clears the deficit:
   `val hasProteinDeficit = if (totalProtein >= 50.0f) false else (meals.any { it.isLowProtein || it.proteinGrams < 20.0f } || (mealCount >= 2 && totalProtein < 50.0f))`
4. Clean any Drive C: wrapper files if present:
   `Remove-Item -Path "C:\Users\ADMIN\.gradle\wrapper" -Recurse -Force -ErrorAction SilentlyContinue`

Verification Command on Drive D:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
Set-Location "D:\projects\hostel_gains"
.\gradlew.bat --gradle-user-home D:\.gradle testDebugUnitTest
```
Verify that all 211 tests pass with 0 failures and 0 Drive C: bytes written.
Write `handoff.md` and report completion when done.
