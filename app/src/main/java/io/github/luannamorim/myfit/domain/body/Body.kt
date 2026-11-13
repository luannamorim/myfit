package io.github.luannamorim.myfit.domain.body

import java.time.LocalDate

/** NONE: shown as a bare number (BMI). */
enum class MeasureUnit { KG, PERCENT, CM, NONE }

/** Used only to estimate body fat (US Navy formulas differ by sex). */
enum class Sex { MALE, FEMALE }

/**
 * Indicators of a body assessment, in display order. [derived] ones are never typed or stored:
 * the app calculates them (see [BodyMetrics]).
 */
enum class Measure(val unit: MeasureUnit, val derived: Boolean = false) {
    WEIGHT(MeasureUnit.KG),
    HEIGHT(MeasureUnit.CM),
    BODY_FAT(MeasureUnit.PERCENT),
    BMI(MeasureUnit.NONE, derived = true),
    NECK(MeasureUnit.CM),
    SHOULDERS(MeasureUnit.CM),
    CHEST(MeasureUnit.CM),
    WAIST(MeasureUnit.CM),
    ABDOMEN(MeasureUnit.CM),
    HIPS(MeasureUnit.CM),
    ARM_RIGHT(MeasureUnit.CM),
    ARM_LEFT(MeasureUnit.CM),
    FOREARM_RIGHT(MeasureUnit.CM),
    FOREARM_LEFT(MeasureUnit.CM),
    THIGH_RIGHT(MeasureUnit.CM),
    THIGH_LEFT(MeasureUnit.CM),
    CALF_RIGHT(MeasureUnit.CM),
    CALF_LEFT(MeasureUnit.CM),
}

data class BodyAssessment(
    val id: Long = 0,
    val date: LocalDate,
    val values: Map<Measure, Double> = emptyMap(),
    val notes: String? = null,
    /** Measures in [values] the app calculated instead of the user typing them (BMI, an estimated body fat). */
    val estimated: Set<Measure> = emptySet(),
) {
    /** How many values the user typed. */
    val measuredCount: Int get() = values.keys.count { it !in estimated }
}

/** Health range of a value (BMI and body fat only), shown as a green, amber or red dot. */
enum class BodyStatus { GOOD, CAUTION, POOR }

/** Why a dashboard card has no value: what the user can add to get one. */
enum class BodyMissing { HEIGHT, WEIGHT, SEX, MEASUREMENTS }

/**
 * [estimated]: either side is an estimate (BMI, always calculated, isn't flagged).
 * [status]: health range of [to], filled by [BodyMetrics.withStatus] for the dashboard.
 * [missing]: when [to] is null, what is missing to get a value (also from [BodyMetrics.withStatus]).
 */
data class MeasureDiff(
    val measure: Measure,
    val from: Double?,
    val to: Double?,
    val diff: Double?,
    val estimated: Boolean = false,
    val status: BodyStatus? = null,
    val missing: BodyMissing? = null,
)

/** Latest assessment with its change versus the previous one, for the dashboard. */
data class BodySnapshot(
    val latest: BodyAssessment,
    val previous: BodyAssessment?,
    val highlights: List<MeasureDiff>,
    /** Recent values of each highlight, oldest first (at most [BodyComparison.TREND_POINTS]). */
    val trends: Map<Measure, List<Pair<LocalDate, Double>>> = emptyMap(),
)

data class BodyPhoto(val id: Long, val date: LocalDate, val imagePath: String, val pose: io.github.luannamorim.myfit.domain.model.PhotoPose)

object BodyComparison {
    /** Absolute difference per indicator: to − from (null when either side is missing). */
    fun compare(from: BodyAssessment, to: BodyAssessment): List<MeasureDiff> = Measure.entries.map { m ->
        val a = from.values[m]
        val b = to.values[m]
        MeasureDiff(m, a, b, if (a != null && b != null) round2(b - a) else null, isEstimate(m, from, to))
    }

    /** Values of [measure] over time, oldest first, skipping assessments without it. */
    fun series(items: List<BodyAssessment>, measure: Measure): List<Pair<LocalDate, Double>> =
        items.sortedWith(compareBy({ it.date }, { it.id })).mapNotNull { a -> a.values[measure]?.let { a.date to it } }

    /** Snapshot of the two most recent assessments; highlights are weight, body fat and BMI, with or without a value (Home and Progress). */
    fun snapshot(items: List<BodyAssessment>, highlight: List<Measure> = listOf(Measure.WEIGHT, Measure.BODY_FAT, Measure.BMI)): BodySnapshot? {
        val sorted = items.sortedWith(compareByDescending<BodyAssessment> { it.date }.thenByDescending { it.id })
        val latest = sorted.firstOrNull() ?: return null
        val previous = sorted.getOrNull(1)
        val diffs = highlight.map { m ->
            val prev = previous?.values?.get(m)
            val now = latest.values[m]
            // The tile shows the latest value: it's an estimate only if that one is.
            MeasureDiff(m, prev, now, if (prev != null && now != null) round2(now - prev) else null, isEstimate(m, latest, null))
        }
        // A highlight without a current value gets no trend line (its card shows "—").
        val trends = diffs.filter { it.to != null }.associate { d -> d.measure to series(items, d.measure).takeLast(TREND_POINTS) }
        return BodySnapshot(latest, previous, diffs, trends)
    }

    /** How many recent assessments the dashboard trend line shows. */
    const val TREND_POINTS = 12

    private fun isEstimate(m: Measure, a: BodyAssessment?, b: BodyAssessment?) =
        !m.derived && (m in a?.estimated.orEmpty() || m in b?.estimated.orEmpty())

    private fun round2(v: Double) = Math.round(v * 100) / 100.0
}
