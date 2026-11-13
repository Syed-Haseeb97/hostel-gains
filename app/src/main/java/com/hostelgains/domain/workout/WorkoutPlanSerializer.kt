package com.hostelgains.domain.workout

import com.hostelgains.domain.util.SimpleJsonParser

/**
 * Pure Kotlin serializer and deserializer for dynamic Gemini-generated [WorkoutDayPlan] lists.
 * Safe across both Android runtime and local JVM unit tests, with numeric bounds clamping.
 */
object WorkoutPlanSerializer {

    fun toJson(plans: List<WorkoutDayPlan>, coachRationale: String = ""): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"coachRationale\": \"").append(SimpleJsonParser.escape(coachRationale)).append("\",\n")
        sb.append("  \"days\": [\n")

        for (i in plans.indices) {
            val day = plans[i]
            sb.append("    {\n")
            sb.append("      \"dayNumber\": ").append(day.dayNumber).append(",\n")
            sb.append("      \"dayName\": \"").append(SimpleJsonParser.escape(day.dayName)).append("\",\n")
            sb.append("      \"focusArea\": \"").append(SimpleJsonParser.escape(day.focusArea)).append("\",\n")
            sb.append("      \"guidanceNotes\": \"").append(SimpleJsonParser.escape(day.guidanceNotes)).append("\",\n")
            sb.append("      \"isActiveRest\": ").append(day.isActiveRest).append(",\n")
            sb.append("      \"exercises\": [\n")

            for (j in day.exercises.indices) {
                val ex = day.exercises[j]
                sb.append("        {\n")
                sb.append("          \"id\": \"").append(SimpleJsonParser.escape(ex.id)).append("\",\n")
                sb.append("          \"name\": \"").append(SimpleJsonParser.escape(ex.name)).append("\",\n")
                sb.append("          \"targetMuscle\": \"").append(ex.targetMuscle.name).append("\",\n")
                sb.append("          \"category\": \"").append(ex.category.name).append("\",\n")
                sb.append("          \"baselineSets\": ").append(ex.baselineSets).append(",\n")
                sb.append("          \"repRange\": \"").append(SimpleJsonParser.escape(ex.repRange)).append("\",\n")
                sb.append("          \"restSeconds\": ").append(ex.restSeconds).append(",\n")
                sb.append("          \"formNotes\": \"").append(SimpleJsonParser.escape(ex.formNotes)).append("\",\n")
                sb.append("          \"isCompensatoryEligibleCarb\": ").append(ex.isCompensatoryEligibleCarb).append("\n")
                sb.append("        }")
                if (j < day.exercises.size - 1) sb.append(",")
                sb.append("\n")
            }

            sb.append("      ]\n")
            sb.append("    }")
            if (i < plans.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun fromJson(jsonStr: String): Pair<String, List<WorkoutDayPlan>>? {
        val rootNode = SimpleJsonParser.parse(jsonStr) as? SimpleJsonParser.Node.ObjectNode ?: return null

        val coachRationale = rootNode.optString("coachRationale", "").take(500)
        val daysArray = (rootNode["days"] as? SimpleJsonParser.Node.ArrayNode)?.list ?: return null
        val plans = mutableListOf<WorkoutDayPlan>()

        for (i in daysArray.take(7).indices) {
            val dayNode = daysArray[i] as? SimpleJsonParser.Node.ObjectNode ?: continue
            val dayNumber = dayNode.optInt("dayNumber", i + 1).coerceIn(1, 7)
            val dayName = dayNode.optString("dayName", "Day $dayNumber").take(80)
            val focusArea = dayNode.optString("focusArea", "General Training").take(100)
            val guidanceNotes = dayNode.optString("guidanceNotes", "").take(300)
            val isActiveRest = dayNode.optBoolean("isActiveRest", false)

            val exercises = mutableListOf<Exercise>()
            val exArray = (dayNode["exercises"] as? SimpleJsonParser.Node.ArrayNode)?.list
            if (exArray != null) {
                for (j in exArray.take(15).indices) {
                    val exNode = exArray[j] as? SimpleJsonParser.Node.ObjectNode ?: continue
                    val id = exNode.optString("id", "ex_${dayNumber}_$j").take(60)
                    val name = exNode.optString("name", "Exercise $j").take(80)
                    val muscleStr = exNode.optString("targetMuscle", "BACK")
                    val catStr = exNode.optString("category", "COMPOUND_PULL")
                    val sets = exNode.optInt("baselineSets", 3).coerceIn(1, 15)
                    val repRange = exNode.optString("repRange", "8-12 reps").take(40)
                    val restSec = exNode.optInt("restSeconds", 90).coerceIn(15, 600)
                    val formNotes = exNode.optString("formNotes", "").take(250)
                    val isComp = exNode.optBoolean("isCompensatoryEligibleCarb", true)

                    val muscle = runCatching { MuscleGroup.valueOf(muscleStr.uppercase()) }.getOrDefault(MuscleGroup.BACK)
                    val category = runCatching { ExerciseCategory.valueOf(catStr.uppercase()) }.getOrDefault(ExerciseCategory.COMPOUND_PULL)

                    exercises.add(
                        Exercise(
                            id = id,
                            name = name,
                            targetMuscle = muscle,
                            category = category,
                            baselineSets = sets,
                            repRange = repRange,
                            restSeconds = restSec,
                            formNotes = formNotes,
                            isCompensatoryEligibleCarb = isComp
                        )
                    )
                }
            }

            plans.add(
                WorkoutDayPlan(
                    dayNumber = dayNumber,
                    dayName = dayName,
                    focusArea = focusArea,
                    exercises = exercises,
                    isActiveRest = isActiveRest,
                    guidanceNotes = guidanceNotes
                )
            )
        }

        return if (plans.isEmpty()) null else Pair(coachRationale, plans)
    }
}
