package io.github.luannamorim.myfit.core.time

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkTest {

    private val start = Mark(wallMs = 1_700_000_000_000, elapsedMs = 5_000_000, bootCount = 7)

    @Test
    fun sameBoot_usesElapsed_evenIfWallJumpsBack() {
        val later = Mark(wallMs = start.wallMs - 3_600_000, elapsedMs = start.elapsedMs + 60_000, bootCount = 7)
        assertEquals(60_000, start.millisUntil(later))
    }

    @Test
    fun differentBoot_usesWall() {
        val afterReboot = Mark(wallMs = start.wallMs + 120_000, elapsedMs = 1_000, bootCount = 8)
        assertEquals(120_000, start.millisUntil(afterReboot))
    }

    @Test
    fun neverNegative() {
        val afterRebootClockBehind = Mark(wallMs = start.wallMs - 10_000, elapsedMs = 1_000, bootCount = 8)
        assertEquals(0, start.millisUntil(afterRebootClockBehind))
    }

    @Test
    fun fakeTimeSource_advanceAndReboot() {
        val time = FakeTimeSource(start)
        time.advance(30_000)
        assertEquals(30_000, start.millisUntil(time.now()))
        time.jumpWall(-600_000)
        assertEquals(30_000, start.millisUntil(time.now()))
        time.reboot(wallMs = start.wallMs + 90_000)
        assertEquals(90_000, start.millisUntil(time.now()))
    }
}
