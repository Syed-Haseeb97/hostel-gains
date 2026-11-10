# Dispatch: Worker M4 (Jetpack Compose UI & Screens)

## Objective
Implement Milestone M4:
1. Fitness design tokens & theme (`app/src/main/java/com/hostelgains/ui/theme/`): Volt `#c6ff3d`, Ember `#ff5a36`, Ink `#0b0b0d`, Panel `#151519`, Typography, Shapes.
2. Core Navigation & Scaffold with bottom navigation bar across 4 destinations.
3. 4 Core Screens:
   - `MealInputScreen`: Natural language text field, quick meal chips, daily macro totals card, 3D macro chart, active trigger status badges.
   - `WorkoutPlanScreen`: 7-day selector, dynamic adjustment banner, exercise cards with modified sets/reps, form notes, inline 3D mannequin preview, inline "Skip / Can't Do" action, "AI Rebalanced" badge, expandable coach rationale card, and 1-tap reset action.
   - `WorkoutViewerScreen`: Fullscreen interactive 3D exercise viewer with camera angle selector, touch orbit, speed controls, muscle activation highlights, and apparatus rendering.
   - `StepStatusScreen`: Circular progress step dial, 6:00 PM deficit warning card, and animated 3D walking avatar.
4. ViewModels: `MealLogViewModel`, `WorkoutViewModel`, `StepTrackerViewModel`.
5. Compose UI preview tests and verify build with `assembleDebug`:
   Produce clean debug APK on Drive D: (`app/build/outputs/apk/debug/app-debug.apk`).

## Mandatory Inputs to Read First
- `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (READ FIRST)
- `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`

## Write Ownership
You exclusively own:
- `app/src/main/java/com/hostelgains/ui/theme/`
- `app/src/main/java/com/hostelgains/ui/components/`
- `app/src/main/java/com/hostelgains/ui/screens/`
- `app/src/main/java/com/hostelgains/ui/viewmodel/`
- `app/src/main/res/`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/hostelgains/ui/`

## Verification Command on Drive D:
```powershell
$env:GRADLE_USER_HOME = "D:\.gradle"
$env:ANDROID_USER_HOME = "D:\.android"
$env:ANDROID_HOME = "D:\Android\sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\sdk"
Set-Location "D:\projects\hostel_gains"
.\gradlew.bat --gradle-user-home D:\.gradle assembleDebug testDebugUnitTest
```
Ensure build succeeds, APK is generated, and 0 bytes written to Drive C:.
Write `handoff.md` upon completion.


## 2026-11-10T10:19:30Z
[Message] timestamp=2026-11-10T10:19:30Z sender=369d8cfd-670f-4689-a71e-4ac9454c73da priority=MESSAGE_PRIORITY_HIGH content=DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

You are worker_m4 (implementation worker).
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\worker_m4

Task:
Implement Milestone M4: Jetpack Compose UI & Screens and assemble debug APK on Drive D:.

Mandatory Inputs to Read First:
1. `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (READ FIRST - including Follow-up feature request)
2. `D:\projects\hostel_gains\.agents\teamwork\PROJECT.md`
3. `D:\projects\hostel_gains\.agents\teamwork\worker_m4\DISPATCH.md`

Files You Own Exclusively:
- `app/src/main/java/com/hostelgains/ui/theme/`
- `app/src/main/java/com/hostelgains/ui/components/`
- `app/src/main/java/com/hostelgains/ui/screens/`
- `app/src/main/java/com/hostelgains/ui/viewmodel/`
- `app/src/main/res/`
- `app/src/main/AndroidManifest.xml`
- `app/src/test/java/com/hostelgains/ui/`
