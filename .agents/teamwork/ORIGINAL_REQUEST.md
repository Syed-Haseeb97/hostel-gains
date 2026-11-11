# Original User Request

## Initial Request — 2026-11-10T01:44:54Z

Build "The Hostel-Gains Dynamic Optimizer", a native Android personal production mobile application in Kotlin and Jetpack Compose that tracks university hostel mess meals, calculates macronutrient intake via AI API integration, and dynamically adjusts evening bodyweight and gym workouts (following the V-Taper PPL framework) and daily step targets.

Working directory: D:\projects\hostel_gains
Integrity mode: development

## Requirements

### R1. Native Android Application & UI with 3D Assets
Build a native Android application using Kotlin and Jetpack Compose.
Extract the 3D models and UI resources from `D:\downloads\Build a 3D Fitness Planner.zip` and integrate them directly into the application's interface and workout screens.

### R2. Mess Menu Input & AI Macronutrient Calculation
Allow the user to log daily mess meals using natural language text descriptions (e.g., "mutton, 4 dry rotis, eggs", "dal and rice").
Integrate an AI/LLM API to parse meal descriptions and compute estimated macronutrient totals (protein, carbohydrates, fats, and total calories) for each meal and daily total.

### R3. V-Taper PPL Framework & Dynamic Compensatory Workout Engine
Implement the 6-day V-Taper PPL framework (Day 1: Pull A, Day 2: Push A, Day 3: Legs/Core, Day 4: Pull B, Day 5: Push B, Day 6: Weak-Point Pump, Day 7: Active Rest) with exercise models, target sets, rep ranges, and form notes:
- Day 1 (Pull A): Wide-Grip Lat Pulldowns (4 sets, 8-12 reps), Seated Cable Rows (3 sets, 8-12 reps), Machine Rear Delt Flyes (4 sets, 10-15 reps), Dumbbell Hammer Curls (3 sets, 8-12 reps).
- Day 2 (Push A): Incline Dumbbell Press (4 sets, 8-12 reps), Dumbbell Lateral Raises (5 sets, 10-15 reps), Incline Push-ups (3 sets to failure), Tricep Rope Pushdowns (3 sets, 8-12 reps).
- Day 3 (Legs & Core): Goblet Squats (4 sets, 8-12 reps), Bodyweight Squats (2 sets to failure), Lying Leg Curls (3 sets, 8-12 reps), Plank (3 sets, 60 sec).
- Day 4 (Pull B): Lat thickness (horizontal pulls) and biceps focus.
- Day 5 (Push B): Lateral delts (cable focus) and triceps focus.
- Day 6 (Weak-Point Pump): Lat Pulldowns (7 sets of 14 reps, 45s rest), Cable Lateral Raises (5 sets of 12 reps), Face Pulls (3 sets of 15 reps).
- Day 7 (Active Rest): 5,000 - 10,000 steps only.

The app must dynamically modify the scheduled workout based on logged nutrition and activity:
- Carb Trigger: When a high-carb meal is logged, dynamically add 1 extra set to Lat Pulldowns and Squats in the evening session.
- Protein Deficit Trigger: When low protein intake is logged (e.g., vegetarian dal and rice meals), automatically cap all exercises at a maximum of 3 sets.
- Step Counter Sync: Monitor daily steps and trigger a UI alert if the user is under 5,000 steps by 6 PM, prompting 10,000 steps after dinner to stay on track for body recomposition.

