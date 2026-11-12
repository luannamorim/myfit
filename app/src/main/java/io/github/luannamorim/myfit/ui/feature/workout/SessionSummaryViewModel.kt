package io.github.luannamorim.myfit.ui.feature.workout

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.platform.session.SessionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val intensity: Intensity? = null,
    val imagePath: String? = null,
    val photoError: Boolean = false,
    val finishedSessionId: Long? = null,
)

/** What the user picked on the summary screen before saving. */
private data class Choices(val updateLoads: Boolean? = null, val intensity: Intensity? = null, val imagePath: String? = null, val photoError: Boolean = false)

/** Review screen shown before the active session is saved to history. */
@HiltViewModel
class SessionSummaryViewModel @Inject constructor(
    private val controller: SessionController,
    private val time: TimeSource,
    private val settings: SettingsRepository,
    private val images: ImageStore,
) : ViewModel() {
    private val choices = MutableStateFlow(Choices())
    private val finished = MutableStateFlow<Long?>(null)
    private val finishing = MutableStateFlow(false)

    val state: StateFlow<SessionSummaryUiState> = combine(controller.active, choices, finished, finishing, settings.settings) { s, picked, done, busy, prefs ->
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
            updateLoads = picked.updateLoads ?: prefs.updateRoutineLoadsOnFinish,
            intensity = picked.intensity,
            imagePath = picked.imagePath,
            photoError = picked.photoError,
            finishedSessionId = done,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionSummaryUiState())

    fun setUpdateLoads(value: Boolean) = choices.update { it.copy(updateLoads = value) }

    fun setIntensity(value: Intensity?) = choices.update { it.copy(intensity = value) }

    /** Imports (and shrinks) the picked photo right away; the previous pick is deleted. */
    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = runCatching { images.import(uri, ImageKind.SESSION) }.getOrNull()
            val previous = choices.value.imagePath
            choices.update { if (path == null) it.copy(photoError = true) else it.copy(imagePath = path, photoError = false) }
            if (path != null) images.delete(previous)
        }
    }

    fun removePhoto() {
        val previous = choices.value.imagePath
        choices.update { it.copy(imagePath = null, photoError = false) }
        viewModelScope.launch { images.delete(previous) }
    }

    fun confirm() {
        if (finishing.value) return
        finishing.value = true
        val picked = state.value
        viewModelScope.launch {
            finished.value = controller.finish(picked.updateLoads, picked.intensity, picked.imagePath)
        }
    }
}
