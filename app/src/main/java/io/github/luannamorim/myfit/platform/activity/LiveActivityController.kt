package io.github.luannamorim.myfit.platform.activity

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.LiveActivityStore
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.di.ApplicationScope
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.GpsFix
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.roundToLong

/** What the GPS is doing for the running activity (shown on its screen). */
enum class GpsStatus { OFF, SEARCHING, OK, DISABLED }

/**
 * Single entry point for a sport timed live: start, pause/resume, GPS fixes and finish (which
 * turns it into a logged [Activity]). State lives in memory and in [LiveActivityStore].
 */
@Singleton
class LiveActivityController @Inject constructor(
    private val store: LiveActivityStore,
    private val activities: ActivityRepository,
    private val time: TimeSource,
    private val service: ActivityServiceStarter,
    @ApplicationScope scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val _live = MutableStateFlow<LiveActivity?>(null)
    val live: StateFlow<LiveActivity?> = _live.asStateFlow()

    private val _gps = MutableStateFlow(GpsStatus.OFF)
    val gps: StateFlow<GpsStatus> = _gps.asStateFlow()

    /** Wall time of the last write, to keep GPS updates from writing to disk every second. */
    private var lastWriteMs = 0L

    init {
        scope.launch {
            _live.value = store.read()
            loaded.complete(Unit)
        }
    }

    suspend fun current(): LiveActivity? {
        loaded.await()
        return _live.value
    }

    /** Starts timing [sport] (an activity already running is kept as it is). */
    suspend fun start(sport: Sport, name: String?, gps: Boolean): LiveActivity = update(force = true) { existing ->
        existing ?: LiveActivity(sport = sport, name = name?.trim()?.takeIf { it.isNotEmpty() }, startedAt = time.now(), gps = gps && sport.gpsCapable)
    }!!.also {
        _gps.value = if (it.gps) GpsStatus.SEARCHING else GpsStatus.OFF
        service.start()
    }

    suspend fun pause() = update(force = true) { it?.pause(time.now()) }

    suspend fun resume() = update(force = true) { it?.resume(time.now()) }

    suspend fun togglePause() = update(force = true) { it?.let { a -> if (a.isPaused) a.resume(time.now()) else a.pause(time.now()) } }

    /** A position from the service; the distance is written to disk at most every [WRITE_EVERY_MS]. */
    suspend fun onFix(fix: GpsFix, accuracyM: Float) {
        if (accuracyM <= io.github.luannamorim.myfit.domain.activity.GpsDistance.MAX_ACCURACY_M) _gps.value = GpsStatus.OK
        update(force = false) { it?.withFix(fix, accuracyM) }
    }

    fun onGpsStatus(status: GpsStatus) {
        _gps.value = status
    }

    /**
     * Logs the activity (time and GPS distance, plus what the "activity done" step asked) and
     * stops timing. Returns the new activity's id.
     */
    suspend fun finish(intensity: Intensity? = null, notes: String? = null, imagePath: String? = null): Long? {
        val done = mutex.withLock {
            loaded.await()
            val current = _live.value ?: return null
            _live.value = null
            store.write(null)
            current
        }
        service.stop()
        _gps.value = GpsStatus.OFF
        val minutes = minutes(done)
        val km = km(done)
        val date = Instant.ofEpochMilli(done.startedAt.wallMs).atZone(time.zone()).toLocalDate()
        return activities.save(
            Activity(
                sport = done.sport, name = done.name, date = date, durationMinutes = minutes, distanceKm = km,
                intensity = intensity, notes = notes, imagePath = imagePath,
            ),
        )
    }

    /** Whole minutes timed so far (rounded up, at least 1): what [finish] will log. */
    fun minutes(activity: LiveActivity): Int = ceil(activity.elapsedMs(time.now()) / 60_000.0).toInt().coerceAtLeast(1)

    /** GPS distance in km with two decimals, or null without GPS (or under 10 m). */
    fun km(activity: LiveActivity): Double? =
        if (activity.gps && activity.distanceMeters >= 10) (activity.distanceMeters / 10).roundToLong() / 100.0 else null

    /** Local date the activity started on. */
    fun startDate(activity: LiveActivity) = Instant.ofEpochMilli(activity.startedAt.wallMs).atZone(time.zone()).toLocalDate()

    suspend fun discard() {
        mutex.withLock {
            loaded.await()
            _live.value = null
            store.write(null)
        }
        service.stop()
        _gps.value = GpsStatus.OFF
    }

    /** Brings the service back (app start, after the process was killed). */
    suspend fun resync() {
        if (current() != null) service.start()
    }

    private suspend fun update(force: Boolean, op: (LiveActivity?) -> LiveActivity?): LiveActivity? = mutex.withLock {
        loaded.await()
        val before = _live.value ?: if (force) null else return@withLock null
        val next = op(before)
        if (next == before && !force) return@withLock next
        _live.value = next
        val now = time.now().wallMs
        if (force || now - lastWriteMs >= WRITE_EVERY_MS) {
            store.write(next)
            lastWriteMs = now
        }
        next
    }

    private companion object {
        const val WRITE_EVERY_MS = 10_000L
    }
}
