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

    private fun cues(targetSeconds: Int, countdown: Boolean = true, take: Int = 20) =
        TimedSets.holdCues(targetSeconds * 1000L, countdown).take(take).map { (ms, cue) -> ms / 1000 to cue }.toList()

    @Test
    fun holdCues_secondsTarget_countdownAndTargetOnly() {
        assertEquals(
            listOf(42L to HoldCue.COUNTDOWN, 43L to HoldCue.COUNTDOWN, 44L to HoldCue.COUNTDOWN, 45L to HoldCue.TARGET),
            cues(45),
        )
        assertEquals(listOf(45L to HoldCue.TARGET), cues(45, countdown = false))
        assertEquals(listOf(1L to HoldCue.COUNTDOWN, 2L to HoldCue.TARGET), cues(2)) // no countdown before the start
    }

    @Test
    fun holdCues_minutesTarget_beepsEveryMinute_alsoPastTheTarget() {
        assertEquals(
            listOf(
                60L to HoldCue.MINUTE, 120L to HoldCue.MINUTE,
                177L to HoldCue.COUNTDOWN, 178L to HoldCue.COUNTDOWN, 179L to HoldCue.COUNTDOWN, 180L to HoldCue.TARGET, // not doubled at 3:00
                240L to HoldCue.MINUTE, 300L to HoldCue.MINUTE,
            ),
            cues(180, take = 8),
        )
        // 1:30: one minute beep before the countdown; the target itself is the 1:30 sound.
        assertEquals(
            listOf(60L to HoldCue.MINUTE, 87L to HoldCue.COUNTDOWN, 88L to HoldCue.COUNTDOWN, 89L to HoldCue.COUNTDOWN, 90L to HoldCue.TARGET, 120L to HoldCue.MINUTE),
            cues(90, take = 6),
        )
        // A minute on a countdown second isn't doubled (2:01 target: 2:00 is the last-second beep).
        assertEquals(1, cues(121, take = 6).count { it.first == 120L })
        assertEquals(listOf(60L to HoldCue.TARGET, 120L to HoldCue.MINUTE), cues(60, countdown = false, take = 2))
    }
}
