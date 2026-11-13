package io.github.luannamorim.myfit.ui.feature.library

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.ExerciseDeleteResult
import io.github.luannamorim.myfit.data.repository.ExerciseRepository
import io.github.luannamorim.myfit.di.ApplicationScope
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.ui.navigation.ExerciseEditorRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val movementType: MovementType = MovementType.PUSH,
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    val notes: String = "",
    val imagePath: String? = null,
    /** Library entry of a predefined exercise (its bundled drawing); null for user-created ones. */
    val seedKey: String? = null,
    val nameError: Boolean = false,
    val imageError: Boolean = false,
    val isImporting: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class ExerciseEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ExerciseRepository,
    private val images: ImageStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val exerciseId: Long? = savedStateHandle.toRoute<ExerciseEditorRoute>().exerciseId
    private val _state = MutableStateFlow(ExerciseEditorUiState(isNew = exerciseId == null))
    val state: StateFlow<ExerciseEditorUiState> = _state.asStateFlow()

    /** Image saved in the database when the editor opened. */
    private var originalImage: String? = null
    private var isCustom = true

    init {
        viewModelScope.launch {
            val existing = exerciseId?.let { repository.get(it) }
            originalImage = existing?.imagePath
            isCustom = existing?.isCustom ?: true
            _state.update { s ->
                if (existing == null) s.copy(isLoading = false)
                else s.copy(
                    isLoading = false,
                    name = existing.name,
                    muscleGroup = existing.muscleGroup,
                    movementType = existing.movementType,
                    measure = existing.measure,
                    notes = existing.notes.orEmpty(),
                    imagePath = existing.imagePath,
                    seedKey = existing.seedKey,
                )
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, nameError = false) }
    fun setGroup(value: MuscleGroup) = _state.update { it.copy(muscleGroup = value) }
    fun setMovement(value: MovementType) = _state.update { it.copy(movementType = value) }
    fun setMeasure(value: ExerciseMeasure) = _state.update { it.copy(measure = value) }
    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun pickImage(uri: Uri) {
        _state.update { it.copy(isImporting = true, imageError = false) }
        viewModelScope.launch {
            val imported = runCatching { images.import(uri, ImageKind.EXERCISE) }.getOrNull()
            if (imported == null) {
                _state.update { it.copy(isImporting = false, imageError = true) }
                return@launch
            }
            discardPendingImage()
            _state.update { it.copy(isImporting = false, imagePath = imported) }
        }
    }

    fun removeImage() {
        viewModelScope.launch {
            discardPendingImage()
            _state.update { it.copy(imagePath = null) }
        }
    }

    fun save(): Boolean {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return false
        }
        viewModelScope.launch {
            repository.save(
                Exercise(
                    id = exerciseId ?: 0,
                    name = s.name,
                    muscleGroup = s.muscleGroup,
                    movementType = s.movementType,
                    measure = s.measure,
                    imagePath = s.imagePath,
                    notes = s.notes,
                    isCustom = isCustom,
                    seedKey = s.seedKey,
                ),
            )
            if (originalImage != null && originalImage != s.imagePath) images.delete(originalImage)
            originalImage = s.imagePath
            _state.update { it.copy(done = true) }
        }
        return true
    }

    fun delete() {
        val id = exerciseId ?: return
        viewModelScope.launch {
            val result = repository.delete(id)
            discardPendingImage()
            if (result == ExerciseDeleteResult.DELETED) images.delete(originalImage)
            originalImage = null
            _state.update { it.copy(done = true) }
        }
    }

    /** Deletes the image picked in this session if it is not the persisted one. */
    private suspend fun discardPendingImage() {
        val current = _state.value.imagePath
        if (current != null && current != originalImage) images.delete(current)
    }

    override fun onCleared() {
        val pending = _state.value.imagePath
        if (pending != null && pending != originalImage) appScope.launch { images.delete(pending) }
    }
}