### R4. Storage & Platform Constraints
All project workspaces, source code, dependencies, build caches (including Gradle and Android caches), and artifacts must strictly reside on Drive D: (`D:\projects\hostel_gains` or `D:\`), leaving Drive C: completely untouched.

## Acceptance Criteria

### Macronutrient Parsing & Calculation
- [ ] Automated unit tests verify that natural language meal descriptions (including Indian hostel mess items such as mutton, rotis, eggs, dal, and rice) are parsed into protein, carbohydrates, fats, and calories via AI API integration.
- [ ] Daily macronutrient totals update dynamically upon logging each meal.

### Dynamic Workout Adjustment Engine
- [ ] Automated unit tests verify that logging a high-carbohydrate meal adds 1 extra set to Lat Pulldowns and Squats in the evening workout.
- [ ] Automated unit tests verify that logging a low-protein meal caps all exercise sets at a maximum of 3 sets.
- [ ] Automated tests verify that when daily steps are below 5,000 at or after 6:00 PM, a UI alert is triggered recommending a 10,000-step evening walk.
- [ ] Exercise database accurately models all 6 training days and Day 7 active rest with their respective exercises, sets, reps, and form guidance.

### UI & Asset Integration
- [ ] Project builds cleanly with Gradle on Drive D: without writing to Drive C:.
- [ ] 3D models and UI assets extracted from `D:\downloads\Build a 3D Fitness Planner.zip` are integrated into the Jetpack Compose UI.
- [ ] Core screens (Meal Input & Macro Summary, Dynamic Workout Plan, Step Status & Alert) render and function correctly.


## Follow-up — 2026-11-10T10:11:05Z

Subject: Feature Request - Dynamic Exercise Rebalancer & Equipment Fallback Engine

Hello Teamwork Lead,

Please incorporate a new feature into "The Hostel-Gains Dynamic Optimizer": the Dynamic Exercise Rebalancer & Equipment Fallback Engine.

This feature allows the user to report a skipped, unavailable, or painful exercise (e.g., equipment busy in hostel gym, floor wet, injury, lack of time) either via natural language or a quick UI action, and have the app dynamically redistribute the lost volume to synergistic exercises in that day's routine to protect weekly V-taper progression.

### 1. Requirements

#### R-REBALANCE-1: Natural Language & Quick Action Trigger
- Allow the user to input a statement such as:
  - "No Lat Pulldown machine today, only dumbbells and floor"
  - "Wrist hurts, had to skip Incline DB press"
  - "Couldn't do squats today"
- Provide an inline "Skip / Can't Do" action on individual exercises in the active workout screen.

#### R-REBALANCE-2: AI Rebalance Subsystem (GeminiWorkoutRebalancer)
- Send the user's prompt alongside today's baseline WorkoutDayPlan to the Google Gemini API.
- Use structured JSON output extraction (similar to GeminiNutritionParser) returning:
  ```json
  {
    "skippedExerciseId": "wide_grip_lat_pulldowns",
    "reason": "Equipment unavailable",
    "compensations": [
      {
        "exerciseId": "seated_cable_rows",
        "extraSets": 2,
        "repAdjustment": "8-12",
        "rationale": "Redistributes lat and upper-back volume so weekly V-taper growth is maintained."
      }
    ],
    "coachMessage": "Swapped out Lat Pulldowns. Added 2 sets to Seated Cable Rows to hit your back threshold!"
  }
  ```

#### R-REBALANCE-3: Deterministic Offline Rebalance Engine (OfflineWorkoutRebalancer)
- Implement a pure offline fallback when offline or when no Gemini API key is present:
  - Map by targetMuscle and category:
    - Back/Lats (e.g., Lat Pulldowns): Redistribute +2 sets to Seated Cable Rows or Doorway/Towel Rows.
    - Upper Chest/Shoulders (e.g., Incline DB Press): Redistribute +2 sets to Incline Push-ups.
    - Legs/Quads (e.g., Goblet Squats): Redistribute +2 sets to explosive Bodyweight Squats.
    - Hamstrings/Core (e.g., Lying Leg Curls / Plank): Redistribute volume to bodyweight hinges or extended plank intervals.

#### R-REBALANCE-4: Harmonization with CompensatoryEngine
- Ensure rebalancing smoothly layers with meal-based triggers:
  - High-Carb Trigger: Still adds glycogen-utilization sets where applicable.
  - Protein-Deficit Trigger: Strict safety ceiling (max 3 sets cap) must take precedence so rebalancing does not overtax under-recovered muscle tissue.

#### R-REBALANCE-5: Jetpack Compose UI
- Add an "AI Rebalanced" badge and expandable coach note explaining why volume was shifted.
- Allow 1-tap reset back to the default routine if the user changes their mind.

#### R-REBALANCE-6: Storage & Automated Testing
- Maintain all build caches, Gradle daemons, and code strictly on Drive D: (D:\projects\hostel_gains and D:\.gradle). Zero writes to Drive C:.
- Author unit tests (WorkoutRebalancerTest.kt) covering:
  - JSON response parsing & edge-case recovery.
  - Offline fallback matrix across all 6 PPL workout days.
  - Interaction between rebalanced volume and the low-protein 3-set cap.


## Follow-up — 2026-11-10T10:16:51Z

High-Priority Directive from User: Expedite Execution & Accelerate Milestones

Hello Teamwork Lead,

The user has explicitly requested to speed up the process and accelerate delivery. 

Please streamline execution across the team:
1. Fast-track the M2 gate closure and avoid redundant review cycles where tests are already passing.
2. Parallelize Milestone M3 (3D Kinematic Engine & Rebalancer) and Milestone M4 (Jetpack Compose UI) implementation where possible.
3. Keep gate evaluations focused, decisive, and lightweight so work moves swiftly toward the working Android build and interactive UI.
4. Continue maintaining strict Drive D: storage isolation (`D:\projects\hostel_gains` and `D:\.gradle`).

Please adjust team pacing and dispatch workers immediately to deliver the core application as rapidly as possible.

## Follow-up — 2026-11-10T11:35:00Z

where are we now? how much more time will it take? kill the subagents that are no longer needed

## Follow-up — 2026-11-10T12:05:44Z

Feature Request: AI Studio API Key Onboarding & "Premium AI" Unlock Flow

Please implement the following new feature into the Hostel-Gains Dynamic Optimizer app:

### Feature: API Key Onboarding Screen & Premium AI Feature Gate

#### R-APIKEY-1: First-Launch API Key Onboarding Screen (Jetpack Compose)
- On first app launch (or when no key is stored), show a dedicated onboarding/setup screen before the main app.
- The screen should:
  - Explain that the app works offline by default using the built-in Indian mess food database.
  - Invite the user to paste their personal Google AI Studio API key to unlock "Premium AI" features.
  - Include a tappable hyperlink: https://aistudio.google.com/app/apikey (opens in browser) so the user can easily get their free key.
  - Provide a visible password-style TextField (with show/hide toggle) to paste the key.
  - Include a "Unlock Premium AI" CTA button that validates the key format (should start with "AIza" and be non-empty) before saving.
  - Include a "Skip for now — use Offline Mode" button to dismiss and use the app without AI features.

#### R-APIKEY-2: Secure Local Storage of the API Key
- Store the API key using Android's EncryptedSharedPreferences (Jetpack Security library) so it is encrypted at rest on the device.
- Never log, transmit, or expose the key anywhere except in the Authorization header of outgoing Gemini REST API calls.
- Provide a "Clear API Key / Reset to Offline Mode" option in a Settings screen or accessible from the app menu.

#### R-APIKEY-3: Premium AI Feature Gate in UI
- Once a key is saved, the app should show a visible "Premium AI Active ✦" badge or indicator on the home/meal and workout screens.
- When no key is set, the AI-dependent buttons (e.g., "Analyze Meal with AI", "AI Workout Rebalance") should show a subtle "Offline Mode" label and still work via the offline fallback engines — not be disabled or hidden.
- When a key is set, those same buttons should show "AI Powered ✦" and route through the live Gemini API parsers.

#### R-APIKEY-4: Pass Key to Gemini Parsers at Runtime
- Update `GeminiNutritionParser` and `GeminiWorkoutRebalancer` to accept the key from EncryptedSharedPreferences at runtime (via the ViewModel or a KeyRepository) instead of only from `System.getenv("GEMINI_API_KEY")`.
- Maintain the existing graceful fallback: if the key is missing or the API call fails, automatically fall through to the offline engines.

#### R-APIKEY-5: Automated Tests
- Unit tests for `ApiKeyRepository` covering key save, retrieval, clear, and format validation.
- UI state tests verifying that the Premium AI badge is shown/hidden based on key presence.
- Tests verifying that `GeminiNutritionParser` and `GeminiWorkoutRebalancer` correctly receive the runtime-injected key.

#### R-APIKEY-6: Storage Constraint
- All code, Gradle builds, and caches must continue to reside strictly on Drive D: (`D:\projects\hostel_gains` and `D:\.gradle`). Zero writes to Drive C:.

Please implement and verify this feature, then rebuild the debug APK.
