package com.example

import com.example.ai.GeminiNutritionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiNutritionServiceTest {

    private val service = GeminiNutritionService()

    @Test
    fun testNutrientEstimationFromText_eggsAndAvocado() {
        val result = service.estimateNutrientsFromText("two scrambled eggs, sourdough toast, and half an avocado")
        assertTrue(result.calories > 300)
        assertTrue(result.proteinGrams >= 10.0)
        assertTrue(result.carbsGrams >= 15.0)
        assertTrue(result.fatGrams >= 15.0)
    }

    @Test
    fun testNutrientEstimationFromText_chickenAndRice() {
        val result = service.estimateNutrientsFromText("grilled chicken with white rice")
        assertTrue(result.proteinGrams >= 30.0)
        assertTrue(result.carbsGrams >= 30.0)
    }

    @Test
    fun testNutrientEstimation_fallbackNotEmpty() {
        val result = service.estimateNutrientsFromText("mystery stew")
        assertTrue(result.calories > 0)
        assertTrue(result.foodName.isNotBlank())
    }
}
