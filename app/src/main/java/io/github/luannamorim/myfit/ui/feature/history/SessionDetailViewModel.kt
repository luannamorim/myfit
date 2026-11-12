package io.github.luannamorim.myfit.ui.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.domain.history.SessionDetail
import io.github.luannamorim.myfit.ui.navigation.SessionDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(val loading: Boolean = true, val detail: SessionDetail? = null, val deleted: Boolean = false)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<SessionDetailRoute>().sessionId
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<SessionDetailUiState> = combine(history.observeDetail(id), deleted) { d, del ->
        SessionDetailUiState(loading = false, detail = d, deleted = del)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    fun delete() {
        viewModelScope.launch {
            history.deleteSession(id)
            deleted.value = true
        }
    }
}
