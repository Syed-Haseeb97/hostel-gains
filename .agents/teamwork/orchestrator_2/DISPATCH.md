# Dispatch Log

## 2026-11-10T12:06:34Z
Sender: 7b2a5592-0f9d-474c-851b-2312da986849
Recipient: orchestrator_2
Content:
You are the Project Orchestrator for "The Hostel-Gains Dynamic Optimizer".
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\orchestrator_2
The project workspace is: D:\projects\hostel_gains
The authoritative request is in: D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md
The architecture and master project scope document is in: D:\projects\hostel_gains\.agents\teamwork\PROJECT.md

A new follow-up feature request has been submitted:
"Feature Request: AI Studio API Key Onboarding & 'Premium AI' Unlock Flow"

Requirements:
- R-APIKEY-1: First-Launch API Key Onboarding Screen (Jetpack Compose): dedicated onboarding/setup screen before main app when no key is stored. Explains offline mode by default, invites user to paste personal Google AI Studio key, includes hyperlink https://aistudio.google.com/app/apikey (opens in browser), password-style TextField with show/hide toggle, "Unlock Premium AI" button validating format (starts with "AIza" and non-empty), and "Skip for now — use Offline Mode" dismiss button.
- R-APIKEY-2: Secure Local Storage of the API Key: store using Android's EncryptedSharedPreferences (Jetpack Security library) encrypted at rest. Never log or leak the key except in Authorization header of Gemini REST API calls. Provide "Clear API Key / Reset to Offline Mode" option in Settings or app menu.
- R-APIKEY-3: Premium AI Feature Gate in UI: show visible "Premium AI Active ✦" badge on home/meal and workout screens when key is set. When no key is set, show subtle "Offline Mode" label on AI buttons (e.g. "Analyze Meal with AI", "AI Workout Rebalance") and continue routing through offline fallback engines without disabling them. When key is set, show "AI Powered ✦" and route through live Gemini API parsers.
- R-APIKEY-4: Pass Key to Gemini Parsers at Runtime: update GeminiNutritionParser and GeminiWorkoutRebalancer to accept the key at runtime (via ViewModel or KeyRepository) instead of only System.getenv. Maintain graceful fallback to offline engines on failure or missing key.
- R-APIKEY-5: Automated Tests: unit tests for ApiKeyRepository (save, retrieve, clear, format validation), UI state tests (Premium AI badge shown/hidden based on key), and parser tests verifying runtime key injection.
- R-APIKEY-6: Storage Constraint: all code, Gradle builds, caches strictly on Drive D: (D:\projects\hostel_gains and D:\.gradle). Zero writes to Drive C:.
- Rebuild the debug APK at D:\projects\hostel_gains\app\build\outputs\apk\debug\app-debug.apk.

Follow the standard orchestration workflow: decompose, update PROJECT.md, dispatch specialist workers, enforce strict quality gates, verify all tests pass on Drive D:, and deliver handoff.md in your working directory when ready for victory audit.
