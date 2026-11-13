package io.github.luannamorim.myfit.domain.body

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyMetricsTest {
    private val jan = LocalDate.of(2026, 1, 10)
    private val apr = LocalDate.of(2026, 4, 10)
    private val jun = LocalDate.of(2026, 6, 10)
    private val jul = LocalDate.of(2026, 7, 10)

    @Test
    fun bmi_isWeightOverHeightSquared() = assertEquals(23.1, BodyMetrics.bmi(73.25, 178.0), 0.0)

    @Test
    fun navy_male_referenceValue() = assertEquals(16.4, BodyMetrics.navyBodyFat(Sex.MALE, 178.0, 38.0, 85.0, null)!!, 0.0)

    @Test
    fun navy_female_referenceValue() = assertEquals(25.9, BodyMetrics.navyBodyFat(Sex.FEMALE, 165.0, 33.0, 70.0, 98.0)!!, 0.0)

    @Test
    fun navy_nonsense_isNull() {
        assertNull(BodyMetrics.navyBodyFat(Sex.MALE, 178.0, 38.0, 38.0, null)) // log(0)
        assertNull(BodyMetrics.navyBodyFat(Sex.MALE, 178.0, 38.0, 8.5, null)) // typo: negative argument
        assertNull(BodyMetrics.navyBodyFat(Sex.FEMALE, 165.0, 33.0, 70.0, null)) // hips missing
        assertNull(BodyMetrics.navyBodyFat(Sex.MALE, 178.0, 38.0, 38.5, null)) // result below 2 %
    }

    @Test
    fun derive_heightCarriesOver_fromEarlierThenLater() {
        val a = BodyAssessment(1, jan, mapOf(Measure.WEIGHT to 71.0))
        val b = BodyAssessment(2, apr, mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0))
        val c = BodyAssessment(3, jun, mapOf(Measure.WEIGHT to 72.0))
        val d = BodyAssessment(4, jul, mapOf(Measure.WEIGHT to 72.0, Measure.HEIGHT to 180.0))
        val derived = BodyMetrics.derive(listOf(d, c, b, a), sex = null)
        assertEquals(listOf(4L, 3L, 2L, 1L), derived.map { it.id }) // order kept
        val bmi = derived.associate { it.id to it.values[Measure.BMI] }
        assertEquals(22.4, bmi[1]!!, 0.0) // first height recorded after it
        assertEquals(23.1, bmi[2]!!, 0.0)
        assertEquals(22.7, bmi[3]!!, 0.0) // latest height recorded before it
        assertEquals(22.2, bmi[4]!!, 0.0) // its own height
        assertEquals(setOf(Measure.BMI), derived.first().estimated)
        assertFalse(Measure.HEIGHT in derived.single { it.id == 1L }.values) // height isn't copied, only used
    }

    @Test
    fun derive_ignoresImplausibleHeights() {
        val typo = BodyAssessment(2, apr, mapOf(Measure.WEIGHT to 73.0, Measure.HEIGHT to 17.8))
        assertNull(BodyMetrics.derive(listOf(typo), sex = null).single().values[Measure.BMI])
        // A bad height is treated as missing: the assessment borrows a good one instead.
        val good = BodyAssessment(1, jan, mapOf(Measure.WEIGHT to 71.0, Measure.HEIGHT to 178.0))
        assertEquals(23.0, BodyMetrics.derive(listOf(typo, good), sex = null).first().values[Measure.BMI]!!, 0.0)
    }

    @Test
    fun derive_estimatesBodyFat_onlyWithSexAndCircumferences() {
        val full = BodyAssessment(1, apr, mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0))
        val noSex = BodyMetrics.derive(listOf(full), sex = null).single()
        assertEquals(23.1, noSex.values[Measure.BMI]!!, 0.0)
        assertNull(noSex.values[Measure.BODY_FAT])

        val male = BodyMetrics.derive(listOf(full), Sex.MALE).single()
        assertEquals(16.4, male.values[Measure.BODY_FAT]!!, 0.0)
        assertEquals(setOf(Measure.BMI, Measure.BODY_FAT), male.estimated)

        val waistOnly = full.copy(values = full.values - Measure.ABDOMEN + (Measure.WAIST to 79.0))
        assertEquals(11.5, BodyMetrics.derive(listOf(waistOnly), Sex.MALE).single().values[Measure.BODY_FAT]!!, 0.0)

        val noNeck = full.copy(values = full.values - Measure.NECK)
        assertNull(BodyMetrics.derive(listOf(noNeck), Sex.MALE).single().values[Measure.BODY_FAT])
    }

    @Test
    fun derive_typedBodyFatWins() {
        val typed = BodyAssessment(
            1, apr,
            mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0, Measure.BODY_FAT to 20.0),
        )
        val derived = BodyMetrics.derive(listOf(typed), Sex.MALE).single()
        assertEquals(20.0, derived.values[Measure.BODY_FAT]!!, 0.0)
        assertEquals(setOf(Measure.BMI), derived.estimated)
    }

    private fun statuses(measure: Measure, sex: Sex?, vararg values: Double) = values.map { BodyMetrics.status(measure, it, sex) }

    @Test
    fun status_bmi_boundaries() {
        val (P, C, G) = Triple(BodyStatus.POOR, BodyStatus.CAUTION, BodyStatus.GOOD)
        assertEquals(listOf(P, C, C, G, G, C, C, P), statuses(Measure.BMI, null, 15.9, 16.0, 18.4, 18.5, 24.9, 25.0, 29.9, 30.0))
        assertEquals(G, BodyMetrics.status(Measure.BMI, 22.0, Sex.FEMALE)) // sex doesn't matter for BMI
    }

    @Test
    fun status_bodyFat_men_boundaries() {
        val (P, C, G) = Triple(BodyStatus.POOR, BodyStatus.CAUTION, BodyStatus.GOOD)
        assertEquals(listOf(P, C, C, G, G, C, C, P), statuses(Measure.BODY_FAT, Sex.MALE, 1.9, 2.0, 5.9, 6.0, 17.9, 18.0, 24.9, 25.0))
    }

    @Test
    fun status_bodyFat_women_boundaries() {
        val (P, C, G) = Triple(BodyStatus.POOR, BodyStatus.CAUTION, BodyStatus.GOOD)
        assertEquals(listOf(P, C, C, G, G, C, C, P), statuses(Measure.BODY_FAT, Sex.FEMALE, 9.9, 10.0, 13.9, 14.0, 24.9, 25.0, 31.9, 32.0))
    }

    @Test
    fun status_bodyFatWithoutSex_andOtherMeasures_areNull() {
        assertNull(BodyMetrics.status(Measure.BODY_FAT, 16.0, null))
        assertNull(BodyMetrics.status(Measure.WEIGHT, 73.0, Sex.MALE))
        assertNull(BodyMetrics.status(Measure.WAIST, 80.0, Sex.MALE))
    }

    @Test
    fun withStatus_fillsHighlights() {
        val a = BodyAssessment(1, apr, mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0))
        val snap = BodyMetrics.withStatus(BodyComparison.snapshot(BodyMetrics.derive(listOf(a), Sex.MALE)), Sex.MALE)!!
        assertEquals(
            mapOf(Measure.WEIGHT to null, Measure.BODY_FAT to BodyStatus.GOOD, Measure.BMI to BodyStatus.GOOD),
            snap.highlights.associate { it.measure to it.status },
        )
        assertNull(BodyMetrics.withStatus(null, Sex.MALE))
    }
}
