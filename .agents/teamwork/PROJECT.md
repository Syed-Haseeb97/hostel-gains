# Project: The Hostel-Gains Dynamic Optimizer

## Architecture
A native Android mobile application built with Kotlin and Jetpack Compose adhering to Clean Architecture principles:
- **Presentation Layer**: Jetpack Compose UI with dark aesthetic theme (`Volt #c6ff3d`, `Ember #ff5a36`, `Ink #0b0b0d`, `Panel #151519`), StateFlow ViewModels (`MealLogViewModel`, `WorkoutViewModel`, `StepTrackerViewModel`, `WorkoutViewerViewModel`), and 4 core navigation destinations:
  1. Meal Input & Macro Summary (`MealInputScreen`)
  2. Dynamic Workout Plan (`WorkoutPlanScreen` with inline Skip actions, "AI Rebalanced" badge, coach explanation card, 1-tap reset)
  3. 3D Workout Viewer (`WorkoutViewerScreen` with 360° touch orbit drag, speed controls, camera presets)
  4. Step Status & Alert (`StepStatusScreen` with circular step dial, 6 PM alert card, 3D walking avatar)
- **3D Biomechanical Visual Engine**:
  - Native Kotlin Jetpack Compose 3D Canvas Mesh Engine: 4×4 affine matrix projection, forward kinematics solver (`solve(p, mv)`), depth-sorted polygon shading (Painter's algorithm), interactive touch drag orbiting, and 28 exercise routines ported from `moves.ts` and `Mannequin.tsx`.
  - Bundled offline WebView 3D fallback in `app/src/main/assets/3d_viewer/` for 1:1 parity with web assets.
- **Domain Layer**:
  - `NutritionParser` interface with dual implementations:
    - `GeminiNutritionParser`: Live Google Gemini REST client for natural language meal parsing.
    - `FixtureNutritionParser`: Deterministic offline lookup + heuristic tokenizer for Indian mess items (mutton, dry rotis, eggs, dal, rice, paneer, etc.) enabling 100% reliable offline/CI unit tests.
  - V-Taper PPL 7-Day Framework: Complete exercise repository for Days 1 to 7 (Pull A, Push A, Legs/Core, Pull B, Push B, Weak-Point Pump, Active Rest) with exact sets, reps, and form guidance.
  - `CompensatoryEngine`: Dynamic workout modifier:
    - Carb Trigger: When meal carbs >= 60g, adds +1 set to Lat Pulldowns and Squats.
    - Protein Deficit Trigger: When meal protein < 20g, caps all exercises at max 3 sets.
    - Precedence: Protein Deficit Cap acts as an absolute physiological safety ceiling (`min(3, baseline + carbAdjustment)`).
  - `WorkoutRebalancer`: Dynamic Exercise Rebalancer & Equipment Fallback Subsystem:
    - Allows user to report skipped/unavailable/painful exercises via natural language or inline quick action.
    - Dual implementations: `GeminiWorkoutRebalancer` (AI structured JSON redistribution) and `OfflineWorkoutRebalancer` (deterministic synergistic fallback matrix across all 6 PPL training days).
    - Harmonization: Layers with `CompensatoryEngine` where High-Carb adds sets, and Protein Deficit 3-set cap strictly caps rebalanced sets as an absolute ceiling.
  - `StepAlertEvaluator`: Triggers evening alert when daily steps < 5,000 at or after 6:00 PM recommending a 10,000-step walk.
- **Data & Storage Layer**:
  - In-memory & Room local repositories (`MealRepository`, `WorkoutRepository`, `StepRepository`).
  - Strict Drive D: storage isolation:
    - `GRADLE_USER_HOME = D:\.gradle`
    - `ANDROID_USER_HOME = D:\.android`
    - `ANDROID_HOME = D:\Android\sdk`
    - `java.io.tmpdir = D:\temp`
    - Zero bytes written to Drive C:.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| F1 | Strict Drive D: Build Setup | Gradle 9.6.0 + AGP 9.4.0 + Kotlin 2.4.10 project configured strictly on Drive D: with zero Drive C: writes | M1 | ORIGINAL_REQUEST §R4, Explorer 1 |
| F2 | Core Domain Models | Data classes for meals, food items, macro nutrients, exercises, workout days, and step status | M1 | ORIGINAL_REQUEST §R2, R3, Explorer 3 |
| F3 | V-Taper PPL Framework Database | Full 7-day model (Days 1 to 7) with exercise models, target sets, rep ranges, rest timers, and form cues | M1 | ORIGINAL_REQUEST §R3, Explorer 3 |
| F4 | Dynamic Compensatory Workout Engine | Dynamic set modifications: Carb trigger (+1 set to Lat Pulldowns & Squats), Protein deficit trigger (cap at max 3 sets), and safety precedence rule | M1 | ORIGINAL_REQUEST §R3, Explorer 3 |
| F5 | Step Counter Sync & 6 PM Alert Engine | Step monitoring and evening alert trigger when steps < 5,000 at/after 6:00 PM recommending 10,000 steps | M1 | ORIGINAL_REQUEST §R3, Explorer 3 |
| F6 | AI Nutrition Parsing Interface & Offline Fixtures | `NutritionParser` contract with `FixtureNutritionParser` handling Indian mess meals (mutton, rotis, eggs, dal, rice) deterministically offline | M2 | ORIGINAL_REQUEST §R2, Explorer 3 |
| F7 | Live Gemini Nutrition Parser | `GeminiNutritionParser` using OkHttp REST client to parse natural language meal descriptions via Gemini API | M2 | ORIGINAL_REQUEST §R2, Explorer 3 |
| F8 | Daily Macronutrient Aggregator & Repository | Aggregates daily protein, carbs, fats, calories, and tracks cumulative trigger states | M2 | ORIGINAL_REQUEST §R2, Explorer 3 |
| F9 | Biomechanical Forward Kinematics Solver | Kotlin port of `moves.ts` 28 exercise routines, joint angle poses, and `solve(p, mv)` kinematics calculations | M3 | ORIGINAL_REQUEST §R1, Explorer 2 |
| F10 | Native Compose 3D Canvas Mesh Engine | Procedural 3D box mannequin renderer with 4x4 matrix projection, depth sorting, touch orbit, and muscle highlighting | M3 | ORIGINAL_REQUEST §R1, Explorer 2 |
| F11 | Bundled 3D Web Assets & WebView Fallback | Extracted assets in `app/src/main/assets/3d_viewer/` providing offline HTML5/CSS 3D fallback viewer | M3 | ORIGINAL_REQUEST §R1, Explorer 2 |
| F12 | 3D Hero Barbell & Isometric Macro Charts | 3D visual components for interactive barbell and isometric macro nutrient breakdown | M3 | ORIGINAL_REQUEST §R1, Explorer 2 |
| F13 | UI Theme & Navigation Architecture | Dark fitness design system (`Volt`, `Ember`, `Ink`, `Panel`) and bottom navigation scaffold | M4 | ORIGINAL_REQUEST §R1, Explorer 2 |
| F14 | Meal Input & Macro Summary Screen | Meal input screen with natural language text bar, preset chips, macro summary card, 3D macro bars, and trigger badges | M4 | ORIGINAL_REQUEST §R2, Explorer 2 |
| F15 | Dynamic Workout Plan Screen | 7-day selector, dynamic adjustment banner, exercise list with modified sets/reps, form notes, and 3D preview | M4 | ORIGINAL_REQUEST §R3, Explorer 2 |
| F16 | 3D Workout Viewer Screen | Fullscreen interactive 3D exercise viewer with camera presets, touch orbit, and playback controls | M4 | ORIGINAL_REQUEST §R1, Explorer 2 |
| F17 | Step Status & Evening Alert Screen | Circular step dial, 6:00 PM deficit warning card, and animated 3D walking avatar | M4 | ORIGINAL_REQUEST §R3, Explorer 2 |
| F18 | Full Application Build & Packaging | Clean debug APK build on Drive D: verifying end-to-end integration (`app-debug.apk` 19.5 MB) | M4 | ORIGINAL_REQUEST §R1, R4 |
| F19 | E2E Test Suite Pass (Tiers 1-4) | 100% pass rate on opaque-box requirement-driven E2E test suite published in TEST_READY.md (61 tests) | M5 | Acceptance Criteria |
| F20 | Adversarial Coverage Hardening (Tier 5) | White-box adversarial testing, edge-case hardening, and zero remaining gaps verification (59 adversarial tests) | M5 | Dual Track Specification |
| F21 | Offline Dynamic Exercise Rebalancer | Deterministic synergistic redistribution matrix across all 6 PPL days with CompensatoryEngine precedence | M3 | ORIGINAL_REQUEST §R-REBALANCE-1,3,4,6 |
| F22 | Gemini AI Workout Rebalancer | Google Gemini API integration for natural language skipped/unavailable exercise volume redistribution | M3 | ORIGINAL_REQUEST §R-REBALANCE-2 |
| F23 | Rebalanced Workout UI & Coach Card | Compose UI components: "AI Rebalanced" badge, expandable coach rationale card, 1-tap reset action | M4 | ORIGINAL_REQUEST §R-REBALANCE-5 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Domain & Build Infra | F1, F2, F3, F4, F5: Gradle project on Drive D:, domain models, V-Taper PPL repository, CompensatoryEngine, StepAlertEvaluator, and M1 unit tests | none | DONE |
| M2 | AI Nutrition & Macro Logging | F6, F7, F8: NutritionParser, FixtureNutritionParser, GeminiNutritionParser, DailyMacroSummary repository, and M2 unit tests | M1 | DONE |
| M3 | 3D Biomechanical Engine & Dynamic Exercise Rebalancer | F9, F10, F11, F12, F21, F22: Kinematics solver, Native Compose 3D Canvas mesh engine, assets bundling, 3D barbell hero, 3D macro charts, OfflineWorkoutRebalancer, and GeminiWorkoutRebalancer | M1 | DONE |
| M4 | Jetpack Compose UI & Screens | F13, F14, F15, F16, F17, F18, F23: Design system, 4 core screens with AI Rebalanced badge & coach card, ViewModels, navigation, and APK assembly (`app-debug.apk` 19.5 MB) | M2, M3 | DONE |
| M5 | Final E2E Verification & Hardening | F19, F20: 100% E2E test suite pass (Tiers 1-4) + Tier 5 adversarial coverage hardening (264/264 tests passing) | M4, TEST_READY | DONE |

## Code Layout
```
D:\projects\hostel_gains\
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── local.properties
├── gradle\
│   └── wrapper\
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
└── app\
    ├── build.gradle.kts
    ├── src\
    │   ├── main\
    │   │   ├── AndroidManifest.xml
    │   │   ├── assets\
    │   │   │   └── 3d_viewer\
    │   │   ├── java\
    │   │   │   └── com\hostelgains\
    │   │   │       ├── domain\
    │   │   │       │   ├── model\
    │   │   │       │   ├── nutrition\
    │   │   │       │   ├── workout\
    │   │   │       │   ├── rebalance\
    │   │   │       │   └── steps\
    │   │   │       ├── data\
    │   │   │       │   ├── repository\
    │   │   │       │   └── local\
    │   │   │       └── ui\
    │   │   │           ├── theme\
    │   │   │           ├── components\
    │   │   │           ├── kinematics\
    │   │   │           ├── viewmodel\
    │   │   │           └── screens\
    │   │   └── res\
    │   │       └── values\
    │   └── test\
    │       └── java\com\hostelgains\
    │           ├── domain\
    │           ├── data\
    │           ├── ui\
    │           └── e2e\
```
