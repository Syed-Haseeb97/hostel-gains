package com.hostelgains.e2e.fixtures

import com.hostelgains.domain.nutrition.FixtureNutritionParser as DomainFixtureNutritionParser

/**
 * Test fixture delegating to production [DomainFixtureNutritionParser] to ensure full parity
 * across unit tests, E2E tests, and production code.
 */
class FixtureNutritionParser : DomainFixtureNutritionParser()
