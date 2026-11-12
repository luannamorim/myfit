package io.github.luannamorim.myfit.ui.feature.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutinesUiState(
    val routines: List<RoutineOverview> = emptyList(),
    val todayEpochDay: Long = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routines: RoutineRepository,
    private val time: TimeSource,
) : ViewModel() {

    val state: StateFlow<RoutinesUiState> = routines.observeOverviews()
        .map { RoutinesUiState(routines = it, todayEpochDay = time.today().toEpochDay(), isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    fun duplicate(id: Long) {
        viewModelScope.launch { routines.duplicate(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { routines.delete(id) }
    }
}
