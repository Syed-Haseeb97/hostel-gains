# Dispatch Messages

## 2026-11-10T01:44:54Z

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

## 2026-11-10T09:53:10Z

Sentinel Liveness Check: Please update progress.md and report current status of Milestone M2 Gate.


## 2026-11-10T09:59:42Z

User Directive: User asked to kill any subagents that are no longer needed, provide current status, and expedite delivery. Please terminate all retired/completed subagents in your swarm, address the M2 gate findings promptly, and advance to Milestone M3 (3D Kinematic Engine) and M4 (Compose UI).


## 2026-11-10T10:11:49Z

A new formal follow-up feature request has been received from the user and recorded in D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md:

Subject: Feature Request - Dynamic Exercise Rebalancer & Equipment Fallback Engine
Requirements R-REBALANCE-1 through R-REBALANCE-6:
- Natural Language & Quick Action Skip/Trigger for unavailable/painful exercises
- GeminiWorkoutRebalancer (AI structured JSON redistribution)
- OfflineWorkoutRebalancer (deterministic fallback matrix across all 6 PPL workout days)
- Harmonization with CompensatoryEngine (High-Carb trigger & strict Protein-Deficit 3-set cap precedence)
- Jetpack Compose UI ("AI Rebalanced" badge, coach explanation card, 1-tap reset)
- Comprehensive unit tests (WorkoutRebalancerTest.kt) and Drive D: storage isolation.

Please incorporate this feature into the project scope and milestone roadmap.


## 2026-11-10T10:17:02Z

HIGH-PRIORITY DIRECTIVE: Expedite Execution & Accelerate Milestones

The user has explicitly requested to speed up the process and accelerate delivery:
1. Fast-track M2 gate closure and avoid redundant review cycles where tests are already passing.
2. Parallelize Milestone M3 (3D Kinematic Engine & Rebalancer) and Milestone M4 (Jetpack Compose UI) implementation immediately.
3. Keep gate evaluations focused, decisive, and lightweight so work moves swiftly toward the working Android build and interactive UI.
4. Continue maintaining strict Drive D: storage isolation (D:\projects\hostel_gains and D:\.gradle).

Please adjust team pacing immediately and dispatch workers in parallel to complete M3 and M4 rapidly.
