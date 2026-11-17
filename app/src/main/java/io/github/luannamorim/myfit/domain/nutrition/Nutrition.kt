package io.github.luannamorim.myfit.domain.nutrition

import java.time.LocalDate

/** What the calorie target aims for (Settings › Profile). */
enum class NutritionGoal { LOSE, MAINTAIN, GAIN }

/** What the user can fill in to get a target, in display order. */
enum class TargetMissing { WEIGHT, HEIGHT, SEX, BIRTH_DATE }

/** Targets for one day; null when a value is [missing]. Water only needs the weight. */
data class DailyTargets(val kcal: Int?, val waterMl: Int?, val missing: Set<TargetMissing>)

/** A logged meal: free text and the calories the user typed (often estimated by an AI app). */
data class Meal(
    val id: Long = 0,
    val date: LocalDate,
    /** Minutes after midnight, 0..1439. */
    val minuteOfDay: Int,
    val description: String?,
    val kcal: Int,
) {
    companion object {
        const val MIN_KCAL = 1
        const val MAX_KCAL = 9999
    }
}

/** Everything logged on [date]: meals by time, and the water total. */
data class NutritionDay(val date: LocalDate, val meals: List<Meal>, val waterMl: Int, val waterEntries: Int) {
    val kcal: Int get() = meals.sumOf { it.kcal }

    companion object {
        const val MAX_WATER_ML = 5000
    }
}
