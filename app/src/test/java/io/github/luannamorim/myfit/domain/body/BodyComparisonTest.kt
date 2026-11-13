package io.github.luannamorim.myfit.domain.body

import io.github.luannamorim.myfit.core.format.formatSigned
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BodyComparisonTest {
    @get:org.junit.Rule val locale = io.github.luannamorim.myfit.testutil.LocaleRule()

    private val d1 = LocalDate.of(2026, 1, 10)
    private val d2 = LocalDate.of(2026, 4, 10)
    private val first = BodyAssessment(1, d1, mapOf(Measure.WEIGHT to 71.0, Measure.CHEST to 92.0, Measure.WAIST to 78.0, Measure.ARM_RIGHT to 29.0))
    private val current = BodyAssessment(2, d2, mapOf(Measure.WEIGHT to 73.0, Measure.CHEST to 96.0, Measure.WAIST to 79.0, Measure.ARM_RIGHT to 31.0, Measure.BODY_FAT to 15.5))

    @Test
    fun compare_diffIsToMinusFrom() {
        val diffs = BodyComparison.compare(first, current).associateBy { it.measure }
        assertEquals(2.0, diffs[Measure.WEIGHT]!!.diff!!, 0.0)
        assertEquals(4.0, diffs[Measure.CHEST]!!.diff!!, 0.0)
        assertEquals(1.0, diffs[Measure.WAIST]!!.diff!!, 0.0)
        assertEquals(2.0, diffs[Measure.ARM_RIGHT]!!.diff!!, 0.0)
        assertEquals(Measure.entries.size, diffs.size)
    }

    @Test
    fun compare_missingValue_diffNull() {
        val fat = BodyComparison.compare(first, current).single { it.measure == Measure.BODY_FAT }
        assertNull(fat.from)
        assertEquals(15.5, fat.to!!, 0.0)
        assertNull(fat.diff)
    }

    @Test
    fun series_sortedByDate_skipsMissing() {
        val noWeight = BodyAssessment(3, LocalDate.of(2026, 2, 1), mapOf(Measure.WAIST to 77.0))
        val series = BodyComparison.series(listOf(current, noWeight, first), Measure.WEIGHT)
        assertEquals(listOf(d1 to 71.0, d2 to 73.0), series)
    }

    @Test
    fun snapshot_usesLatestTwo() {
        val snap = BodyComparison.snapshot(listOf(first, current))!!
        assertEquals(2L, snap.latest.id)
        assertEquals(1L, snap.previous!!.id)
        // Weight, body fat and BMI always (waist left out on purpose); BMI has no value here (no height).
        assertEquals(listOf(Measure.WEIGHT, Measure.BODY_FAT, Measure.BMI), snap.highlights.map { it.measure })
        assertNull(snap.highlights[2].to)
        assertEquals(emptyList<Pair<LocalDate, Double>>(), snap.trends[Measure.BMI].orEmpty())
        assertEquals(listOf(71.0, 73.0), snap.trends.getValue(Measure.WEIGHT).map { it.second }) // oldest first
        assertEquals(listOf(15.5), snap.trends.getValue(Measure.BODY_FAT).map { it.second })
        assertEquals(2.0, snap.highlights[0].diff!!, 0.0)
        assertNull(snap.highlights[1].diff)
        assertNull(BodyComparison.snapshot(emptyList()))
    }

    @Test
    fun snapshot_highlightsBmi_andMarksEstimates() {
        val derived = BodyMetrics.derive(
            listOf(
                first.copy(values = first.values + (Measure.HEIGHT to 178.0)),
                current.copy(values = current.values - Measure.BODY_FAT + mapOf(Measure.NECK to 38.0, Measure.ABDOMEN to 85.0)),
            ),
            Sex.MALE,
        )
        val snap = BodyComparison.snapshot(derived)!!
        assertEquals(listOf(Measure.WEIGHT, Measure.BODY_FAT, Measure.BMI), snap.highlights.map { it.measure })
        assertEquals(listOf(false, true, false), snap.highlights.map { it.estimated }) // BMI is always calculated: not flagged

        val fat = BodyComparison.compare(derived[0], derived[1]).single { it.measure == Measure.BODY_FAT }
        assertTrue(fat.estimated)
        assertFalse(BodyComparison.compare(first, current).single { it.measure == Measure.BODY_FAT }.estimated)
    }

    @Test
    fun snapshot_typedLatest_isNotMarkedEstimated() {
        // Previous body fat estimated, latest typed (e.g. a bioimpedance scale): the tile shows a measurement.
        val derived = BodyMetrics.derive(
            listOf(
                first.copy(values = first.values + mapOf(Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0)),
                current,
            ),
            Sex.MALE,
        )
        assertTrue(Measure.BODY_FAT in derived[0].estimated)
        val fat = BodyComparison.snapshot(derived)!!.highlights.single { it.measure == Measure.BODY_FAT }
        assertEquals(15.5, fat.to!!, 0.0)
        assertFalse(fat.estimated)
        assertTrue(BodyComparison.compare(derived[0], derived[1]).single { it.measure == Measure.BODY_FAT }.estimated) // Compare: either side
    }

    @Test
    fun snapshot_emptyHighlight_hasNoTrend() {
        // Two weigh-ins, then a tape-measure-only assessment: the weight card is "—", with no trend line.
        val items = listOf(
            BodyAssessment(1, d1, mapOf(Measure.WEIGHT to 71.0)),
            BodyAssessment(2, LocalDate.of(2026, 2, 10), mapOf(Measure.WEIGHT to 72.0)),
            BodyAssessment(3, d2, mapOf(Measure.WAIST to 80.0)),
        )
        val snap = BodyComparison.snapshot(items)!!
        assertNull(snap.highlights.single { it.measure == Measure.WEIGHT }.to)
        assertEquals(emptyList<Pair<LocalDate, Double>>(), snap.trends[Measure.WEIGHT].orEmpty())
    }

    @Test
    fun diffFormat_signed() {
        assertEquals("+2 kg", formatSigned(2.0, "kg"))
        assertEquals("−1,5 cm", formatSigned(-1.5, "cm"))
        assertEquals("0 cm", formatSigned(0.0, "cm"))
    }
}
