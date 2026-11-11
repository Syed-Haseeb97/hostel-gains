# Explorer 2 Dispatch

## 2026-11-10T12:09:00Z
Task: Investigate Jetpack Compose UI Navigation, Onboarding Screen Flow, and Settings / Clear Key Action.
Working directory: D:\projects\hostel_gains\.agents\teamwork\explorer_m6_2
Workspace root: D:\projects\hostel_gains
Original Request: D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md
Master Project: D:\projects\hostel_gains\.agents\teamwork\PROJECT.md

Scope:
1. Examine existing UI screens and navigation in app/src/main/java/com/hostelgains/ui/ (MainActivity, navigation scaffold, screens).
2. Design the First-Launch API Key Onboarding Screen (`ApiKeyOnboardingScreen.kt`):
   - Check if key exists or if user skipped onboarding.
   - Explain offline mode by default using the built-in Indian mess food database.
   - Invite user to paste personal Google AI Studio API key to unlock "Premium AI" features.
   - Hyperlink: https://aistudio.google.com/app/apikey (opens in browser).
   - Password-style TextField with show/hide toggle.
   - "Unlock Premium AI" CTA button validating format (starts with "AIza" and non-empty).
   - "Skip for now — use Offline Mode" button to dismiss.
3. Design Settings / App Menu option for "Clear API Key / Reset to Offline Mode".
4. Formulate recommendations and write report to handoff.md in your working directory.
