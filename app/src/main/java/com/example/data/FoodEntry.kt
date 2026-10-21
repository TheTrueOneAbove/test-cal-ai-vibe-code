package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a logged meal or food item.
 * Stores nutritional values and optional local image reference.
 */
@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val foodName: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val imageUrl: String? = null,
    val notes: String? = null
)

/**
 * Structured nutritional response expected from Gemini API.
 */
data class NutritionResponse(
    val foodName: String,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double
) {
    fun toFoodEntry(imageUrl: String? = null, notes: String? = null): FoodEntry {
        return FoodEntry(
            foodName = foodName,
            calories = calories,
            protein = proteinGrams,
            carbs = carbsGrams,
            fat = fatGrams,
            imageUrl = imageUrl,
            notes = notes
        )
    }
}
