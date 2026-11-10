# Dispatch Task: Explorer Survey 3 (Domain Logic, AI Nutrition Parsing & Dynamic Workout Engine)

## Objective
Analyze the business and domain logic for hostel mess nutrition tracking, AI-based macronutrient parsing, V-Taper PPL framework modeling, and the dynamic compensatory workout engine.

## Inputs
- `D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md` (read first)
- Working directory: `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3`

## Specific Questions to Answer
1. Nutrition Parsing: How should natural language hostel mess meals (mutton, dry rotis, eggs, dal, rice, etc.) be parsed via AI API (e.g., Gemini API / Google GenAI SDK / HTTP client)? What prompt and schema are needed for reliable JSON extraction of protein, carbs, fats, and calories? How can offline/unit tests run deterministically with mock/fixture responses?
2. V-Taper PPL Framework: Document all data structures for Days 1 to 7 (Pull A, Push A, Legs & Core, Pull B, Push B, Weak-Point Pump, Active Rest). Enumerate all exercises, baseline sets, rep ranges, target muscles, and form notes.
3. Dynamic Compensatory Engine:
   - Carb Trigger: When high-carb meal is logged, dynamically add 1 extra set to Lat Pulldowns and Squats in the evening session. (Define threshold and exact modification logic).
   - Protein Deficit Trigger: When low protein intake is logged (e.g., vegetarian dal and rice meals), automatically cap all exercises at maximum 3 sets. (Define threshold and exact modification logic).
   - Step Counter Sync: Monitor daily steps, trigger UI alert if under 5,000 steps by 6:00 PM recommending 10,000-step walk.
4. How should the state flow through ViewModels, Repositories, Room Database / local persistence, and test harnesses?
5. Design the automated unit test specifications required by the acceptance criteria.

## Deliverable
Write your detailed report to `D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md`.
Send a completion message back with the summary and path.


## 2026-11-10T01:47:36Z
[Message from parent 369d8cfd-670f-4689-a71e-4ac9454c73da]
You are an Explorer subagent (Domain Logic & Engine Explorer).
Your working directory is: D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3
Read D:\projects\hostel_gains\.agents\teamwork\ORIGINAL_REQUEST.md and D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\DISPATCH.md.

Investigate and document the domain logic and engine architecture:
1. AI Nutrition Parsing: Natural language meal input (mutton, dry rotis, eggs, dal, rice, etc.) -> protein, carbs, fats, calories. Design prompt template, structured JSON schema, and API integration (Gemini REST API / client). Define deterministic offline mocking/fixtures so automated unit tests verify parsing without requiring live API keys or network during test runs.
2. V-Taper PPL Framework: Document all 6 training days + Day 7 active rest with exact exercises, target sets, rep ranges, and form notes as defined in R3.
3. Dynamic Compensatory Engine:
   - Carb Trigger: When high-carb meal is logged, dynamically add 1 extra set to Lat Pulldowns and Squats in the evening session.
   - Protein Deficit Trigger: When low protein intake is logged (e.g., vegetarian dal and rice meals), automatically cap all exercises at maximum 3 sets.
   - Step Counter Sync: Monitor daily steps, trigger UI alert if under 5,000 steps by 6:00 PM recommending 10,000-step walk.
4. Repository, Room DB / StateFlow / ViewModel architecture.
5. Automated test specifications covering all Acceptance Criteria.

Write your findings to D:\projects\hostel_gains\.agents\teamwork\explorer_survey_3\handoff.md.
Update your progress.md periodically for liveness.
When finished, send a message to the orchestrator with the summary and report path.
