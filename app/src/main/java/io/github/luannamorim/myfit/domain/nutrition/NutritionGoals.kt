package io.github.luannamorim.myfit.domain.nutrition

import kotlin.math.abs

/** Whether a day hit its targets (History): null = nothing logged, or no target to judge by. */
object NutritionGoals {
    /** "Maintain weight" counts within this share of the target (user's pick: 10 %). */
    const val MAINTAIN_MARGIN = 0.10

    /** Water: reached the target. */
    fun waterHit(day: NutritionDay, targets: DailyTargets): Boolean? {
        if (day.waterEntries == 0) return null
        val target = targets.waterMl ?: return null
        return day.waterMl >= target
    }

    /** Calories: at most the target to lose fat, at least it to gain muscle, within 10 % of it to maintain. */
    fun kcalHit(day: NutritionDay, targets: DailyTargets, goal: NutritionGoal): Boolean? {
        if (day.meals.isEmpty()) return null
        val target = targets.kcal ?: return null
        return when (goal) {
            NutritionGoal.LOSE -> day.kcal <= target
            NutritionGoal.GAIN -> day.kcal >= target
            NutritionGoal.MAINTAIN -> abs(day.kcal - target) <= target * MAINTAIN_MARGIN
        }
    }
}
