package io.github.luannamorim.myfit.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatsTest {
    @get:org.junit.Rule val locale = io.github.luannamorim.myfit.testutil.LocaleRule()

    @Test
    fun decimalInput_parse() {
        assertEquals(42.5, DecimalInput.parse("42,5")!!, 0.0)
        assertEquals(42.5, DecimalInput.parse("42.5")!!, 0.0)
        assertEquals(40.0, DecimalInput.parse(" 40 ")!!, 0.0)
        assertEquals(12.25, DecimalInput.parse("12,25")!!, 0.0)
        assertNull(DecimalInput.parse("abc"))
        assertNull(DecimalInput.parse("-5"))
        assertNull(DecimalInput.parse(""))
        assertNull(DecimalInput.parse("1,234"))
    }

    @Test
    fun decimalInput_format() {
        assertEquals("42,5", DecimalInput.format(42.5))
        assertEquals("40", DecimalInput.format(40.0))
        assertEquals("12,25", DecimalInput.format(12.25))
        assertEquals("", DecimalInput.format(null))
    }

    @Test
    fun thousands() {
        assertEquals("4.870", formatThousands(4870.0))
        assertEquals("12", formatThousands(12.0))
    }

    @Test
    fun english_usesDotAndEnglishDates() {
        java.util.Locale.setDefault(java.util.Locale.US)
        assertEquals("42.5", DecimalInput.format(42.5))
        assertEquals(42.5, DecimalInput.parse("42,5")!!, 0.0) // input still accepts both
        assertEquals("4,870", formatThousands(4870.0))
        val date = java.time.LocalDate.of(2026, 11, 12)
        assertEquals("Thursday, November 12", Dates.long(date))
        assertEquals("Nov 12, 2026", Dates.medium(date))
        assertEquals("November 2026", Dates.monthYear(2026, 11))
    }

    @Test
    fun portuguese_dates() {
        val date = java.time.LocalDate.of(2026, 11, 12)
        assertEquals("quinta-feira, 12 de novembro", Dates.long(date))
        assertEquals("Novembro 2026", Dates.monthYear(2026, 11))
        assertEquals("12/11", Dates.axis(date))
    }

    @Test
    fun durations() {
        assertEquals("1:30", Durations.clock(90_000))
        assertEquals("1:02:45", Durations.clock(3_765_000))
        assertEquals("0:01", Durations.countdown(1))
        assertEquals("1:00", Durations.countdown(60_000))
        assertEquals("1 h 05 min", Durations.human(3_900_000))
        assertEquals("45 min", Durations.human(2_700_000))
        assertEquals("45 s", Durations.rest(45))
        assertEquals("90 s", Durations.rest(90))
        assertEquals("150 s", Durations.rest(150))
        assertEquals("3 min", Durations.rest(180))
        assertEquals("3 min 30 s", Durations.rest(210))
    }
}
