package io.github.luannamorim.myfit.ui.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.platform.session.SessionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryExercise(val name: String, val done: Int, val total: Int, val skipped: Boolean, val bestLoadKg: Double?)

data class SessionSummaryUiState(
    val loading: Boolean = true,
    val hasSession: Boolean = false,
    val routineName: String = "",
    val durationMs: Long = 0,
    val setsDone: Int = 0,
    val setsTotal: Int = 0,
    val volumeKg: Double = 0.0,
    val exercises: List<SummaryExercise> = emptyList(),
    val updateLoads: Boolean = true,
    val finishedSessionId: Long? = null,
)

/** Review screen shown before the active session is saved to history. */
@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val controller: SessionController,
    private val time: TimeSource,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val updateLoads = MutableStateFlow<Boolean?>(null)
    private val finished = MutableStateFlow<Long?>(null)
    private val finishing = MutableStateFlow(false)

    val state: StateFlow<SessionSummaryUiState> = combine(controller.active, updateLoads, finished, finishing, settings.settings) { s, update, done, busy, prefs ->
        // While finishing, the active session disappears before the id comes back: keep "loading".
        if (s == null) return@combine SessionSummaryUiState(loading = busy && done == null, hasSession = false, finishedSessionId = done)
        val sets = s.exercises.flatMap { it.sets }
        val doneSets = sets.filter { it.status == ItemStatus.DONE }
        SessionSummaryUiState(
            loading = false,
            hasSession = true,
            routineName = s.routineName,
            durationMs = SessionEngine.elapsedMs(s, time.now()),
            setsDone = doneSets.size,
            setsTotal = sets.size,
            volumeKg = doneSets.sumOf { (it.actualReps ?: 0) * (it.actualLoadKg ?: 0.0) },
            exercises = s.exercises.map { ex ->
                SummaryExercise(
                    name = ex.name,
                    done = ex.doneCount,
                    total = ex.sets.size,
                    skipped = ex.doneCount == 0,
                    bestLoadKg = ex.sets.filter { it.status == ItemStatus.DONE }.mapNotNull { it.actualLoadKg }.maxOrNull(),
                )
            },
            updateLoads = update ?: prefs.updateRoutineLoadsOnFinish,
            finishedSessionId = done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionSummaryUiState())

    fun setUpdateLoads(value: Boolean) {
        updateLoads.value = value
    }

    fun confirm() {
        if (finishing.value) return
        finishing.value = true
        viewModelScope.launch {
            finished.value = controller.finish(state.value.updateLoads)
        }
    }
}
