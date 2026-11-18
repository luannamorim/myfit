package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.MealEntity
import io.github.luannamorim.myfit.data.db.entity.WaterEntryEntity
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    db: MyFitDatabase,
    private val time: TimeSource,
    private val backup: BackupTrigger,
) {
    private val dao = db.nutritionDao()

    fun observeDay(date: LocalDate): Flow<NutritionDay> {
        val day = date.toEpochDay()
        return combine(dao.observeMeals(day), dao.observeWater(day)) { meals, water ->
            NutritionDay(date, meals.map { it.toDomain() }, water.sumOf { it.ml }, water.size)
        }
    }

    /** One [NutritionDay] per date in [range] (empty days included), oldest first. */
    fun observeRange(range: ClosedRange<LocalDate>): Flow<List<NutritionDay>> {
        val from = range.start.toEpochDay()
        val to = range.endInclusive.toEpochDay()
        return combine(dao.observeMealsRange(from, to), dao.observeWaterRange(from, to)) { meals, water ->
            val mealsByDay = meals.groupBy { it.dateEpochDay }
            val waterByDay = water.groupBy { it.dateEpochDay }
            (from..to).map { d ->
                val w = waterByDay[d].orEmpty()
                NutritionDay(LocalDate.ofEpochDay(d), mealsByDay[d].orEmpty().map { it.toDomain() }, w.sumOf { it.ml }, w.size)
            }
        }
    }

    /** Adds [ml] of water to [date]; amounts outside 1..[NutritionDay.MAX_WATER_ML] are ignored. */
    suspend fun addWater(date: LocalDate, ml: Int) {
        if (ml !in 1..NutritionDay.MAX_WATER_ML) return
        dao.insertWater(WaterEntryEntity(dateEpochDay = date.toEpochDay(), ml = ml, createdAtMs = time.now().wallMs))
        backup.onDataChanged()
    }

    /** Removes the last water entry added to [date], if any. */
    suspend fun undoLastWater(date: LocalDate) {
        if (dao.deleteLastWater(date.toEpochDay()) > 0) backup.onDataChanged()
    }

    suspend fun getMeal(id: Long): Meal? = dao.getMeal(id)?.toDomain()

    /** Inserts (id 0) or updates. Returns the id. */
    suspend fun saveMeal(meal: Meal): Long {
        val clean = meal.copy(
            description = meal.description?.trim()?.takeIf { it.isNotEmpty() },
            kcal = meal.kcal.coerceIn(Meal.MIN_KCAL, Meal.MAX_KCAL),
            minuteOfDay = meal.minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1),
        )
        val existing = if (clean.id == 0L) null else dao.getMeal(clean.id)
        val id = if (existing == null) {
            dao.insertMeal(clean.toEntity(id = 0, createdAtMs = time.now().wallMs))
        } else {
            dao.updateMeal(clean.toEntity(id = clean.id, createdAtMs = existing.createdAtMs))
            clean.id
        }
        backup.onDataChanged()
        return id
    }

    suspend fun deleteMeal(id: Long) {
        dao.getMeal(id) ?: return
        dao.deleteMeal(id)
        backup.onDataChanged()
    }

    private fun MealEntity.toDomain() = Meal(id, LocalDate.ofEpochDay(dateEpochDay), minuteOfDay, description, kcal)

    private fun Meal.toEntity(id: Long, createdAtMs: Long) =
        MealEntity(id = id, dateEpochDay = date.toEpochDay(), minuteOfDay = minuteOfDay, description = description, kcal = kcal, createdAtMs = createdAtMs)

    private companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}
