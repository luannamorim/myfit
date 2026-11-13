package io.github.luannamorim.myfit.ui.feature.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.platform.activity.GpsStatus
import io.github.luannamorim.myfit.platform.activity.LiveActivityController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LiveActivityUiState(
    val loading: Boolean = true,
    /** False once there is no activity running (finished, discarded or never started). */
    val running: Boolean = false,
    val sport: Sport = Sport.RUNNING,
    val name: String? = null,
    val elapsedMs: Long = 0,
    val paused: Boolean = false,
    val gps: Boolean = false,
    val gpsStatus: GpsStatus = GpsStatus.OFF,
    val distanceMeters: Double = 0.0,
    val finishedId: Long? = null,
)

/** The sport being timed: clock, GPS distance, pause/resume and finish. */
@HiltViewModel
class LiveActivityViewModel @Inject constructor(
    private val controller: LiveActivityController,
    private val time: TimeSource,
) : ViewModel() {
    private val finished = MutableStateFlow<Long?>(null)
    private val ticker = flow {
        while (true) {
            emit(time.now())
            delay(TICK_MS)
        }
    }
    private val live = flow<LiveActivity?> {
        emit(controller.current())
        emitAll(controller.live)
    }

    val state: StateFlow<LiveActivityUiState> = combine(live, controller.gps, ticker, finished) { a, gps, now, done ->
        if (a == null) {
            LiveActivityUiState(loading = false, running = false, finishedId = done)
        } else {
            LiveActivityUiState(
                loading = false,
                running = true,
                sport = a.sport,
                name = a.name,
                elapsedMs = a.elapsedMs(now),
                paused = a.isPaused,
                gps = a.gps,
                gpsStatus = gps,
                distanceMeters = a.distanceMeters,
                finishedId = done,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveActivityUiState())

    fun togglePause() {
        viewModelScope.launch { controller.togglePause() }
    }

    fun finish() {
        viewModelScope.launch { finished.value = controller.finish() }
    }

    fun discard() {
        viewModelScope.launch { controller.discard() }
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
