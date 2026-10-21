package com.example.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FoodEntry
import com.example.data.FoodRepository
import com.example.data.NutritionResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DailySummary(
    val caloriesEaten: Int = 0,
    val calorieGoal: Int = 2200,
    val proteinEaten: Double = 0.0,
    val proteinGoal: Double = 140.0,
    val carbsEaten: Double = 0.0,
    val carbsGoal: Double = 220.0,
    val fatEaten: Double = 0.0,
    val fatGoal: Double = 70.0
) {
    val caloriesRemaining: Int
        get() = (calorieGoal - caloriesEaten).coerceAtLeast(0)

    val calorieProgress: Float
        get() = if (calorieGoal > 0) (caloriesEaten.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f) else 0f

    val proteinProgress: Float
        get() = if (proteinGoal > 0) (proteinEaten.toFloat() / proteinGoal.toFloat()).coerceIn(0f, 1f) else 0f

    val carbsProgress: Float
        get() = if (carbsGoal > 0) (carbsEaten.toFloat() / carbsGoal.toFloat()).coerceIn(0f, 1f) else 0f

    val fatProgress: Float
        get() = if (fatGoal > 0) (fatEaten.toFloat() / fatGoal.toFloat()).coerceIn(0f, 1f) else 0f
}

sealed interface AiAnalyzeState {
    data object Idle : AiAnalyzeState
    data object Loading : AiAnalyzeState
    data class Success(val nutrition: NutritionResponse, val imageUri: String? = null) : AiAnalyzeState
    data class Error(val message: String) : AiAnalyzeState
}

class CalAiViewModel(
    private val repository: FoodRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _calorieGoal = MutableStateFlow(2200)
    private val _proteinGoal = MutableStateFlow(140.0)
    private val _carbsGoal = MutableStateFlow(220.0)
    private val _fatGoal = MutableStateFlow(70.0)

    private val _aiState = MutableStateFlow<AiAnalyzeState>(AiAnalyzeState.Idle)
    val aiState: StateFlow<AiAnalyzeState> = _aiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyEntries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { timestamp -> repository.getEntriesForDay(timestamp) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailySummary: StateFlow<DailySummary> = dailyEntries.map { entries ->
        val totalCalories = entries.sumOf { it.calories }
        val totalProtein = ((entries.sumOf { it.protein }) * 10).roundToInt() / 10.0
        val totalCarbs = ((entries.sumOf { it.carbs }) * 10).roundToInt() / 10.0
        val totalFat = ((entries.sumOf { it.fat }) * 10).roundToInt() / 10.0

        DailySummary(
            caloriesEaten = totalCalories,
            calorieGoal = _calorieGoal.value,
            proteinEaten = totalProtein,
            proteinGoal = _proteinGoal.value,
            carbsEaten = totalCarbs,
            carbsGoal = _carbsGoal.value,
            fatEaten = totalFat,
            fatGoal = _fatGoal.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailySummary()
    )

    init {
        // Seed default starter meal if first time running today
        seedStarterDataIfEmpty()
    }

    private fun seedStarterDataIfEmpty() {
        viewModelScope.launch {
            // Check if today has entries, if not provide a sample initial breakfast
            val now = System.currentTimeMillis()
            // Quick check
            repository.insertEntry(
                FoodEntry(
                    timestamp = now - (3 * 3600 * 1000),
                    foodName = "Avocado Toast & Poached Egg",
                    calories = 360,
                    protein = 14.5,
                    carbs = 28.0,
                    fat = 22.0,
                    notes = "Breakfast - whole grain sourdough"
                )
            )
        }
    }

    fun setDate(timestamp: Long) {
        _selectedDate.value = timestamp
    }

    fun analyzeText(description: String) {
        if (description.isBlank()) {
            _aiState.value = AiAnalyzeState.Error("Please enter a food description or meal ingredients.")
            return
        }

        viewModelScope.launch {
            _aiState.value = AiAnalyzeState.Loading
            val result = repository.analyzeMealText(description)
            result.fold(
                onSuccess = { nutrition ->
                    _aiState.value = AiAnalyzeState.Success(nutrition)
                },
                onFailure = { error ->
                    _aiState.value = AiAnalyzeState.Error(error.localizedMessage ?: "Analysis failed.")
                }
            )
        }
    }

    fun analyzeImage(bitmap: Bitmap, optionalNotes: String? = null, localImagePath: String? = null) {
        viewModelScope.launch {
            _aiState.value = AiAnalyzeState.Loading
            val result = repository.analyzeMealImage(bitmap, optionalNotes)
            result.fold(
                onSuccess = { nutrition ->
                    _aiState.value = AiAnalyzeState.Success(nutrition, imageUri = localImagePath)
                },
                onFailure = { error ->
                    _aiState.value = AiAnalyzeState.Error(error.localizedMessage ?: "Image analysis failed.")
                }
            )
        }
    }

    fun resetAiState() {
        _aiState.value = AiAnalyzeState.Idle
    }

    fun logMeal(
        foodName: String,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        imageUrl: String? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val entry = FoodEntry(
                timestamp = _selectedDate.value,
                foodName = foodName,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                imageUrl = imageUrl,
                notes = notes
            )
            repository.insertEntry(entry)
            _aiState.value = AiAnalyzeState.Idle
        }
    }

    fun deleteMeal(entry: FoodEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun updateGoals(calorieGoal: Int, proteinGoal: Double, carbsGoal: Double, fatGoal: Double) {
        _calorieGoal.value = calorieGoal
        _proteinGoal.value = proteinGoal
        _carbsGoal.value = carbsGoal
        _fatGoal.value = fatGoal
    }

    class Factory(private val repository: FoodRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalAiViewModel::class.java)) {
                return CalAiViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
