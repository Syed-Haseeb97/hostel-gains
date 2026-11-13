# ⚡ The "Hostel-Gains" Dynamic Optimizer

> **Most fitness apps are useless when you live in a university hostel and can't control the kitchen. This one actually understands your constraints.**

[![Platform: Android](https://img.shields.io/badge/Platform-Android_API_26+-22C55E?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_BOM-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Database: Room](https://img.shields.io/badge/Database-Room_SQLite-FF6F00?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Tests: 315 Passing](https://img.shields.io/badge/Unit_Tests-315_Passing_(100%25)-C6FF3D?style=for-the-badge&logo=checkmarx&logoColor=black)](#automated-testing)
[![Latest Release](https://img.shields.io/badge/Release-v1.2.1-FF5A36?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Syed-Haseeb97/hostel-gains/releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-38BDF8?style=for-the-badge)](#license)

---

## 📲 Quick Download
- 📥 **Download APK Directly**: [**hostel-gains-v1.2.1.apk** (GitHub Releases)](https://github.com/Syed-Haseeb97/hostel-gains/releases/download/v1.2.1/hostel-gains-v1.2.1.apk)
- 🔄 **In-App Auto-Updates**: The app checks GitHub Releases in real-time on every launch and downloads/installs newer versions seamlessly in the background.

---

## 🎯 The Problem & The Solution

University hostel food and campus gym setups are notoriously unpredictable:
- One day the mess serves **mutton curry with 4 dry rotis** (heavy carb surplus).
- The next day it's **watery dal and white rice** (severe protein deficit).
- Campus gym equipment is frequently broken, occupied, or missing.
- Energy and recovery fluctuate wildly depending on exams, sleep, and nutrition.

**The Hostel-Gains Dynamic Optimizer** calculates your exact macronutrients from natural language mess entries and **dynamically adapts your workout volume, rep ranges, equipment substitutions, and daily step targets in real time**.

---

## 🚀 Key Features

### 1. 🧬 Multi-Path Physique Engine (7 Distinct Archetypes)
Instead of forcing everyone into a static workout plan, users select their desired target physique build alongside their height, weight, and age:

| Physique Archetype | Primary Focus | Rep Range | Split Strategy |
|:---|:---|:---:|:---|
| **The V-Taper** *(Aesthetic Focus)* | Shoulder width (lateral delts) & flaring lats tapering to a narrow waist | 8–12 reps | Lat Pulldowns, Lateral Raises, Incline Press, Rear Delts |
| **The Tank** *(Raw Strength)* | Dense, thick muscle mass and raw CNS pulling/squatting power | 4–6 reps | Heavy Rows, Squats, Deadlifts, Overhead Press |
| **The Athlete** *(Functional & Lean)* | Hypertrophy blended with stamina, core stability, and agility | 12–15 reps | Unilateral movements, plyometrics, explosive pushups |
| **The X-Frame** *(Proportional Sweep)* | Upper-body V-taper balanced with outer quad sweep and calves | 8–12 reps | Squats, leg extensions, wide pulldowns, heavy delts |
| **The Yoke** *(Bear Mode Density)* | Thick traps, heavy upper back, 3D side profile at 15–18% body fat | 6–10 reps | Shrugs, rack pulls, heavy barbell rows, farmers carries |
| **The Golden Era** *(1970s Classic)* | Proportions, vacuum waist, and shelf-like upper chest development | 8–12 reps | Incline DB Press, Pec Deck, Preacher Curls, Dips |
| **The Sleeper Build** *(Ultra-Lean Stealth)* | Dense muscle separation with low body fat (10–12%) and high relative strength | 10–15 reps | Pullups, bodyweight dips, lateral raises, abdominal crunches |

---

### 2. ⚡ Lifting Experience Level & Live Gemini 1.5 Flash Routine Generation
Users choose their experience level during onboarding and profile setup:
- **Beginner (0–1 yrs)**: Focus on form mastery, joint stability, and controlled reps (10–15 reps) with 3 working sets and 90–120s recovery.
- **Intermediate (1–3 yrs)**: Progressive overload and mechanical tension (8–12 reps), 3–4 working sets, 60–90s recovery.
- **Advanced / Pro (3+ yrs)**: High density, heavy CNS compound sets (4–8 reps) paired with failure sets (12–15 reps), pause reps, dead-stop rows, and weak-point specialization.

**Live Gemini Periodization**: Tapping *"Calibrate with Gemini AI"* directly sends your build, level, and anthropometrics to Google Gemini 1.5 Flash REST API to synthesize a 100% custom 7-day Push/Pull/Legs periodized routine with zero static offline fixtures faking the schedule.

---

### 3. 🍽️ Mess Menu AI & Room Database Persistence
- **Natural Language Parsing**: Input meals naturally like *"4 boiled eggs and 2 bananas"*, *"dal, rice, and 3 rotis"*, or *"mutton curry, 1 bowl"*.
- **Google Gemini 1.5 Flash REST Client**: Real-time structured JSON extraction with grams of protein, carbs, fats, and calories.
- **Local Indian Mess Dataset (NIN/USDA)**: Instant offline fallback when network is absent or no API key is supplied.
- **Room Database Architecture (`hostelgains.db`)**:
  - `MealEntity` table with indexed `dateIso` timestamps.
  - Asynchronous ordered write queue (`Channel<suspend () -> Unit>`) preventing UI thread locking.
  - High-performance in-memory `StateFlow` cache hydrated on app start.
  - `DailyMacroSummary.calculate(date, meals)` recomputed dynamically without duplicated data.

---

### 3. 🧠 Dynamic Nutrition-Driven Workout Rebalancer
The app's compensatory engine automatically re-tunes your daily workout based on logged nutrition:
- 🍞 **The High-Carb Trigger (≥ 60g Carbs)**: Adds **+1 extra set** to prime movers (e.g., Lat Pulldowns, Squats) to take advantage of glycogen supercompensation.
- 🥗 **The Protein Deficit Trigger (< 20g Protein)**: Imposes a strict **3-set recovery ceiling** across all exercises to prevent catabolic overtraining on low-nutrition mess days.
- 🔄 **Equipment & Injury Fallbacks**: 
  - 1-tap inline skip or conversational skip prompts (*"Wrist hurts, had to skip Incline DB press"*).
  - Gemini AI analyzes anatomical synergy and transfers lost volume into matching movements (e.g. Incline Press → Incline Push-ups; Lat Pulldowns → Cable Rows).
  - Protein-deficit safety ceilings strictly supersede rebalanced set bonuses.

---

### 4. 👟 Hardware Step Counter Sensor (`Sensor.TYPE_STEP_COUNTER`)
- **Direct Hardware Sensor Integration**: Listens to Android `Sensor.TYPE_STEP_COUNTER` via Kotlin coroutine `callbackFlow`.
- **Jetpack DataStore Persistence**: Tracks daily step deltas, midnight date rollovers, and device reboots (handles hardware step counter resetting to zero).
- **WorkManager Background Sync**: `StepSyncWorker` runs a 15-minute periodic job keeping steps updated even when the app is closed.
- **6:00 PM Deficit Alert**: If total steps are under 5,000 at 6:00 PM, triggers an alert recommending a post-dinner campus walk to protect fat recomposition.
- **Live Status Indicator**: Green `LIVE SENSOR` status pill displayed in the Move tab.

---

### 5. ⏰ Scheduled Mess & Gym Motivation Alarms (5 Time Slots)
`MealReminderScheduler` uses Android `AlarmManager` with exact/inexact timing and auto-restores on device reboot via `BootReceiver`:
- **9:00 AM**: Breakfast Reminder (*"Done with breakfast? Enter what you ate to keep a record!"*)
- **1:45 PM**: Lunch Reminder (*"Finished lunch? Log your mess meal to stay on track!"*)
- **5:40 PM**: Snacks Reminder (*"Evening snacks done? Keep your macros dialed in!"*)
- **7:45 PM**: Gym Motivation Alert (*"She left you, now make her regret it. Hit the gym!"*)
- **8:45 PM**: Dinner Reminder (*"Done with mess dinner? Log your final meal of the day!"*)

---

### 6. 🕺 Native 3D Kinematics Mesh Engine
- Forward kinematics solver with $4 \times 4$ affine transformations and Painter's algorithm depth-sorting built directly in Jetpack Compose Canvas.
- **28 Animated Exercise Routines**: Real-time skeletal joint rotations for push, pull, and leg movements.
- Orbiting camera controls (Front, Side, $45^\circ$ Three-Quarter presets) and interactive angle sliders.

---

### 7. 🔄 Real-Time In-App GitHub Auto-Updater
- Queries the GitHub Releases API (`Syed-Haseeb97/hostel-gains/releases/latest`) on every application launch.
- Automatically compares semantic version strings (`1.0.0` < `1.1.0`).
- Provides in-app non-intrusive download banners with live byte progress bars.
- Triggers standard Android package installer via secure `FileProvider`.

---

## 📂 Project Architecture

```
app/src/main/java/com/hostelgains/
├── data/
│   ├── ApiKeyRepository.kt             # AES-256 EncryptedSharedPreferences key storage
│   ├── ApiKeyValidator.kt              # Live Google AI Studio network verification
│   ├── database/
│   │   ├── AppDatabase.java            # Room Database singleton (hostelgains.db)
│   │   ├── MealDao.java                # Synchronous Room DAO operations
│   │   ├── MealEntity.java             # Persisted Room meal records
│   │   └── MealEntityExtensions.kt     # Domain <-> Entity mappers
│   ├── notifications/
│   │   ├── BootReceiver.kt             # Restores alarms on BOOT_COMPLETED
│   │   ├── MealReminderReceiver.kt     # Dispatches heads-up notifications
│   │   ├── MealReminderScheduler.kt    # Schedules 5 daily reminder slots
│   │   └── MealReminderSlot.kt         # Breakfast, Lunch, Snacks, Gym, Dinner
│   ├── nutrition/
│   │   └── MealRepository.kt           # Room write-through queue + StateFlow cache
│   ├── profile/
│   │   └── UserProfileRepository.kt    # Anthropometrics, height/weight & build prefs
│   ├── steps/
│   │   ├── StepRepository.kt           # DataStore step counter with reboot handling
│   │   ├── StepSensorSource.kt         # Sensor.TYPE_STEP_COUNTER callback flow
│   │   └── StepSyncWorker.kt           # 15-minute WorkManager background sync
│   └── update/
│       └── AppUpdateService.kt         # GitHub Releases API checker & APK downloader
├── domain/
│   ├── nutrition/
│   │   ├── GeminiNutritionParser.kt    # Live Google Gemini 1.5 Flash client
│   │   ├── FixtureNutritionParser.kt   # Offline Indian mess database (NIN/USDA)
│   │   └── NutritionModels.kt          # MealNutrition & DailyMacroSummary
│   ├── physique/
│   │   ├── PhysiqueBuild.kt            # 7 aesthetic archetypes & target parameters
│   │   ├── UserProfile.kt              # Anthropometry data models & BMI metrics
│   │   └── GeminiPhysiqueOptimizer.kt  # AI anthropometric calibration
│   ├── rebalance/
│   │   ├── GeminiWorkoutRebalancer.kt  # Conversational AI volume rebalancing
│   │   ├── OfflineWorkoutRebalancer.kt # Muscle-synergy volume fallback matrix
│   │   └── RebalanceModels.kt          # RebalanceResult & ExerciseCompensation
│   ├── steps/
│   │   ├── StepAlertEvaluator.kt       # 6:00 PM deficit detection logic
│   │   └── StepModels.kt               # StepAlertResult models
│   └── workout/
│       ├── DynamicWorkoutRepository.kt # 6-day PPL schedule for all 7 builds
│       ├── CompensatoryEngine.kt       # Carb surplus & protein deficit rules
│       └── WorkoutModels.kt            # Exercise, WorkoutDayPlan, AdjustedPlan
└── ui/
    ├── MainActivity.kt                 # Edge-to-edge entry point & permission handlers
    ├── HostelGainsApp.kt               # Tab navigation, onboarding & update dialogs
    ├── screens/
    │   ├── MealInputScreen.kt          # Meal tracker, quick mess presets & macro cards
    │   ├── WorkoutPlanScreen.kt        # Daily workout feed with rebalance cards
    │   ├── PhysiqueSelectionScreen.kt  # Interactive build selector & anthropometry
    │   ├── WorkoutViewerScreen.kt      # Interactive 3D Canvas kinematic mannequin
    │   ├── StepStatusScreen.kt         # Real hardware step counter & deficit ring
    │   └── ApiKeyOnboardingScreen.kt   # First-launch API key setup & verification
    ├── components/                     # Logo, buttons, cards, pills, update banners
    ├── three/                          # 3D Math, forward kinematics, camera & scenes
    ├── theme/                          # Volt, Ember, Ignite athletic theme tokens
    └── viewmodel/                      # Reactive MVI / MVVM ViewModels
```

---

## 🧪 Automated Testing

The project maintains a comprehensive, **100% green test suite of 301 automated unit tests** covering:
- **Room Database Persistence**: Hydration, ordered channel queues, entity-domain mapping, and zero data loss.
- **Hardware Step Counter**: Midnight rollover, device reboot baseline compensation, and 6:00 PM deficit alerts.
- **Nutrition Parsing**: Gemini JSON decoding, markdown fence stripping, and offline fallback accuracy.
- **Compensatory Engine**: Glycogen set additions, protein-deficit caps, and precedence harmonization.
- **Physique Generation**: 6-day PPL volume swaps across all 7 body builds.
- **Security & Validation**: AES-256 encrypted storage, non-empty validation, and error recovery.

To run the entire test suite locally:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

```text
BUILD SUCCESSFUL in 1m 31s
301 tests completed, 0 failed, 0 skipped (100% Passing)
```

---

## 🛠️ Building & Deploying

### Prerequisites
- **JDK 17+** or **JDK 21**
- **Android SDK Platform API 35** (Compile SDK: 37)
- **Android Gradle Plugin (AGP) 9.4.0**
- **Gradle 9.6.0**

### Compile APK
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```
The compiled APK will be created at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
```powershell
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
```

---

## 🔒 Security & Privacy

- **Local-First Storage**: All logged meals, step records, and workout completions remain 100% on-device in SQLite (`hostelgains.db`) and DataStore.
- **Keystore Encryption**: Google AI Studio API keys are encrypted at rest with AES-256 GCM using the hardware-backed Android Keystore.
- **Offline Reliability**: The app operates with complete fidelity without an API key or internet access using built-in sports science formulas and local nutritional databases.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
