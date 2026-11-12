package io.github.luannamorim.myfit.ui.feature.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.ExerciseRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.Routine
import io.github.luannamorim.myfit.domain.model.RoutineItem
import io.github.luannamorim.myfit.ui.navigation.RoutineEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DraftItem(
    /** Stable key for list animations/reordering (not persisted). */
    val key: Long,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val loadKg: Double?,
    val notes: String?,
)

data class RoutineEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val description: String = "",
    val items: List<DraftItem> = emptyList(),
    val nameError: Boolean = false,
    val dirty: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routines: RoutineRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val routineId: Long? = savedStateHandle.toRoute<RoutineEditorRoute>().routineId
    private val _state = MutableStateFlow(RoutineEditorUiState(isNew = routineId == null))
    val state: StateFlow<RoutineEditorUiState> = _state.asStateFlow()
    private var nextKey = 1L

    init {
        viewModelScope.launch {
            val routine = routineId?.let { routines.get(it) }
            _state.update {
                if (routine == null) {
                    it.copy(isLoading = false)
                } else {
                    it.copy(
                        isLoading = false,
                        name = routine.name,
                        description = routine.description.orEmpty(),
                        items = routine.items.map { item -> item.toDraft() },
                    )
                }
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name, nameError = false, dirty = true) }

    fun setDescription(description: String) = _state.update { it.copy(description = description, dirty = true) }

    /** Appends exercises picked from the library with sensible defaults (3 × 12, default rest). */
    fun addExercises(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val rest = runCatching { settings.current().defaultRestSeconds }.getOrDefault(AppSettings().defaultRestSeconds)
            val picked = exercises.getAll(ids)
            _state.update { s ->
                s.copy(
                    items = s.items + picked.map { ex ->
                        DraftItem(nextKey++, ex, sets = DEFAULT_SETS, reps = DEFAULT_REPS, restSeconds = rest, loadKg = null, notes = null)
                    },
                    dirty = true,
                )
            }
        }
    }

    fun remove(index: Int) = _state.update { s ->
        s.copy(items = s.items.filterIndexed { i, _ -> i != index }, dirty = true)
    }

    fun move(from: Int, to: Int) = _state.update { s ->
        if (from !in s.items.indices || to !in s.items.indices || from == to) return@update s
        val list = s.items.toMutableList()
        list.add(to, list.removeAt(from))
        s.copy(items = list, dirty = true)
    }

    fun moveByKey(fromKey: Any, toKey: Any) {
        val items = _state.value.items
        move(items.indexOfFirst { it.key == fromKey }, items.indexOfFirst { it.key == toKey })
    }

    fun updateItem(index: Int, transform: (DraftItem) -> DraftItem) = _state.update { s ->
        if (index !in s.items.indices) return@update s
        val updated = transform(s.items[index]).let {
            it.copy(
                sets = it.sets.coerceIn(1, 20),
                reps = it.reps.coerceIn(1, 100),
                restSeconds = it.restSeconds.coerceIn(0, AppSettings.MAX_REST_SECONDS),
                loadKg = it.loadKg?.coerceIn(0.0, 1000.0),
            )
        }
        s.copy(items = s.items.toMutableList().also { it[index] = updated }, dirty = true)
    }

    /** Validates and persists. Returns false (and flags the error) when the name is blank. */
    fun save(): Boolean {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return false
        }
        viewModelScope.launch {
            routines.save(
                Routine(
                    id = routineId ?: 0,
                    name = s.name,
                    description = s.description,
                    items = s.items.map {
                        RoutineItem(exercise = it.exercise, sets = it.sets, reps = it.reps, restSeconds = it.restSeconds, loadKg = it.loadKg, notes = it.notes)
                    },
                ),
            )
            _state.update { it.copy(saved = true, dirty = false) }
        }
        return true
    }

    private fun RoutineItem.toDraft() = DraftItem(nextKey++, exercise, sets, reps, restSeconds, loadKg, notes)

    companion object {
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = 12
        const val PICKED_IDS_KEY = "picked_exercise_ids"
    }
}
