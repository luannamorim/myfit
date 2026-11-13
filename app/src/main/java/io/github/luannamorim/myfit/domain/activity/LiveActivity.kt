package io.github.luannamorim.myfit.domain.activity

import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.core.time.millisUntil
import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** A GPS position (degrees) at [timeMs] (the fix's own time, wall clock). */
@Serializable
data class GpsFix(val lat: Double, val lon: Double, val timeMs: Long)

/**
 * A sport being timed right now. Like a workout session, time is derived from persisted marks
 * (so it survives the app being killed); [distanceMeters] adds up accepted GPS steps.
 */
@Serializable
data class LiveActivity(
    val sport: Sport,
    val name: String? = null,
    val startedAt: Mark,
    val pausedAt: Mark? = null,
    val totalPausedMs: Long = 0,
    /** Measure distance with GPS (outdoor sports, permission granted). */
    val gps: Boolean = false,
    val distanceMeters: Double = 0.0,
    /** Last accepted position; cleared on pause so the paused stretch never counts. */
    val lastFix: GpsFix? = null,
) {
    val isPaused: Boolean get() = pausedAt != null

    fun elapsedMs(now: Mark): Long = (startedAt.millisUntil(pausedAt ?: now) - totalPausedMs).coerceAtLeast(0)

    fun pause(now: Mark): LiveActivity = if (isPaused) this else copy(pausedAt = now, lastFix = null)

    fun resume(now: Mark): LiveActivity {
        val paused = pausedAt ?: return this
        return copy(pausedAt = null, totalPausedMs = totalPausedMs + paused.millisUntil(now), lastFix = null)
    }

    /** Adds the GPS step to [fix] when it looks real (see [GpsDistance]). */
    fun withFix(fix: GpsFix, accuracyM: Float): LiveActivity = GpsDistance.add(this, fix, accuracyM)
}

/**
 * Turns noisy GPS fixes into distance: imprecise fixes are skipped, tiny steps (standing still)
 * don't count and jumps faster than the sport allows are treated as glitches.
 */
object GpsDistance {
    const val MAX_ACCURACY_M = 30f
    const val MIN_STEP_M = 4.0
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun maxSpeedMps(sport: Sport): Double = when (sport) {
        Sport.WALKING -> 4.0
        Sport.RUNNING -> 9.0
        else -> 25.0
    }

    fun add(activity: LiveActivity, fix: GpsFix, accuracyM: Float): LiveActivity {
        if (!activity.gps || activity.isPaused || accuracyM > MAX_ACCURACY_M) return activity
        val last = activity.lastFix ?: return activity.copy(lastFix = fix)
        val step = metersBetween(last, fix)
        if (step < MIN_STEP_M) return activity
        val seconds = (fix.timeMs - last.timeMs) / 1000.0
        if (seconds <= 0 || step / seconds > maxSpeedMps(activity.sport)) return activity
        return activity.copy(distanceMeters = activity.distanceMeters + step, lastFix = fix)
    }

    /** Great-circle distance (haversine). */
    fun metersBetween(a: GpsFix, b: GpsFix): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }
}
