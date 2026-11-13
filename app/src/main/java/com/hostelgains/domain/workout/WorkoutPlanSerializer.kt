package com.hostelgains.domain.workout

import com.hostelgains.domain.util.SimpleJsonParser

/**
 * Pure Kotlin serializer and deserializer for dynamic Gemini-generated [WorkoutDayPlan] lists.
 * Safe across both Android runtime and local JVM unit tests.
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

        val coachRationale = (rootNode["coachRationale"] as? SimpleJsonParser.Node.StringNode)?.value ?: ""
        val daysArray = (rootNode["days"] as? SimpleJsonParser.Node.ArrayNode)?.list ?: return null
        val plans = mutableListOf<WorkoutDayPlan>()

        for (i in daysArray.indices) {
            val dayNode = daysArray[i] as? SimpleJsonParser.Node.ObjectNode ?: continue
            val dayNumber = (dayNode["dayNumber"] as? SimpleJsonParser.Node.NumberNode)?.value?.toInt() ?: (i + 1)
            val dayName = (dayNode["dayName"] as? SimpleJsonParser.Node.StringNode)?.value ?: "Day $dayNumber"
            val focusArea = (dayNode["focusArea"] as? SimpleJsonParser.Node.StringNode)?.value ?: "General Training"
            val guidanceNotes = (dayNode["guidanceNotes"] as? SimpleJsonParser.Node.StringNode)?.value ?: ""
            val isActiveRest = (dayNode["isActiveRest"] as? SimpleJsonParser.Node.BooleanNode)?.value ?: false

            val exercises = mutableListOf<Exercise>()
            val exArray = (dayNode["exercises"] as? SimpleJsonParser.Node.ArrayNode)?.list
            if (exArray != null) {
                for (j in exArray.indices) {
                    val exNode = exArray[j] as? SimpleJsonParser.Node.ObjectNode ?: continue
                    val id = (exNode["id"] as? SimpleJsonParser.Node.StringNode)?.value ?: "ex_${dayNumber}_$j"
                    val name = (exNode["name"] as? SimpleJsonParser.Node.StringNode)?.value ?: "Exercise $j"
                    val muscleStr = (exNode["targetMuscle"] as? SimpleJsonParser.Node.StringNode)?.value ?: "BACK"
                    val catStr = (exNode["category"] as? SimpleJsonParser.Node.StringNode)?.value ?: "COMPOUND_PULL"
                    val sets = (exNode["baselineSets"] as? SimpleJsonParser.Node.NumberNode)?.value?.toInt() ?: 3
                    val repRange = (exNode["repRange"] as? SimpleJsonParser.Node.StringNode)?.value ?: "8-12 reps"
                    val restSec = (exNode["restSeconds"] as? SimpleJsonParser.Node.NumberNode)?.value?.toInt() ?: 90
                    val formNotes = (exNode["formNotes"] as? SimpleJsonParser.Node.StringNode)?.value ?: ""
                    val isComp = (exNode["isCompensatoryEligibleCarb"] as? SimpleJsonParser.Node.BooleanNode)?.value ?: true

                    val muscle = runCatching { MuscleGroup.valueOf(muscleStr) }.getOrDefault(MuscleGroup.BACK)
                    val category = runCatching { ExerciseCategory.valueOf(catStr) }.getOrDefault(ExerciseCategory.COMPOUND_PULL)

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
