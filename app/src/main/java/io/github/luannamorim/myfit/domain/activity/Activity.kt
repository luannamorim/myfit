package io.github.luannamorim.myfit.domain.activity

import io.github.luannamorim.myfit.domain.model.Intensity
import java.time.LocalDate

/** How a sport's distance is typed and shown (stored as km either way). */
enum class DistanceUnit { KM, METERS }

/** Sports that can be logged besides gym workouts, in picker order. [distance] is null when distance makes no sense. */
enum class Sport(val distance: DistanceUnit? = null) {
    SWIMMING(DistanceUnit.METERS),
    CYCLING(DistanceUnit.KM),
    INDOOR_CYCLING(DistanceUnit.KM),
    RUNNING(DistanceUnit.KM),
    TREADMILL(DistanceUnit.KM),
    WALKING(DistanceUnit.KM),
    SOCCER,
    VOLLEYBALL,
    BASKETBALL,
    BEACH_TENNIS,
    MUAY_THAI,
    JIU_JITSU,
    BOXING,
    YOGA,
    PILATES,
    STRETCHING,

    /** A gym session logged as an activity (no routine). */
    GYM,

    /** "Free": the user names it. */
    OTHER(DistanceUnit.KM),
    ;

    /** Outdoor sports whose distance the phone's GPS can measure while the activity runs. */
    val gpsCapable: Boolean get() = this in GPS_SPORTS

    private companion object {
        val GPS_SPORTS by lazy { setOf(CYCLING, RUNNING, WALKING, OTHER) }
    }
}

/** A sport logged after the fact: when, how long, optionally how far and how hard. */
data class Activity(
    val id: Long = 0,
    val sport: Sport,
    /** The user's name for [Sport.OTHER]; ignored for the other sports. */
    val name: String? = null,
    val date: LocalDate,
    val durationMinutes: Int,
    val distanceKm: Double? = null,
    val intensity: Intensity? = null,
    val notes: String? = null,
    val imagePath: String? = null,
) {
    val durationMs: Long get() = durationMinutes * 60_000L
}
