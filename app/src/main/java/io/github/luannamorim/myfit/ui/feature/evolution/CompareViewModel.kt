package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyPhoto
import io.github.luannamorim.myfit.domain.body.MeasureDiff
import io.github.luannamorim.myfit.ui.navigation.PhotoCompareRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompareUiState(
    val loading: Boolean = true,
    /** Oldest first. */
    val assessments: List<BodyAssessment> = emptyList(),
    val from: BodyAssessment? = null,
    val to: BodyAssessment? = null,
    /** Only indicators present in at least one side. */
    val rows: List<MeasureDiff> = emptyList(),
)

@HiltViewModel
class CompareViewModel @Inject constructor(body: BodyRepository) : ViewModel() {
    private val fromId = MutableStateFlow<Long?>(null)
    private val toId = MutableStateFlow<Long?>(null)

    val state: StateFlow<CompareUiState> = combine(body.observeAssessments(), fromId, toId) { items, f, t ->
        val ordered = items.sortedWith(compareBy({ it.date }, { it.id }))
        // Defaults: first and latest assessment.
        val from = ordered.firstOrNull { it.id == f } ?: ordered.firstOrNull()
        val to = ordered.firstOrNull { it.id == t } ?: ordered.lastOrNull()
        val rows = if (from != null && to != null && from.id != to.id) {
            BodyComparison.compare(from, to).filter { it.from != null || it.to != null }
        } else {
            emptyList()
        }
        CompareUiState(loading = false, assessments = ordered, from = from, to = to, rows = rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompareUiState())

    fun setFrom(id: Long) {
        fromId.value = id
    }

    fun setTo(id: Long) {
        toId.value = id
    }
}

data class PhotoCompareUiState(val first: BodyPhoto? = null, val second: BodyPhoto? = null)

@HiltViewModel
class PhotoCompareViewModel @Inject constructor(savedStateHandle: SavedStateHandle, body: BodyRepository) : ViewModel() {
    private val route = savedStateHandle.toRoute<PhotoCompareRoute>()
    private val _state = MutableStateFlow(PhotoCompareUiState())
    val state: StateFlow<PhotoCompareUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val a = body.getPhoto(route.firstId)
            val b = body.getPhoto(route.secondId)
            // Older photo on the left.
            val ordered = listOfNotNull(a, b).sortedBy { it.date }
            _state.value = PhotoCompareUiState(ordered.getOrNull(0), ordered.getOrNull(1))
        }
    }
}
