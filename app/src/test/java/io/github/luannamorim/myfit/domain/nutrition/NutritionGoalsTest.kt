package io.github.luannamorim.myfit.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NutritionGoalsTest {
    private val day = LocalDate.of(2026, 11, 17)
    private val targets = DailyTargets(2300, 2800, emptySet())
    private fun log(kcal: Int?, waterMl: Int?) = NutritionDay(
        day,
        kcal?.let { listOf(Meal(1, day, 600, null, it)) }.orEmpty(),
        waterMl ?: 0,
        if (waterMl == null) 0 else 1,
    )

    @Test
    fun water_hitAtOrPastTheTarget_noRecordWithoutEntries() {
        assertEquals(true, NutritionGoals.waterHit(log(null, 2800), targets))
        assertEquals(false, NutritionGoals.waterHit(log(null, 2750), targets))
        assertNull(NutritionGoals.waterHit(log(null, null), targets))
        assertNull(NutritionGoals.waterHit(log(null, 3000), DailyTargets(2300, null, setOf(TargetMissing.WEIGHT))))
    }

    @Test
    fun calories_dependOnTheGoal() {
        assertEquals(true, NutritionGoals.kcalHit(log(2300, null), targets, NutritionGoal.LOSE))
        assertEquals(false, NutritionGoals.kcalHit(log(2310, null), targets, NutritionGoal.LOSE))
        assertEquals(true, NutritionGoals.kcalHit(log(2300, null), targets, NutritionGoal.GAIN))
        assertEquals(false, NutritionGoals.kcalHit(log(2290, null), targets, NutritionGoal.GAIN))
        // Maintain: within 10 % (2 070–2 530 for 2 300).
        assertEquals(true, NutritionGoals.kcalHit(log(2070, null), targets, NutritionGoal.MAINTAIN))
        assertEquals(true, NutritionGoals.kcalHit(log(2530, null), targets, NutritionGoal.MAINTAIN))
        assertEquals(false, NutritionGoals.kcalHit(log(2069, null), targets, NutritionGoal.MAINTAIN))
        assertEquals(false, NutritionGoals.kcalHit(log(2531, null), targets, NutritionGoal.MAINTAIN))
    }

    @Test
    fun calories_noMealsOrNoTarget_isNoRecord() {
        assertNull(NutritionGoals.kcalHit(log(null, 500), targets, NutritionGoal.MAINTAIN))
        assertNull(NutritionGoals.kcalHit(log(2000, null), DailyTargets(null, 2800, setOf(TargetMissing.HEIGHT)), NutritionGoal.MAINTAIN))
    }
}
