package com.hostelgains.domain.nutrition

/**
 * Deterministic offline nutrition parser implementing [NutritionParser].
 *
 * Provides calibrated macronutrient calculations for Indian university hostel mess meals
 * derived from nutritional standards and university dining hall portions:
 * - 1 standard dry roti/chapati: ~17g carbs, ~3g protein, ~0.5g fat, ~80 kcal
 * - 1 katori hostel dal (150g): ~18g carbs, ~5.5g protein, ~4.5g fat, ~135 kcal
 * - 1 plate cooked white rice: ~54g carbs, ~5.5g protein, ~0.5g fat, ~245 kcal
 * - 1 whole boiled egg: ~0.5g carbs, ~6g protein, ~2.5g fat, ~50 kcal
 * - 1 boiled egg white: ~0.25g carbs, ~3.5g protein, ~0.05g fat, ~15 kcal
 * - 1 serving hostel mutton curry (150g): ~6g carbs, ~24g protein, ~16g fat, ~260 kcal
 * - 1 serving paneer bhurji (150g): ~8g carbs, ~19g protein, ~17g fat, ~260 kcal
 * - 1 tawa paratha: ~19g carbs, ~3g protein, ~3g fat, ~115 kcal
 * - 1 cup hostel chai: ~14g carbs, ~2g protein, ~3g fat, ~90 kcal
 * - 1 aloo samosa: ~24g carbs, ~2g protein, ~9.5g fat, ~190 kcal
 */
open class FixtureNutritionParser : NutritionParser {

    override suspend fun parseMeal(rawDescription: String): Result<MealNutrition> {
        val trimmed = rawDescription.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Meal description cannot be empty or whitespace"))
        }

        val normalized = trimmed.lowercase()

        // 1. Check exact / preconfigured hostel mess fixtures
        val preconfigured = getPreconfiguredMeal(normalized, rawDescription)
        if (preconfigured != null) {
            return Result.success(preconfigured)
        }

