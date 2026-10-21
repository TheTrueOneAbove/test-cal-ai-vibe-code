package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM food_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEntriesForDay(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): FoodEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: FoodEntry): Long

    @Update
    suspend fun updateEntry(entry: FoodEntry)

    @Delete
    suspend fun deleteEntry(entry: FoodEntry)

    @Query("DELETE FROM food_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)

    @Query("SELECT SUM(calories) FROM food_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getDailyTotalCalories(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT SUM(protein) FROM food_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getDailyTotalProtein(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(carbs) FROM food_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getDailyTotalCarbs(startOfDay: Long, endOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(fat) FROM food_entries WHERE timestamp >= :startOfDay AND timestamp < :endOfDay")
    fun getDailyTotalFat(startOfDay: Long, endOfDay: Long): Flow<Double?>
}
