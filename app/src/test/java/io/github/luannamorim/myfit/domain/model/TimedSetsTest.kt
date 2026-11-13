package io.github.luannamorim.myfit.domain.model

import io.github.luannamorim.myfit.core.format.Durations
import org.junit.Assert.assertEquals
import org.junit.Test

class TimedSetsTest {
    @Test
    fun steps_growWithTheTime() {
        assertEquals(35, TimedSets.step(30, up = true))
        assertEquals(25, TimedSets.step(30, up = false))
        assertEquals(60, TimedSets.step(55, up = true))
        assertEquals(55, TimedSets.step(60, up = false))
        assertEquals(75, TimedSets.step(60, up = true))
        assertEquals(360, TimedSets.step(300, up = true))
        assertEquals(285, TimedSets.step(300, up = false))
        assertEquals(15, TimedSets.step(12, up = true)) // snaps to the step grid
        assertEquals(5, TimedSets.step(5, up = false)) // never below 5 s
    }

    @Test
    fun defaults_andFormatting() {
        assertEquals(30, TimedSets.defaultSeconds(MuscleGroup.ABS))
        assertEquals(600, TimedSets.defaultSeconds(MuscleGroup.CARDIO))
        assertEquals("45 s", Durations.setTime(45))
        assertEquals("1:30 min", Durations.setTime(90))
        assertEquals("10 min", Durations.setTime(600))
    }
}
