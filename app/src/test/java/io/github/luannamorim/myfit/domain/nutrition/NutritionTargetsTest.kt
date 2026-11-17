package io.github.luannamorim.myfit.domain.nutrition

import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NutritionTargetsTest {
    private val day = LocalDate.of(2026, 11, 17)
    private val born = LocalDate.of(1996, 11, 17) // 30 on [day]

    private fun assessment(id: Long, date: LocalDate, weight: Double? = null, height: Double? = null) =
        BodyAssessment(id, date, listOfNotNull(weight?.let { Measure.WEIGHT to it }, height?.let { Measure.HEIGHT to it }).toMap())

    private val body = listOf(assessment(1, day.minusDays(10), weight = 80.0, height = 178.0))

    private fun targets(
        assessments: List<BodyAssessment> = body,
        activeDays: Set<LocalDate> = emptySet(),
        sex: Sex? = Sex.MALE,
        birth: LocalDate? = born,
        goal: NutritionGoal = NutritionGoal.MAINTAIN,
        date: LocalDate = day,
    ) = NutritionTargets.forDay(date, assessments, activeDays, sex, birth, goal)

    @Test
    fun bmr_male_mifflinStJeor() = assertEquals(1767.5, NutritionTargets.bmr(80.0, 178.0, 30, Sex.MALE), 0.001)

    @Test
    fun bmr_female_mifflinStJeor() = assertEquals(1320.25, NutritionTargets.bmr(60.0, 165.0, 30, Sex.FEMALE), 0.001)

    @Test
    fun activityFactor_bandsByActiveDaysPerWeek() {
        assertEquals(1.2, NutritionTargets.activityFactor(0), 0.0)
        assertEquals(1.2, NutritionTargets.activityFactor(3), 0.0) // < 1 day/week
        assertEquals(1.375, NutritionTargets.activityFactor(4), 0.0)
        assertEquals(1.375, NutritionTargets.activityFactor(11), 0.0) // < 3 days/week
        assertEquals(1.55, NutritionTargets.activityFactor(12), 0.0)
        assertEquals(1.55, NutritionTargets.activityFactor(19), 0.0) // < 5 days/week
        assertEquals(1.725, NutritionTargets.activityFactor(20), 0.0)
        assertEquals(1.725, NutritionTargets.activityFactor(28), 0.0)
    }

    @Test
    fun maintain_sedentary_isBmrTimes1_2_roundedToTens() {
        // 1767.5 × 1.2 = 2121 → 2120
        assertEquals(2120, targets().kcal)
    }

    @Test
    fun goal_shiftsTheTarget() {
        val active = (1L..12L).map { day.minusDays(it) }.toSet() // 1.55 → 2739.6
        assertEquals(2240, targets(activeDays = active, goal = NutritionGoal.LOSE).kcal) // 2239.6
        assertEquals(2420, targets(goal = NutritionGoal.GAIN).kcal) // 2121 + 300
        // Sedentary: 2121 − 500 = 1621 is below the basal 1767.5 → floor, 1770.
        assertEquals(1770, targets(goal = NutritionGoal.LOSE).kcal)
    }

    @Test
    fun lose_neverGoesBelowBmr() {
        // Light woman, sedentary: 1320.25 × 1.2 − 500 = 1084.3 < BMR → floor at BMR, 1320.
        val small = listOf(assessment(1, day.minusDays(1), weight = 60.0, height = 165.0))
        assertEquals(1320, targets(small, sex = Sex.FEMALE, goal = NutritionGoal.LOSE).kcal)
    }

    @Test
    fun activeDays_countOnlyThe28DaysBeforeTheDay() {
        val window = (1L..12L).map { day.minusDays(it) }.toSet() // 12 days → 3/week → 1.55
        val outside = setOf(day, day.minusDays(29), day.plusDays(1)) // the day itself and out of range don't count
        // 1767.5 × 1.55 = 2739.6 → 2740
        assertEquals(2740, targets(activeDays = window + outside).kcal)
        assertEquals(NutritionTargets.activityWindow(day), day.minusDays(28)..day.minusDays(1))
    }

    @Test
    fun water_is35mlPerKg_roundedTo50() {
        assertEquals(2800, targets().waterMl) // 80 × 35
        val odd = listOf(assessment(1, day, weight = 71.3, height = 178.0)) // 2495.5 → 2500
        assertEquals(2500, targets(odd).waterMl)
    }

    @Test
    fun usesLastKnownWeightAndHeight_upToTheDay() {
        val items = listOf(
            assessment(1, day.minusDays(60), weight = 90.0, height = 178.0),
            assessment(2, day.minusDays(5), weight = 80.0), // quick weigh-in, no height
            assessment(3, day.plusDays(3), weight = 70.0, height = 180.0), // after the day: ignored
        )
        assertEquals(2800, targets(items).waterMl) // 80 kg
        assertEquals(2120, targets(items).kcal) // 80 kg, 178 cm
    }

    @Test
    fun beforeAnyAssessment_usesTheFirstOne() {
        val later = listOf(assessment(1, day.plusDays(2), weight = 80.0, height = 178.0))
        assertEquals(2800, targets(later).waterMl)
        assertEquals(2120, targets(later).kcal)
    }

    @Test
    fun implausibleHeight_isIgnored() {
        val items = listOf(
            assessment(1, day.minusDays(30), height = 178.0),
            assessment(2, day.minusDays(2), weight = 80.0, height = 17.8), // typo
        )
        assertEquals(2120, targets(items).kcal)
    }

    @Test
    fun ageIsTakenOnTheDay() {
        // Turns 30 on [day]: the day before is still 29 → BMR + 5 → 1772.5 × 1.2 = 2127 → 2130.
        assertEquals(2130, targets(date = day.minusDays(1)).kcal)
    }

    @Test
    fun missing_listsWhatToFill_andWaterOnlyNeedsWeight() {
        val heightOnly = listOf(assessment(1, day, height = 178.0))
        val t = targets(heightOnly, sex = null, birth = null)
        assertNull(t.kcal)
        assertNull(t.waterMl)
        assertEquals(setOf(TargetMissing.WEIGHT, TargetMissing.SEX, TargetMissing.BIRTH_DATE), t.missing)

        val weightOnly = targets(listOf(assessment(1, day, weight = 80.0)))
        assertNull(weightOnly.kcal)
        assertEquals(2800, weightOnly.waterMl)
        assertEquals(setOf(TargetMissing.HEIGHT), weightOnly.missing)

        assertTrue(targets().missing.isEmpty())
    }

    @Test
    fun implausibleAge_countsAsMissingBirthDate() {
        assertEquals(setOf(TargetMissing.BIRTH_DATE), targets(birth = day.minusYears(5)).missing)
        assertEquals(setOf(TargetMissing.BIRTH_DATE), targets(birth = day.minusYears(120)).missing)
    }

    @Test
    fun nutritionDay_sumsMealsAndWater() {
        val d = NutritionDay(
            date = day,
            meals = listOf(Meal(1, day, 480, "Pão com ovo", 320), Meal(2, day, 750, null, 920)),
            waterMl = 1500,
            waterEntries = 4,
        )
        assertEquals(1240, d.kcal)
    }
}