        // 2. Fall back to heuristic token parsing
        return parseHeuristic(normalized, rawDescription)
    }

    private fun getPreconfiguredMeal(normalized: String, originalQuery: String): MealNutrition? {
        return when {
            // Mutton, Rotis & Eggs
            (normalized.contains("mutton") || normalized.contains("matan") || normalized.contains("muttan")) &&
                    (normalized.contains("roti") || normalized.contains("roty") || normalized.contains("chapati") || normalized.contains("phulka")) &&
                    (normalized.contains("egg") || normalized.contains("anda") || normalized.contains("omlet") || normalized.contains("omelet")) -> {
                MealNutrition(
                    rawQuery = originalQuery,
                    mealName = "Hostel Sunday Special: Mutton, Rotis & Eggs",
                    proteinGrams = 48.0f,
                    carbsGrams = 74.0f,
                    fatsGrams = 30.0f,
                    calories = 770,
                    items = listOf(
                        FoodItemNutrition("Mutton Curry", "1 serving (150g)", 24.0f, 6.0f, 18.0f, 280),
                        FoodItemNutrition("Dry Rotis", "4 chapatis", 12.0f, 68.0f, 2.0f, 340),
                        FoodItemNutrition("Boiled Eggs", "2 whole eggs", 12.0f, 0.0f, 10.0f, 150)
                    ),
                    isHighCarb = true,
                    isLowProtein = false,
                    dietaryNotes = "High protein and carb surplus optimal for hypertrophy"
                )
            }

            // Dal & Rice
            (normalized.contains("dal") || normalized.contains("daal") || normalized.contains("dhal")) &&
                    (normalized.contains("rice") || normalized.contains("chawal") || normalized.contains("chaawal")) &&
                    !normalized.contains("mutton") && !normalized.contains("matan") && !normalized.contains("chicken") &&
                    !normalized.contains("chiken") && !normalized.contains("paneer") && !normalized.contains("panir") &&
                    !normalized.contains("egg") && !normalized.contains("anda") && !normalized.contains("roti") &&
                    !normalized.contains("roty") && !normalized.contains("phulka") &&
                    !normalized.contains("paratha") && !normalized.contains("dudh") && !normalized.contains("milk") &&
                    !normalized.contains("dahi") && !normalized.contains("curd") && !normalized.contains("chana") &&
                    !normalized.contains("chapati") -> {
                MealNutrition(
                    rawQuery = originalQuery,
                    mealName = "Hostel Mess Lunch: Dal & Rice",
                    proteinGrams = 8.5f,
                    carbsGrams = 60.0f,
                    fatsGrams = 4.5f,
                    calories = 315,
                    items = listOf(
                        FoodItemNutrition("Hostel Dal", "1 katori (150g)", 5.0f, 15.0f, 4.0f, 115),
                        FoodItemNutrition("White Rice", "1 plate cooked", 3.5f, 45.0f, 0.5f, 200)
                    ),
                    isHighCarb = true,
                    isLowProtein = true,
                    dietaryNotes = "Carb-dense with protein deficit; triggers compensatory volume ceiling"
                )
            }

            // Paneer Bhurji & Parathas
            (normalized.contains("paneer") || normalized.contains("panir") || normalized.contains("panner")) &&
                    (normalized.contains("paratha") || normalized.contains("roti") || normalized.contains("roty")) -> {
                MealNutrition(
                    rawQuery = originalQuery,
                    mealName = "Vegetarian Power Breakfast: Paneer Bhurji & Parathas",
                    proteinGrams = 27.0f,
                    carbsGrams = 68.0f,
                    fatsGrams = 37.0f,
                    calories = 710,
                    items = listOf(
                        FoodItemNutrition("Paneer Bhurji", "1 plate", 18.0f, 8.0f, 22.0f, 300),
                        FoodItemNutrition("Plain Parathas", "3 pieces", 9.0f, 60.0f, 15.0f, 410)
                    ),
                    isHighCarb = true,
                    isLowProtein = false,
                    dietaryNotes = "High-protein vegetarian option with glycogen-replenishing carbohydrates"
                )
            }

            // Boiled Egg Whites & Rotis
            ((normalized.contains("egg white") || (normalized.contains("white") && (normalized.contains("egg") || normalized.contains("anda"))))) &&
                    (normalized.contains("roti") || normalized.contains("roty") || normalized.contains("chapati")) &&
                    !normalized.contains("dal") && !normalized.contains("daal") && !normalized.contains("rice") -> {
                MealNutrition(
                    rawQuery = originalQuery,
                    mealName = "Lean Fitness Snack: Boiled Egg Whites & Rotis",
                    proteinGrams = 20.0f,
                    carbsGrams = 35.0f,
                    fatsGrams = 1.2f,
                    calories = 238,
                    items = listOf(
                        FoodItemNutrition("Boiled Egg Whites", "4 whites", 14.0f, 1.0f, 0.2f, 68),
                        FoodItemNutrition("Dry Rotis", "2 chapatis", 6.0f, 34.0f, 1.0f, 170)
                    ),
                    isHighCarb = false,
                    isLowProtein = false,
                    dietaryNotes = "Lean protein with moderate carbohydrate support"
                )
            }

            // Chai & Samosas (including "tee", "chaii", "samose")
            (normalized.contains("chai") || normalized.contains("tea") || normalized.contains("tee") || normalized.contains("chaii")) &&
                    (normalized.contains("samosa") || normalized.contains("samose")) -> {
                MealNutrition(
                    rawQuery = originalQuery,
                    mealName = "Hostel Canteen Snack: Chai & Samosas",
                    proteinGrams = 8.0f,
                    carbsGrams = 60.0f,
                    fatsGrams = 23.0f,
                    calories = 485,
                    items = listOf(
                        FoodItemNutrition("Hostel Chai", "1 cup with milk & sugar", 2.0f, 12.0f, 3.0f, 85),
                        FoodItemNutrition("Aloo Samosas", "2 pieces", 6.0f, 48.0f, 20.0f, 400)
                    ),
                    isHighCarb = true,
                    isLowProtein = true,
                    dietaryNotes = "Refined carb and fat heavy; poor protein quality"
                )
            }

            else -> null
        }
    }

    private fun parseHeuristic(normalized: String, originalQuery: String): Result<MealNutrition> {
        val items = mutableListOf<FoodItemNutrition>()
        var totalProtein = 0.0f
        var totalCarbs = 0.0f
        var totalFats = 0.0f
        var totalCalories = 0
        var tokensRecognized = 0

        fun extractCount(pattern: String, defaultCount: Int = 1): Int {
            val match = Regex(pattern).find(normalized) ?: return defaultCount
            val numStr = match.groupValues[1].trim()
            return when (numStr.lowercase()) {
                "one", "1" -> 1
                "two", "2" -> 2
                "three", "3" -> 3
                "four", "4" -> 4
                "five", "5" -> 5
                "six", "6" -> 6
                "seven", "7" -> 7
                "eight", "8" -> 8
                "nine", "9" -> 9
                "ten", "10" -> 10
                else -> numStr.toIntOrNull() ?: defaultCount
            }
        }

        // 1. Rotis / Chapatis / Phulkas (including "roty", "chappati", "fulka")
        if (normalized.contains("roti") || normalized.contains("roty") || normalized.contains("chapati") ||
            normalized.contains("chappati") || normalized.contains("phulka") || normalized.contains("fulka")) {
            val count = extractCount("(\\d+|one|two|three|four|five|six|seven|eight|nine|ten)?\\s*(?:dry\\s*)?(?:roti|roty|chapati|chappati|phulka|fulka)s?", defaultCount = 2)
            val p = count * 3.0f
            val c = count * 17.0f
            val f = count * 0.5f
            val cal = count * 80
            items.add(FoodItemNutrition("Dry Rotis", "$count pieces", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 2. Eggs / Omelette / Anda (including "omlet", "omlete", "omlette", "anda")
        if (normalized.contains("egg") || normalized.contains("anda") || normalized.contains("omlet") ||
            normalized.contains("omlete") || normalized.contains("omlette") || normalized.contains("omelet") || normalized.contains("omelette")) {
            val isWhite = normalized.contains("white")
            val isOmelette = normalized.contains("omlet") || normalized.contains("omelet")
            val count = extractCount("(\\d+|one|two|three|four|five|six|seven|eight|nine|ten)?\\s*(?:boiled\\s*)?(?:eggs?|andas?|omlets?|omletes?|omlettes?|omelets?|omelettes?)", defaultCount = 2)
            val p = count * if (isWhite) 3.5f else 6.0f
            val c = count * if (isWhite) 0.25f else 0.5f
            val f = count * if (isWhite) 0.05f else 5.0f
            val cal = count * if (isWhite) 17 else 75
            val label = when {
                isWhite -> "Boiled Egg Whites"
                isOmelette -> "Hostel Omelette / Anda"
                else -> "Boiled Eggs"
            }
            items.add(FoodItemNutrition(label, "$count eggs", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 3. Mutton (including "matan", "muttan", "gosht")
        if (normalized.contains("mutton") || normalized.contains("matan") || normalized.contains("muttan") || normalized.contains("gosht")) {
            val p = 24.0f; val c = 6.0f; val f = 16.0f; val cal = 260
            items.add(FoodItemNutrition("Hostel Mutton Curry", "1 serving (150g)", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 4. Chicken (including "chiken", "chikken", "chikn", "murgh")
        if (normalized.contains("chicken") || normalized.contains("chiken") || normalized.contains("chikken") ||
            normalized.contains("chikn") || normalized.contains("murgh")) {
            val p = 28.0f; val c = 5.0f; val f = 10.0f; val cal = 220
            items.add(FoodItemNutrition("Hostel Chicken Curry", "1 plate (150g)", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 5. Fish
        if (normalized.contains("fish") || normalized.contains("machhli") || normalized.contains("machli")) {
            val p = 22.0f; val c = 4.0f; val f = 8.0f; val cal = 180
            items.add(FoodItemNutrition("Hostel Fish Curry", "1 plate", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 6. Dal / Daal / Lentils / Sambar (including "dhal", "tadka")
        if (normalized.contains("dal") || normalized.contains("daal") || normalized.contains("dhal") ||
            normalized.contains("sambar") || normalized.contains("lentil") || normalized.contains("tadka")) {
            val count = extractCount("(\\d+|one|two|three)?\\s*(?:katori|bowl|plate)?s?\\s*(?:of\\s*)?(?:dal|daal|dhal|sambar)", defaultCount = 1)
            val p = count * 5.5f
            val c = count * 18.0f
            val f = count * 4.5f
            val cal = count * 135
            items.add(FoodItemNutrition("Hostel Dal", "$count katori", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 7. Rice / Chawal / Biryani / Pulao (including "chaawal", "bhaat")
        if (normalized.contains("rice") || normalized.contains("chawal") || normalized.contains("chaawal") ||
            normalized.contains("biryani") || normalized.contains("pulao") || normalized.contains("bhaat")) {
            val count = extractCount("(\\d+|one|two|three)?\\s*(?:plate|bowl)?s?\\s*(?:of\\s*)?(?:rice|chawal|chaawal|biryani|pulao)", defaultCount = 1)
            val isBiryani = normalized.contains("biryani")
            val p = count * if (isBiryani) 10.0f else 5.5f
            val c = count * if (isBiryani) 60.0f else 54.0f
            val f = count * if (isBiryani) 8.0f else 0.5f
            val cal = count * if (isBiryani) 350 else 245
            val label = if (isBiryani) "Biryani Rice" else "Cooked White Rice"
            items.add(FoodItemNutrition(label, "$count plate", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 8. Paneer (including "panir", "panner")
        if (normalized.contains("paneer") || normalized.contains("panir") || normalized.contains("panner")) {
            val p = 19.0f; val c = 8.0f; val f = 17.0f; val cal = 260
            items.add(FoodItemNutrition("Paneer Dish", "1 plate", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 9. Parathas (including "paratha", "parathe", "paronthe")
        if (normalized.contains("paratha") || normalized.contains("parathe") || normalized.contains("paronthe")) {
            val count = extractCount("(\\d+|one|two|three|four)?\\s*(?:aloo\\s*|paneer\\s*|plain\\s*)?paratha?s?", defaultCount = 2)
            val p = count * 3.5f
            val c = count * 22.0f
            val f = count * 6.0f
            val cal = count * 150
            items.add(FoodItemNutrition("Parathas", "$count pieces", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 10. Samosas (including "samose", "kachori", "patis", "patties")
        if (normalized.contains("samosa") || normalized.contains("samose") || normalized.contains("kachori") ||
            normalized.contains("patis") || normalized.contains("patties")) {
            val count = extractCount("(\\d+|one|two|three|four)?\\s*(?:aloo\\s*)?(?:samosa|samose|kachori|patties)s?", defaultCount = 2)
            val p = count * 2.0f
            val c = count * 24.0f
            val f = count * 9.5f
            val cal = count * 190
            items.add(FoodItemNutrition("Samosas", "$count pieces", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 11. Chai / Tea / Coffee (including "tee", "te", "chaii", "chaai", "koffee")
        val isTeaOrCoffee = normalized.contains("chai") || normalized.contains("chaii") || normalized.contains("chaai") ||
                normalized.contains("tea") || normalized.contains("tee") ||
                normalized.split(" ", ",", "+", "&").any { it == "te" || it == "tee" } ||
                normalized.contains("coffee") || normalized.contains("coffe") || normalized.contains("koffee")
        if (isTeaOrCoffee) {
            val count = extractCount("(\\d+|one|two|three|four)?\\s*(?:cup|cups|glass|glasses)?\\s*(?:of\\s*)?(?:chai|chaii|tea|tee|te|coffee|coffe)", defaultCount = 1)
            val p = count * 2.0f
            val c = count * 14.0f
            val f = count * 3.0f
            val cal = count * 90
            items.add(FoodItemNutrition("Hostel Chai / Tea", "$count cup", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 12. Biscuits (including "biskit", "biscuit", "parle g", "rusk", "cookies")
        if (normalized.contains("biskit") || normalized.contains("biscuit") || normalized.contains("parle") ||
            normalized.contains("rusk") || normalized.contains("cookie") || normalized.contains("cookies")) {
            val count = extractCount("(\\d+|one|two|three|four|five|six)?\\s*(?:piece|pieces)?\\s*(?:biskit|biscuit|rusk|cookie)s?", defaultCount = 2)
            val p = count * 0.8f
            val c = count * 7.0f
            val f = count * 1.5f
            val cal = count * 45
            items.add(FoodItemNutrition("Biscuits / Rusk", "$count pieces", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 13. Milk / Dudh (including "doodh", "dood")
        if (normalized.contains("milk") || normalized.contains("dudh") || normalized.contains("doodh") || normalized.contains("dood")) {
            val count = extractCount("(\\d+|one|two)?\\s*(?:glass|cup)?s?\\s*(?:milk|dudh|doodh)", defaultCount = 1)
            val p = count * 8.0f
            val c = count * 12.0f
            val f = count * 8.0f
            val cal = count * 150
            items.add(FoodItemNutrition("Hostel Milk", "$count glass", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 14. Curd / Dahi / Yogurt / Lassi (including "dhai", "chaas", "buttermilk")
        if (normalized.contains("curd") || normalized.contains("dahi") || normalized.contains("dhai") ||
            normalized.contains("yogurt") || normalized.contains("lassi") || normalized.contains("chaas")) {
            val isLassi = normalized.contains("lassi")
            val p = if (isLassi) 6.0f else 4.0f
            val c = if (isLassi) 25.0f else 5.0f
            val f = if (isLassi) 6.0f else 4.0f
            val cal = if (isLassi) 180 else 70
            val label = if (isLassi) "Sweet Lassi" else "Fresh Curd (Dahi)"
            items.add(FoodItemNutrition(label, "1 serving", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 15. Chole / Rajma / Chana
        if (normalized.contains("chole") || normalized.contains("rajma") || normalized.contains("chana")) {
            val p = 12.0f; val c = 35.0f; val f = 6.0f; val cal = 240
            items.add(FoodItemNutrition("Hostel Curry (Chole/Rajma)", "1 bowl", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        } else if (normalized.contains("soya") || normalized.contains("soyabean")) {
            val p = 18.0f; val c = 12.0f; val f = 1.0f; val cal = 130
            items.add(FoodItemNutrition("Soya Chunks Curry", "1 bowl", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 16. Maggi / Noodles (including "maggie", "meggi")
        if (normalized.contains("maggi") || normalized.contains("maggie") || normalized.contains("meggi") || normalized.contains("noodle")) {
            val count = extractCount("(\\d+|one|two)?\\s*(?:pack|packet|bowl)?s?\\s*(?:maggi|maggie|noodles)", defaultCount = 1)
            val p = count * 6.0f
            val c = count * 42.0f
            val f = count * 13.0f
            val cal = count * 310
            items.add(FoodItemNutrition("Maggi Instant Noodles", "$count pack", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 17. South Indian / Breakfast (Dosa, Idli, Poha, Upma)
        if (normalized.contains("dosa") || normalized.contains("dhosa") || normalized.contains("idli") ||
            normalized.contains("idly") || normalized.contains("poha") || normalized.contains("upma")) {
            val isDosa = normalized.contains("dosa") || normalized.contains("dhosa")
            val isIdli = normalized.contains("idli") || normalized.contains("idly")
            val label = when {
                isDosa -> "Dosa & Chutney"
                isIdli -> "Steamed Idlis"
                else -> "Breakfast Poha / Upma"
            }
            val p = if (isDosa) 4.0f else if (isIdli) 4.0f else 3.5f
            val c = if (isDosa) 30.0f else if (isIdli) 28.0f else 38.0f
            val f = if (isDosa) 5.0f else if (isIdli) 0.5f else 4.0f
            val cal = if (isDosa) 180 else if (isIdli) 140 else 200
            items.add(FoodItemNutrition(label, "1 portion", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        // 18. Fruits / Fitness Supplements (Banana, Whey Shake)
        if (normalized.contains("banana") || normalized.contains("kela") || normalized.contains("apple") ||
            normalized.contains("whey") || normalized.contains("shake") || normalized.contains("scoop")) {
            val isShake = normalized.contains("whey") || normalized.contains("shake") || normalized.contains("scoop")
            val label = if (isShake) "Whey Protein Shake" else "Fresh Banana / Fruit"
            val p = if (isShake) 25.0f else 1.3f
            val c = if (isShake) 3.0f else 27.0f
            val f = if (isShake) 1.5f else 0.3f
            val cal = if (isShake) 130 else 105
            items.add(FoodItemNutrition(label, "1 serving", p, c, f, cal))
            totalProtein += p; totalCarbs += c; totalFats += f; totalCalories += cal
            tokensRecognized++
        }

        if (tokensRecognized == 0) {
            return Result.failure(IllegalArgumentException("No recognized food items in query: '$originalQuery'"))
        }

        return Result.success(
            MealNutrition(
                rawQuery = originalQuery,
                mealName = "Parsed Hostel Meal",
                proteinGrams = totalProtein,
                carbsGrams = totalCarbs,
                fatsGrams = totalFats,
                calories = totalCalories,
                items = items,
                isHighCarb = totalCarbs >= 60.0f,
                isLowProtein = totalProtein < 20.0f,
                dietaryNotes = "Parsed using heuristic offline food tokenizer"
            )
        )
    }
}
