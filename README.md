# ⚡ The "Hostel-Gains" Dynamic Optimizer

> **Most fitness apps are useless when you live in a university hostel and can't control the kitchen. This one actually understands your constraints.**

[![Android](https://img.shields.io/badge/Platform-Android_API_26+-22C55E?style=for-the-badge&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_BOM-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Tests](https://img.shields.io/badge/Unit_Tests-283_Passing_(100%25)-C6FF3D?style=for-the-badge&logo=checkmarx&logoColor=black)](#automated-testing)
[![License](https://img.shields.io/badge/License-MIT-FF5A36?style=for-the-badge)](#license)

---

## 🎯 The Problem & The Solution

University hostel mess food is unpredictable:
- One day you get **mutton curry with 4 dry rotis** (carb surplus).
- The next day it's **watery dal and white rice** (severe protein deficit).
- Gym equipment is often occupied, missing, or broken.

**The Hostel-Gains Dynamic Optimizer** calculates your exact macronutrients from natural language mess entries and **dynamically adjusts your evening training and daily step targets to compensate**.

---

## 🚀 Key Features

### 1. 🍽️ Mess Menu AI & Offline Nutrition Engine
- **Natural Language Parsing:** Input meals naturally like *"mutton curry, 4 dry rotis, 2 boiled eggs"* or *"dal and rice"*.
- **Live Google Gemini REST API Client:** Zero-dependency pure-Kotlin recursive-descent JSON parser extracting grams of protein, carbs, fats, and total calories.
- **Offline Indian Food Database:** Built-in dietary composition fixtures (calibrated from National Institute of Nutrition & USDA) covering rotis, dal, mutton, eggs, rice, paneer, curd, and oats.
- **API Key Onboarding & Live Validation:** Secure onboarding sheet with live network verification against Google AI Studio and AES-256 GCM encrypted storage via Jetpack `security-crypto`.

### 2. ⚡ The V-Taper PPL Framework (6 Days On, 1 Day Off)
A specialized routine designed for aesthetics and posture:

| Day | Focus | Primary Target | Key Exercises |
|:---:|:---:|:---:|:---|
| **Day 1** | **Pull A** | Lat Width & Rear Delts | Wide-Grip Lat Pulldowns, Seated Cable Rows, Rear Delt Flyes, DB Hammer Curls |
| **Day 2** | **Push A** | Lateral Delts & Upper Chest | Incline DB Press, Lateral Raises, Incline Push-ups to failure, Tricep Rope Pushdowns |
| **Day 3** | **Legs / Core** | Football Athletic Maintenance | Goblet Squats, Explosive Bodyweight Squats, Lying Leg Curls, Plank |
| **Day 4** | **Pull B** | Lat Thickness & Biceps | Horizontal cable & dumbbell pulls, barbell biceps curls |
| **Day 5** | **Push B** | Cable Delts & Triceps | Cable lateral raises, overhead tricep extensions, dips |
| **Day 6** | **Weak-Point Pump** | V-Taper Volume Shock | FST-7 Lat Pulldowns (7 sets × 14 reps, 45s rest), Cable Lateral Raises, Face Pulls |
| **Day 7** | **Active Rest** | Recovery & Caloric Baseline | 5,000 – 10,000 steps only |

### 3. 🧠 Dynamic Compensatory Engine
The app's brain recalibrates your workout based on logged nutrition and activity:
- 🍞 **The High-Carb Trigger:** If you eat extra rotis or rice, the app registers a high-carb state and dynamically adds **+1 extra set** to your Lat Pulldowns and Squats that evening to utilize glycogen stores.
- 🥗 **The Protein Deficit Trigger:** If mess food is mostly vegetarian/low-protein (e.g. dal & rice), the app enters a **"Low Recovery"** state, capping all exercises at **3 sets maximum** to prevent muscle breakdown.
- 👟 **The 6:00 PM Step Sync:** Evaluates daily steps at 6:00 PM; if you are under 5,000 steps, it alerts you to hit 10,000 steps after dinner to protect fat recomposition.

### 4. 🔄 Dynamic Exercise Rebalancer (Equipment Fallback)
- **1-Tap Inline Skip or Conversational Prompt:** Report broken equipment or joint pain (*"Lat pulldown busy, only dumbbells available"*).
- **Muscle-Synergy Redistribution:** Automatically shifts lost volume into matching synergists (e.g., Incline Press → Incline Push-ups; Lat Pulldowns → Cable Rows / Towel Rows; Squats → Explosive Bodyweight Squats).
- **Recovery Ceiling Precedence:** The 3-set protein deficit cap strictly overrides rebalanced additions, preventing overtraining.

### 5. 🕺 3D Kinematics Mesh Engine
- **Native Compose Canvas 3D Engine:** Forward kinematics solver with $4 \times 4$ affine matrices and Painter's depth-sorting rendered in pure Jetpack Compose.
- **28 Animated Exercise Routines:** Full skeletal joint angle keyframes ported from biomechanical movement assets.
- **Interactive Views:** Touch-orbiting 3D mannequin, spinning 3D hero barbell, and 3D step progress avatar.

### 6. 🎨 Athletic Design System & Brand Identity
- High-contrast athletic dark aesthetic (**Volt `#C6FF3D`**, **Ember `#FF5A36`**, **Ink `#0B0B0D`**).
- Full Android adaptive icon suite (vector drawables + multi-density mipmaps).

---

## 📂 Project Architecture

```
app/src/main/java/com/hostelgains/
├── data/
│   ├── ApiKeyRepository.kt         # AES-256 EncryptedSharedPreferences key storage
│   ├── ApiKeyValidator.kt          # Live Google AI Studio network validation
│   └── nutrition/
│       └── MealRepository.kt       # Multi-meal daily macronutrient state
├── domain/
│   ├── nutrition/
│   │   ├── GeminiNutritionParser.kt    # Live Google Gemini REST API client
│   │   ├── FixtureNutritionParser.kt   # Offline Indian mess food tokenizer (NIN/USDA)
│   │   └── NutritionModels.kt          # MealNutrition & DailyMacroSummary
│   ├── workout/
│   │   ├── VTaperWorkoutRepository.kt  # Full 7-day V-Taper PPL database
│   │   ├── CompensatoryEngine.kt       # Carb, protein, and recovery triggers
│   │   └── WorkoutModels.kt            # Exercise, WorkoutDayPlan, AdjustedPlan
│   ├── rebalance/
│   │   ├── GeminiWorkoutRebalancer.kt  # AI conversational exercise rebalancing
│   │   ├── OfflineWorkoutRebalancer.kt # Muscle-synergy volume redistribution matrix
│   │   └── RebalanceModels.kt          # RebalanceResult & CompensationItem
│   └── steps/
│       └── StepAlertEvaluator.kt       # 6:00 PM deficit detection & alerts
└── ui/
    ├── MainActivity.kt             # Edge-to-edge Compose entry point
    ├── HostelGainsApp.kt           # Navigation scaffolding & onboarding gate
    ├── screens/
    │   ├── MealInputScreen.kt          # Natural language meal tracker & 3D macro chart
    │   ├── WorkoutPlanScreen.kt        # Dynamic V-taper plan with rebalancer UI
    │   ├── WorkoutViewerScreen.kt      # Interactive 3D Canvas kinematic mannequin
    │   ├── StepStatusScreen.kt         # Step tracker with 3D walking avatar
    │   └── ApiKeyOnboardingScreen.kt   # First-launch API key setup & verification
    ├── components/
    │   ├── HostelGainsLogo.kt          # Animated brand logo & header badges
    │   ├── AiModeBadge.kt              # "Premium AI Active ✦" / "Offline Mode" pills
    │   ├── Macro3DChart.kt             # 3D nutrition breakdown chart
    │   └── TriggerBadges.kt            # High-Carb & Low-Protein status badges
    ├── kinematics/
    │   └── BiomechanicalKinematicsSolver.kt # 3D skeletal projection & matrix math
    ├── theme/                          # Volt, Ember, Ink design tokens & typography
    └── viewmodel/                      # Reactive MVI / MVVM ViewModels
```

---

## 🧪 Automated Testing

The codebase includes an extensive 4-tier automated test suite:
- **Tier 1:** Core feature coverage (meal parsing, 7-day V-Taper routines, triggers).
- **Tier 2:** Boundary & corner cases (empty strings, zero steps, exact 18:00 cutoff).
- **Tier 3:** Conflicting state combinations (high carb + low protein simultaneous triggers).
- **Tier 4:** Real-world university hostel mess dietary fixtures.
- **Domain & UI:** 3D kinematics solver, ViewModel state flows, AES-256 key encryption, live Google error recovery.

Run the full test suite:
```powershell
.\gradlew.bat testDebugUnitTest --rerun-tasks
```

```text
BUILD SUCCESSFUL
283 tests passed, 0 failures, 0 errors, 0 skipped (100% Green)
```

---

## 📦 Building & Installing the App

### Prerequisites
- JDK 17+ or JDK 21
- Android SDK (API 35, Build-Tools 35.0.0)

### Compile Debug APK
```powershell
.\gradlew.bat assembleDebug
```
The compiled APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install onto Device
Enable **USB Debugging** on your Android phone and run:
```powershell
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
```

---

## 🔒 Security & Privacy
- **Zero Cloud Leakage:** Meal logs and workout histories are stored 100% locally on your device.
- **Keystore Encryption:** Google AI Studio API keys are encrypted at rest via `EncryptedSharedPreferences` using Android Keystore AES-256 GCM.
- **Offline Resilient:** Operates entirely without an API key or internet connection using built-in dietary heuristics and exercise rebalancing matrices.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
