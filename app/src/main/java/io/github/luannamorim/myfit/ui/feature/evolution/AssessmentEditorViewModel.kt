package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.ui.navigation.AssessmentEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AssessmentEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val date: LocalDate = LocalDate.MIN,
    /** Raw text per measure (Brazilian decimal input). */
    val fields: Map<Measure, String> = emptyMap(),
    /** Values of the most recent other assessment, shown as hints. */
    val lastValues: Map<Measure, Double> = emptyMap(),
    val notes: String = "",
    val invalid: Set<Measure> = emptySet(),
    val emptyError: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class AssessmentEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val body: BodyRepository,
    time: TimeSource,
) : ViewModel() {
    private val id: Long? = savedStateHandle.toRoute<AssessmentEditorRoute>().assessmentId
    private val _state = MutableStateFlow(AssessmentEditorUiState(isNew = id == null, date = time.today()))
    val state: StateFlow<AssessmentEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = id?.let { body.get(it) }
            val last = body.observeAssessments().first().firstOrNull { it.id != id }
            _state.update { s ->
                s.copy(
                    loading = false,
                    date = existing?.date ?: s.date,
                    fields = existing?.values?.mapValues { DecimalInput.format(it.value) }.orEmpty(),
                    notes = existing?.notes.orEmpty(),
                    lastValues = last?.values.orEmpty(),
                )
            }
        }
    }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date) }

    fun setField(measure: Measure, text: String) = _state.update { s ->
        val invalid = text.isNotBlank() && DecimalInput.parse(text) == null
        s.copy(
            fields = s.fields + (measure to text),
            invalid = if (invalid) s.invalid + measure else s.invalid - measure,
            emptyError = false,
        )
    }

    fun setNotes(text: String) = _state.update { it.copy(notes = text) }

    /** Saves when every filled field is a valid number and at least one is filled. */
    fun save(): Boolean {
        val s = _state.value
        if (s.invalid.isNotEmpty()) return false
        val values = s.fields.mapNotNull { (m, text) -> DecimalInput.parse(text)?.let { m to it } }.toMap()
        if (values.isEmpty()) {
            _state.update { it.copy(emptyError = true) }
            return false
        }
        viewModelScope.launch {
            body.save(BodyAssessment(id = id ?: 0, date = s.date, values = values, notes = s.notes))
            _state.update { it.copy(done = true) }
        }
        return true
    }

    fun delete() {
        val existing = id ?: return
        viewModelScope.launch {
            body.delete(existing)
            _state.update { it.copy(done = true) }
        }
    }
}
