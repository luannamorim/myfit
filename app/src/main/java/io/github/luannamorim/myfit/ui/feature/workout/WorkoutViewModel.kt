package io.github.luannamorim.myfit.ui.feature.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.domain.session.FinishCheck
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.platform.session.SessionController
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseChip(val name: String, val status: ItemStatus, val isCurrent: Boolean, val done: Int, val total: Int)

data class RestUi(val remainingMs: Long, val progress: Float, val nextName: String?, val nextSet: Int, val nextSetCount: Int)

data class WorkoutUiState(
    val loading: Boolean = true,
    val hasSession: Boolean = false,
    val sessionId: Long = 0,
    val routineName: String = "",
    val elapsedMs: Long = 0,
    val isPaused: Boolean = false,
    val exerciseIndex: Int = 0,
    val exerciseName: String = "",
    val exerciseNotes: String? = null,
    val imagePath: String? = null,
    val seedKey: String? = null,
    val muscleGroup: MuscleGroup = MuscleGroup.OTHER,
    val setNumber: Int = 0,
    val setCount: Int = 0,
    val plannedReps: Int = 0,
    val plannedLoadKg: Double? = null,
    val reps: Int = 0,
    val loadKg: Double? = null,
    val exerciseRestSeconds: Int = 0,
    /** Non-null while a rest is running (remaining > 0). */
    val rest: RestUi? = null,
    /** A rest just finished and no action was taken yet. */
    val restOver: Boolean = false,
    val allDone: Boolean = false,
    val exercises: List<ExerciseChip> = emptyList(),
    val keepScreenOn: Boolean = true,
    val showDiscardPrompt: Boolean = false,
)

/** Values adjusted with the steppers before completing the current set. */
private data class SetDraft(val setId: Long, val reps: Int, val loadKg: Double?)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val controller: SessionController,
    private val time: TimeSource,
    settings: SettingsRepository,
) : ViewModel() {

    /** Immediate refresh after actions; the ticker keeps clocks moving while the screen collects. */
    private val manualNow = MutableStateFlow(time.now())
    private val ticker = flow {
        while (true) {
            emit(time.now())
            delay(TICK_MS)
        }
    }
    private val now = merge(manualNow, ticker)
    private val draft = MutableStateFlow<SetDraft?>(null)
    private val discardPrompt = MutableStateFlow(false)

    private val loaded = controller.active.map { Loaded(it) }

    val state: StateFlow<WorkoutUiState> = combine(loaded, now, draft, discardPrompt, settings.settings) { l, n, d, prompt, prefs ->
        toUi(l.session, n, d, prompt, prefs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    /** Recomputes clocks now (they always derive from persisted marks). */
    fun tick() {
        manualNow.value = time.now()
    }

    fun changeReps(delta: Int) = editDraft { it.copy(reps = (it.reps + delta).coerceIn(0, 200)) }

    fun changeLoad(deltaKg: Double) = editDraft { d ->
        val next = ((d.loadKg ?: 0.0) + deltaKg).coerceAtLeast(0.0)
        d.copy(loadKg = if (next == 0.0) null else next)
    }

    fun setLoad(value: Double?) = editDraft { it.copy(loadKg = value) }

    fun completeSet() = act {
        val s = state.value
        controller.completeSet(s.reps, s.loadKg)
        draft.value = null
    }

    fun skipSet() = act { controller.skipSet(); draft.value = null }
    fun skipExercise() = act { controller.skipExercise(); draft.value = null }
    fun goToExercise(index: Int) = act { controller.goToExercise(index); draft.value = null }
    fun adjustRest(deltaSeconds: Int) = act { controller.adjustRest(deltaSeconds) }
    fun skipRest() = act { controller.skipRest() }
    fun setExerciseRest(seconds: Int) = act { controller.setExerciseRest(state.value.exerciseIndex, seconds) }
    fun togglePause() = act { if (state.value.isPaused) controller.resume() else controller.pause() }

    /** Returns true when the session can go to the summary; otherwise prompts to discard. */
    fun requestFinish(): Boolean {
        val session = currentSession ?: return false
        return if (SessionEngine.checkFinish(session) == FinishCheck.NothingCompleted) {
            discardPrompt.value = true
            false
        } else {
            true
        }
    }

    fun dismissDiscardPrompt() {
        discardPrompt.value = false
    }

    fun discard() = act {
        discardPrompt.value = false
        controller.discard()
    }

    private var currentSession: ActiveSession? = null

    private fun editDraft(transform: (SetDraft) -> SetDraft) {
        val s = state.value
        val set = currentSession?.currentSet ?: return
        draft.update { current ->
            val base = current?.takeIf { it.setId == set.id } ?: SetDraft(set.id, s.reps, s.loadKg)
            transform(base)
        }
    }

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            tick()
        }
    }

    private class Loaded(val session: ActiveSession?)

    private fun toUi(session: ActiveSession?, now: Mark, draft: SetDraft?, prompt: Boolean, prefs: AppSettings): WorkoutUiState {
        currentSession = session
        if (session == null) return WorkoutUiState(loading = false, hasSession = false)
        val exercise = session.currentExercise
        val set = session.currentSet?.takeIf { it.status == ItemStatus.PENDING }
        val activeDraft = draft?.takeIf { it.setId == set?.id }
        val remaining = SessionEngine.restRemainingMs(session, now)
        val allDone = SessionEngine.isComplete(session)
        val rest = if (remaining != null && remaining > 0 && session.rest != null) {
            RestUi(
                remainingMs = remaining,
                progress = remaining.toFloat() / session.rest.durationMs.coerceAtLeast(1),
                nextName = exercise?.name.takeUnless { allDone },
                nextSet = session.cursor.setIndex + 1,
                nextSetCount = exercise?.sets?.size ?: 0,
            )
        } else {
            null
        }
        return WorkoutUiState(
            loading = false,
            hasSession = true,
            sessionId = session.id,
            routineName = session.routineName,
            elapsedMs = SessionEngine.elapsedMs(session, now),
            isPaused = session.isPaused,
            exerciseIndex = session.cursor.exerciseIndex,
            exerciseName = exercise?.name.orEmpty(),
            exerciseNotes = exercise?.notes,
            imagePath = exercise?.imagePath,
            seedKey = exercise?.seedKey,
            muscleGroup = exercise?.muscleGroup ?: MuscleGroup.OTHER,
            setNumber = session.cursor.setIndex + 1,
            setCount = exercise?.sets?.size ?: 0,
            plannedReps = set?.plannedReps ?: 0,
            plannedLoadKg = set?.plannedLoadKg,
            reps = activeDraft?.reps ?: set?.plannedReps ?: 0,
            loadKg = if (activeDraft != null) activeDraft.loadKg else set?.plannedLoadKg,
            exerciseRestSeconds = exercise?.restSeconds ?: 0,
            rest = rest,
            restOver = remaining == 0L,
            allDone = allDone,
            exercises = session.exercises.mapIndexed { i, ex ->
                ExerciseChip(ex.name, ex.status, i == session.cursor.exerciseIndex && !allDone, ex.doneCount, ex.sets.size)
            },
            keepScreenOn = prefs.keepScreenOn,
            showDiscardPrompt = prompt,
        )
    }
}

private const val TICK_MS = 250L
