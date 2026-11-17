package io.github.luannamorim.myfit.domain.nutrition

import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import java.time.LocalDate
import java.time.Period
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Daily calorie and water targets. Estimates from well-known formulas, not advice:
 * - calories: Mifflin-St Jeor basal rate × an activity factor from the days with a workout or an
 *   activity in the 4 weeks before the day, shifted by the goal and never below the basal rate;
 * - water: 35 ml per kg.
 * Everything is taken as it was on the shown day, so a later weigh-in doesn't rewrite past days.
 */
object NutritionTargets {
    const val WATER_ML_PER_KG = 35
    const val ACTIVITY_WINDOW_DAYS = 28L
    private val PLAUSIBLE_AGE = 10..100

    fun bmr(weightKg: Double, heightCm: Double, ageYears: Int, sex: Sex): Double =
        10 * weightKg + 6.25 * heightCm - 5 * ageYears + if (sex == Sex.MALE) 5 else -161

    /** [activeDays] = days with a workout or activity in the [activityWindow] (4 weeks). */
    fun activityFactor(activeDays: Int): Double {
        val perWeek = activeDays / 4.0
        return when {
            perWeek < 1 -> 1.2
            perWeek < 3 -> 1.375
            perWeek < 5 -> 1.55
            else -> 1.725
        }
    }

    /** The 4 weeks that end the day before [date]. */
    fun activityWindow(date: LocalDate): ClosedRange<LocalDate> = date.minusDays(ACTIVITY_WINDOW_DAYS)..date.minusDays(1)

    fun goalDelta(goal: NutritionGoal): Int = when (goal) {
        NutritionGoal.LOSE -> -500
        NutritionGoal.MAINTAIN -> 0
        NutritionGoal.GAIN -> 300
    }

    fun forDay(
        date: LocalDate,
        assessments: List<BodyAssessment>,
        activeDays: Set<LocalDate>,
        sex: Sex?,
        birthDate: LocalDate?,
        goal: NutritionGoal,
    ): DailyTargets {
        val weight = known(BodyComparison.series(assessments, Measure.WEIGHT), date)
        val height = known(BodyComparison.series(assessments, Measure.HEIGHT).filter { it.second in BodyMetrics.PLAUSIBLE_HEIGHT_CM }, date)
        val age = birthDate?.let { Period.between(it, date).years }?.takeIf { it in PLAUSIBLE_AGE }

        val missing = buildSet {
            if (weight == null) add(TargetMissing.WEIGHT)
            if (height == null) add(TargetMissing.HEIGHT)
            if (sex == null) add(TargetMissing.SEX)
            if (age == null) add(TargetMissing.BIRTH_DATE)
        }
        val kcal = if (weight != null && height != null && sex != null && age != null) {
            val basal = bmr(weight, height, age, sex)
            val window = activityWindow(date)
            val active = activeDays.count { it in window }
            roundTo(max(basal * activityFactor(active) + goalDelta(goal), basal), 10)
        } else {
            null
        }
        val water = weight?.let { roundTo(it * WATER_ML_PER_KG, 50) }
        return DailyTargets(kcal, water, missing)
    }

    /** Last value up to [date]; before the first one, the first one (nutrition logs can predate the first weigh-in). */
    private fun known(series: List<Pair<LocalDate, Double>>, date: LocalDate): Double? =
        (series.lastOrNull { !it.first.isAfter(date) } ?: series.firstOrNull())?.second

    private fun roundTo(value: Double, step: Int): Int = (value / step).roundToInt() * step
}
