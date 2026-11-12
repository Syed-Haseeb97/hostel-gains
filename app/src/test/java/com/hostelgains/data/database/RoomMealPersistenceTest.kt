package com.hostelgains.data.database

import com.hostelgains.data.nutrition.DefaultMealRepository
import com.hostelgains.domain.nutrition.MealNutrition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMealDao : MealDao {
    val items = mutableListOf<MealEntity>()

    override fun getAll(): List<MealEntity> = items.toList()

    override fun insert(meal: MealEntity) {
        items.removeAll { it.id == meal.id }
        items.add(meal)
    }

    override fun deleteById(id: String) {
        items.removeAll { it.id == id }
    }

    override fun clear() {
        items.clear()
    }
}

class RoomMealPersistenceTest {

    @Test
    fun testEntityDomainMapping() {
        val original = MealNutrition(
            id = "test-123",
            rawQuery = "4 boiled eggs and 2 bananas",
            mealName = "Hostel Breakfast",
            proteinGrams = 28f,
            carbsGrams = 54f,
            fatsGrams = 14f,
            calories = 450
        )

        val entity = original.toEntity("2026-11-12")
        assertEquals("test-123", entity.id)
        assertEquals("2026-11-12", entity.dateIso)
        assertEquals(28f, entity.protein, 0.01f)

        val domain = entity.toDomain()
        assertEquals(original.id, domain.id)
        assertEquals(original.rawQuery, domain.rawQuery)
        assertEquals(original.mealName, domain.mealName)
        assertEquals(original.proteinGrams, domain.proteinGrams, 0.01f)
        assertEquals(original.carbsGrams, domain.carbsGrams, 0.01f)
    }

    @Test
    fun testRepositoryHydratesFromDaoAndQueuesWrites() = runBlocking {
        val fakeDao = FakeMealDao()
        fakeDao.items.add(
            MealEntity(
                "preloaded-1",
                "2026-11-12",
                "Preloaded Paneer",
                "200g paneer",
                36f,
                8f,
                40f,
                540,
                System.currentTimeMillis()
            )
        )

        val repo = DefaultMealRepository(
            dao = fakeDao,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Give initial hydrate a cycle
        Thread.sleep(50)
        val meals = repo.getMealsForDate("2026-11-12").value
        assertEquals(1, meals.size)
        assertEquals("Preloaded Paneer", meals.first().mealName)

        val summary = repo.getDailySummary("2026-11-12").value
        assertEquals(36f, summary.totalProtein, 0.01f)

        // Log a new meal
        val newMeal = MealNutrition(
            id = "live-2",
            rawQuery = "1 scoop whey",
            mealName = "Whey Protein",
            proteinGrams = 24f,
            carbsGrams = 2f,
            fatsGrams = 1f,
            calories = 120
        )
        repo.logMeal(newMeal, "2026-11-12")

        val updatedMeals = repo.getMealsForDate("2026-11-12").value
        assertEquals(2, updatedMeals.size)

        // Verify write was queued to DAO
        assertTrue(fakeDao.items.any { it.id == "live-2" })

        // Delete meal
        repo.deleteMeal("preloaded-1", "2026-11-12")
        assertEquals(1, repo.getMealsForDate("2026-11-12").value.size)
        assertTrue(fakeDao.items.none { it.id == "preloaded-1" })

        // Clear all
        repo.clearAll()
        assertEquals(0, repo.getMealsForDate("2026-11-12").value.size)
        assertEquals(0, fakeDao.items.size)
    }
}
