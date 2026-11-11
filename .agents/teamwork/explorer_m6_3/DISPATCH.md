# Explorer 3 Dispatch

## 2026-11-10T12:09:00Z
Task: Investigate Gemini Parsers, Runtime Key Injection, UI Feature Gating Badges, and Automated Test Coverage.
Working directory: D:\projects\hostel_gains\.agents\teamwork\explorer_m6_3
Workspace root: D:\projects\hostel_gains
Original Request: D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md
Master Project: D:\projects\hostel_gains\.agents\teamwork\PROJECT.md

Scope:
1. Examine `GeminiNutritionParser.kt` and `GeminiWorkoutRebalancer.kt` in domain/data layers.
2. Determine how to update them to accept the API key at runtime (via constructor, method parameter, or provider/repository) while preserving fallback to `System.getenv("GEMINI_API_KEY")` and graceful offline fallback on missing key or failure.
3. Examine `MealInputScreen.kt` and `WorkoutPlanScreen.kt` and corresponding ViewModels:
   - Identify UI buttons ("Analyze Meal with AI", "AI Workout Rebalance").
   - Define "Premium AI Active ✦" badge when key is present.
   - Define "Offline Mode" label when no key is set (still routing through offline fallback engines).
   - Define "AI Powered ✦" label when key is set (routing through live Gemini parsers).
4. Outline unit and UI state tests for R-APIKEY-5.
5. Formulate recommendations and write report to handoff.md in your working directory.

## 2026-11-10T12:08:53Z
Sender: 97171a9e-2cbf-459a-8ed3-a6ff42b19839
Content:
You are Explorer 3 (Parser & Feature Gate Explorer) for Milestone M6.
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\explorer_m6_3
You MUST read:
- D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md
- D:\projects\hostel_gains\.agents\teamwork\PROJECT.md
- D:\projects\hostel_gains\.agents\teamwork\explorer_m6_3\DISPATCH.md

Investigate:
1. GeminiNutritionParser.kt and GeminiWorkoutRebalancer.kt:
   - How API key is currently accessed (System.getenv).
   - How to refactor them to accept runtime key (e.g. from ApiKeyRepository or constructor/method param), while keeping System.getenv fallback and graceful fallback to offline engines on missing key or API failure.
2. Feature Gating Badges in MealInputScreen.kt, WorkoutPlanScreen.kt, and ViewModels:
   - "Premium AI Active ✦" badge when key is set.
   - When no key is set: buttons show "Offline Mode" label and still work via offline fallback engines (never disabled or hidden).
   - When key is set: buttons show "AI Powered ✦" and route through live Gemini API parsers.
3. Unit and UI test strategy for R-APIKEY-5.

Write your findings to D:\projects\hostel_gains\.agents\teamwork\explorer_m6_3\handoff.md.
When finished, send a message to the orchestrator with the handoff summary and path.
