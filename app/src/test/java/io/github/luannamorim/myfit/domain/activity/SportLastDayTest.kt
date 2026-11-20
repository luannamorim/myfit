package io.github.luannamorim.myfit.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SportLastDayTest {
    private val days = listOf(
        SportLastDay(Sport.SWIMMING, null, 10),
        SportLastDay(Sport.SWIMMING, null, 14),
        SportLastDay(Sport.OTHER, "Beach vôlei", 12),
        SportLastDay(Sport.OTHER, "Escalada", 20),
    )

    @Test
    fun aSport_takesItsLatestDay_whateverTheName() {
        assertEquals(14L, days.lastDay(Sport.SWIMMING, null))
        assertEquals(14L, days.lastDay(Sport.SWIMMING, "ignored"))
    }

    @Test
    fun other_matchesTheName_ignoringCaseAndSpaces() {
        assertEquals(12L, days.lastDay(Sport.OTHER, " beach Vôlei "))
        assertEquals(20L, days.lastDay(Sport.OTHER, "escalada"))
        assertNull(days.lastDay(Sport.OTHER, "Surf"))
        assertNull(days.lastDay(Sport.OTHER, null))
    }

    @Test
    fun neverLogged_isNull() {
        assertNull(days.lastDay(Sport.RUNNING, null))
    }
}
