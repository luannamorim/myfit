package io.github.luannamorim.myfit.ui.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.repository.ExerciseRepository
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class LibraryUiState(
    val query: String = "",
    val groups: Set<MuscleGroup> = emptySet(),
    val exercises: List<Exercise> = emptyList(),
    /** Selection order is preserved (picker adds in the order tapped). */
    val selected: List<Long> = emptyList(),
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: ExerciseRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val groups = MutableStateFlow(emptySet<MuscleGroup>())
    private val selected = MutableStateFlow(emptyList<Long>())

    private val results = combine(query, groups) { q, g -> q to g }
        .flatMapLatest { (q, g) -> repository.observeLibrary(q, g) }

    val state: StateFlow<LibraryUiState> = combine(query, groups, selected, results) { q, g, s, list ->
        LibraryUiState(query = q, groups = g, exercises = list, selected = s, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun setQuery(text: String) = query.update { text }

    fun toggleGroup(group: MuscleGroup) = groups.update { if (group in it) it - group else it + group }

    fun toggleSelected(id: Long) = selected.update { if (id in it) it - id else it + id }
}
