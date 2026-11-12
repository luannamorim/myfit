package io.github.luannamorim.myfit.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.repository.ActiveSessionExistsException
import io.github.luannamorim.myfit.data.repository.EmptyRoutineException
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.platform.session.SessionController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppEvent {
    data object OpenWorkout : AppEvent
}

/** App-wide state: the in-progress session (for the "Retomar" bar) and starting workouts. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val controller: SessionController,
    settings: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeSession: StateFlow<ActiveSession?> = controller.active
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _events = Channel<AppEvent>(Channel.BUFFERED)
    val events: Flow<AppEvent> = _events.receiveAsFlow()

    fun startRoutine(routineId: Long) {
        viewModelScope.launch {
            try {
                controller.start(routineId)
            } catch (_: ActiveSessionExistsException) {
                // Resume the one in progress instead.
            } catch (_: EmptyRoutineException) {
                return@launch
            }
            _events.send(AppEvent.OpenWorkout)
        }
    }

    /** Re-arms the rest alarm and the foreground service for an in-progress session. */
    fun resync() {
        viewModelScope.launch { controller.resync() }
    }
}
