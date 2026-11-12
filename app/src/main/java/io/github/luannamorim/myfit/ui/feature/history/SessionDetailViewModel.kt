package io.github.luannamorim.myfit.ui.feature.history

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.domain.history.SessionDetail
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.navigation.SessionDetailRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val loading: Boolean = true,
    val detail: SessionDetail? = null,
    val deleted: Boolean = false,
    val photoError: Boolean = false,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    private val images: ImageStore,
) : ViewModel() {
    private val id = savedStateHandle.toRoute<SessionDetailRoute>().sessionId
    private val deleted = MutableStateFlow(false)
    private val photoError = MutableStateFlow(false)

    val state: StateFlow<SessionDetailUiState> = combine(history.observeDetail(id), deleted, photoError) { d, del, err ->
        SessionDetailUiState(loading = false, detail = d, deleted = del, photoError = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    fun setIntensity(value: Intensity?) {
        viewModelScope.launch { history.setIntensity(id, value) }
    }

    fun setPhoto(uri: Uri) {
        viewModelScope.launch {
            val path = runCatching { images.import(uri, ImageKind.SESSION) }.getOrNull()
            photoError.value = path == null
            if (path != null) images.delete(history.setImage(id, path))
        }
    }

    fun removePhoto() {
        viewModelScope.launch { images.delete(history.setImage(id, null)) }
    }

    fun delete() {
        viewModelScope.launch {
            val photo = state.value.detail?.summary?.imagePath
            history.deleteSession(id)
            images.delete(photo)
            deleted.value = true
        }
    }
}
