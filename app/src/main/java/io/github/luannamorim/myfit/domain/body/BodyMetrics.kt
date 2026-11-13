package io.github.luannamorim.myfit.domain.body

import kotlin.math.log10

/**
 * Indicators the app calculates from what the user typed: BMI from weight and height, and body fat
 * estimated with the US Navy circumference method (Hodgdon & Beckett) when none was typed.
 */
object BodyMetrics {
    /** Estimates outside this range come from a typo or a wrong measurement, not from a body. */
    private val PLAUSIBLE_BODY_FAT = 2.0..60.0

    /** Heights outside this range are typos ("17,8"); they are ignored, never used for BMI. */
    val PLAUSIBLE_HEIGHT_CM = 100.0..250.0

    fun bmi(weightKg: Double, heightCm: Double): Double {
        val m = heightCm / 100
        return round1(weightKg / (m * m))
    }

    /**
     * Navy body fat %, all in cm. [waistCm] is the abdomen at the navel for men and the narrowest
     * waist for women; women also need [hipsCm]. Null when the inputs can't come from a body.
     */
    fun navyBodyFat(sex: Sex, heightCm: Double, neckCm: Double, waistCm: Double, hipsCm: Double?): Double? {
        val value = when (sex) {
            Sex.MALE -> {
                val girth = waistCm - neckCm
                if (girth <= 0) return null
                495 / (1.0324 - 0.19077 * log10(girth) + 0.15456 * log10(heightCm)) - 450
            }
            Sex.FEMALE -> {
                val girth = waistCm + (hipsCm ?: return null) - neckCm
                if (girth <= 0) return null
                495 / (1.29579 - 0.35004 * log10(girth) + 0.22100 * log10(heightCm)) - 450
            }
        }
        if (!value.isFinite() || value !in PLAUSIBLE_BODY_FAT) return null
        return round1(value)
    }

    /**
     * Same assessments, in the same order, with BMI and (when [sex] is known and body fat wasn't
     * typed) an estimated body fat added to their values and listed in `estimated`. An assessment
     * without its own (plausible) height uses the latest one recorded before it, else the first one after it.
     */
    fun derive(items: List<BodyAssessment>, sex: Sex?): List<BodyAssessment> {
        val heights = items.mapNotNull { a -> a.height()?.let { a.date to it } }.sortedBy { it.first }
        return items.map { a ->
            val v = a.values
            val height = a.height()
                ?: heights.lastOrNull { !it.first.isAfter(a.date) }?.second
                ?: heights.firstOrNull()?.second
                ?: return@map a
            val added = buildMap {
                v[Measure.WEIGHT]?.let { put(Measure.BMI, bmi(it, height)) }
                if (sex != null && v[Measure.BODY_FAT] == null) estimate(sex, height, v)?.let { put(Measure.BODY_FAT, it) }
            }
            if (added.isEmpty()) a else a.copy(values = v + added, estimated = a.estimated + added.keys)
        }
    }

    private fun BodyAssessment.height(): Double? = values[Measure.HEIGHT]?.takeIf { it in PLAUSIBLE_HEIGHT_CM }

    private fun estimate(sex: Sex, height: Double, v: Map<Measure, Double>): Double? {
        val neck = v[Measure.NECK] ?: return null
        return when (sex) {
            Sex.MALE -> navyBodyFat(sex, height, neck, v[Measure.ABDOMEN] ?: v[Measure.WAIST] ?: return null, null)
            Sex.FEMALE -> navyBodyFat(sex, height, neck, v[Measure.WAIST] ?: return null, v[Measure.HIPS])
        }
    }

    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}
