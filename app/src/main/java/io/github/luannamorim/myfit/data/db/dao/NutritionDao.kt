package io.github.luannamorim.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.luannamorim.myfit.data.db.entity.MealEntity
import io.github.luannamorim.myfit.data.db.entity.WaterEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionDao {
    @Query("SELECT * FROM water_entry WHERE dateEpochDay = :day ORDER BY id")
    fun observeWater(day: Long): Flow<List<WaterEntryEntity>>

    @Query("SELECT * FROM meal WHERE dateEpochDay = :day ORDER BY minuteOfDay, id")
    fun observeMeals(day: Long): Flow<List<MealEntity>>

    @Insert suspend fun insertWater(entry: WaterEntryEntity): Long

    /** Removes the most recent entry of [day]; returns how many rows went (0 or 1). */
    @Query("DELETE FROM water_entry WHERE id = (SELECT id FROM water_entry WHERE dateEpochDay = :day ORDER BY id DESC LIMIT 1)")
    suspend fun deleteLastWater(day: Long): Int

    @Query("SELECT * FROM meal WHERE id = :id")
    suspend fun getMeal(id: Long): MealEntity?

    @Insert suspend fun insertMeal(meal: MealEntity): Long

    @Update suspend fun updateMeal(meal: MealEntity)

    @Query("DELETE FROM meal WHERE id = :id")
    suspend fun deleteMeal(id: Long)

    @Query("SELECT * FROM water_entry ORDER BY id")
    suspend fun getAllWater(): List<WaterEntryEntity>

    @Query("SELECT * FROM meal ORDER BY id")
    suspend fun getAllMeals(): List<MealEntity>
}
