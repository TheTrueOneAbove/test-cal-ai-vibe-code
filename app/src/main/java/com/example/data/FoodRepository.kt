package com.example.data

import android.graphics.Bitmap
import com.example.ai.GeminiNutritionService
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FoodRepository(
    private val foodDao: FoodDao,
    private val aiService: GeminiNutritionService = GeminiNutritionService()
) {
    /**
     * Retrieves food entries for the specified calendar day timestamp.
     */
    fun getEntriesForDay(dayTimestamp: Long): Flow<List<FoodEntry>> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dayTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        return foodDao.getEntriesForDay(startOfDay, endOfDay)
    }

    suspend fun insertEntry(entry: FoodEntry): Long {
        return foodDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: FoodEntry) {
        foodDao.deleteEntry(entry)
    }

    suspend fun deleteEntryById(id: Long) {
        foodDao.deleteEntryById(id)
    }

    suspend fun analyzeMealText(description: String): Result<NutritionResponse> {
        return aiService.analyzeText(description)
    }

    suspend fun analyzeMealImage(bitmap: Bitmap, optionalNotes: String? = null): Result<NutritionResponse> {
        return aiService.analyzeImage(bitmap, optionalNotes)
    }
}
