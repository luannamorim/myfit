package io.github.luannamorim.myfit.domain.activity

import io.github.luannamorim.myfit.core.time.FakeTimeSource
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveActivityTest {
    private val time = FakeTimeSource()
    private fun run(gps: Boolean = true, sport: Sport = Sport.RUNNING) = LiveActivity(sport = sport, startedAt = time.now(), gps = gps)

    /** A fix [metersNorth] north of the start point, [seconds] after it. */
    private fun fix(metersNorth: Double, seconds: Long) = GpsFix(-23.55 + metersNorth / 111_195.0, -46.63, 1_000_000L + seconds * 1000)

    @Test
    fun elapsed_excludesPauses() {
        var a = run()
        time.advance(60_000)
        a = a.pause(time.now())
        time.advance(30_000)
        assertEquals(60_000, a.elapsedMs(time.now())) // frozen while paused
        a = a.resume(time.now())
        time.advance(15_000)
        assertEquals(75_000, a.elapsedMs(time.now()))
    }

    @Test
    fun distance_addsRealSteps_andSkipsNoiseAndJumps() {
        var a = run()
        a = a.withFix(fix(0.0, 0), 8f) // first fix only anchors
        a = a.withFix(fix(10.0, 4), 8f) // 10 m in 4 s: counts
        assertEquals(10.0, a.distanceMeters, 0.2)
        a = a.withFix(fix(12.0, 6), 8f) // 2 m: standing still, ignored
        a = a.withFix(fix(30.0, 8), 50f) // imprecise fix, ignored
        a = a.withFix(fix(300.0, 9), 8f) // 290 m in 3 s while running: a glitch
        assertEquals(10.0, a.distanceMeters, 0.2)
        a = a.withFix(fix(30.0, 12), 8f) // 20 m in 8 s from the last good fix
        assertEquals(30.0, a.distanceMeters, 0.3)
    }

    @Test
    fun distance_ignoresPausedStretch_andActivitiesWithoutGps() {
        var a = run()
        a = a.withFix(fix(0.0, 0), 5f).withFix(fix(20.0, 8), 5f)
        a = a.pause(time.now())
        a = a.withFix(fix(500.0, 400), 5f) // walked home while paused
        a = a.resume(time.now())
        a = a.withFix(fix(500.0, 410), 5f).withFix(fix(520.0, 418), 5f) // resumes from a new anchor
        assertEquals(40.0, a.distanceMeters, 0.5)

        val noGps = run(gps = false).withFix(fix(0.0, 0), 5f).withFix(fix(50.0, 20), 5f)
        assertEquals(0.0, noGps.distanceMeters, 0.0)
    }

    @Test
    fun haversine_matchesKnownDistance() {
        // One degree of latitude is about 111.2 km.
        assertEquals(111_195.0, GpsDistance.metersBetween(GpsFix(0.0, 0.0, 0), GpsFix(1.0, 0.0, 0)), 50.0)
    }

    @Test
    fun onlyOutdoorSportsUseGps() {
        assertEquals(setOf(Sport.CYCLING, Sport.RUNNING, Sport.WALKING, Sport.OTHER), Sport.entries.filter { it.gpsCapable }.toSet())
    }
}
