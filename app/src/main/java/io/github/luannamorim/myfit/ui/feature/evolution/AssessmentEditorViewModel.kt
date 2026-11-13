package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
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
    /** Save refused: the only value is the height filled in from the last assessment, untouched. */
    val heightOnlyError: Boolean = false,
    val done: Boolean = false,
    /** Calculated from the typed weight and height (or the height of another assessment). */
    val bmi: Double? = null,
    /** Navy estimate while body fat isn't typed; null without sex, height or circumferences. */
    val estimatedBodyFat: Double? = null,
)

@HiltViewModel
class AssessmentEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val body: BodyRepository,
    private val settings: SettingsRepository,
    time: TimeSource,
) : ViewModel() {
    private val id: Long? = savedStateHandle.toRoute<AssessmentEditorRoute>().assessmentId
    private val _state = MutableStateFlow(AssessmentEditorUiState(isNew = id == null, date = time.today()))
    val state: StateFlow<AssessmentEditorUiState> = _state.asStateFlow()

    /** The other assessments (for the height a draft without one borrows) and the sex, read once. */
    private var others: List<BodyAssessment> = emptyList()
    private var sex: Sex? = null

    /** Height text filled in for a new assessment; saving only that, untouched, is refused. */
    private var prefilledHeight: String? = null
    private var heightEdited = false

    init {
        viewModelScope.launch {
            val existing = id?.let { body.get(it) }
            others = body.observeAssessments().first().filter { it.id != id }
            if (existing == null) prefilledHeight = others.firstNotNullOfOrNull { it.values[Measure.HEIGHT] }?.let { DecimalInput.format(it) }
            sex = settings.current().sex
            _state.update { s ->
                s.copy(
                    loading = false,
                    date = existing?.date ?: s.date,
                    // A new assessment comes with the last recorded height filled in (typed once, reused).
                    fields = existing?.values?.mapValues { DecimalInput.format(it.value) }
                        ?: prefilledHeight?.let { mapOf(Measure.HEIGHT to it) }.orEmpty(),
                    notes = existing?.notes.orEmpty(),
                    // Height is typed rarely: its hint is the last height recorded, wherever it was.
                    lastValues = others.firstOrNull()?.values.orEmpty() +
                        listOfNotNull(others.firstNotNullOfOrNull { it.values[Measure.HEIGHT] }?.let { Measure.HEIGHT to it }),
                ).withMetrics()
            }
        }
    }

    fun setDate(date: LocalDate) = _state.update { it.copy(date = date).withMetrics() }

    fun setField(measure: Measure, text: String) = _state.update { s ->
        if (measure == Measure.HEIGHT) heightEdited = true
        val invalid = text.isNotBlank() && DecimalInput.parse(text) == null
        s.copy(
            fields = s.fields + (measure to text),
            invalid = if (invalid) s.invalid + measure else s.invalid - measure,
            emptyError = false,
            heightOnlyError = false,
        ).withMetrics()
    }

    /**
     * Valid typed values. A height under 3 with decimals was typed in meters ("1,78") and becomes cm;
     * a bare "1" is the first digit of "178", not one meter.
     */
    private fun AssessmentEditorUiState.values(): Map<Measure, Double> =
        fields.mapNotNull { (m, text) ->
            DecimalInput.parse(text)?.let { m to if (m == Measure.HEIGHT && it < 3 && it % 1.0 != 0.0) it * 100 else it }
        }.toMap()

    private fun AssessmentEditorUiState.withMetrics(): AssessmentEditorUiState {
        val draft = BodyMetrics.derive(others + BodyAssessment(id = -1, date = date, values = values()), sex).last()
        return copy(
            bmi = draft.values[Measure.BMI],
            estimatedBodyFat = draft.values[Measure.BODY_FAT]?.takeIf { Measure.BODY_FAT in draft.estimated },
        )
    }

    fun setNotes(text: String) = _state.update { it.copy(notes = text) }

    /** Saves when every filled field is a valid number and at least one is filled. */
    fun save(): Boolean {
        val s = _state.value
        if (s.invalid.isNotEmpty()) return false
        val values = s.values()
        if (values[Measure.HEIGHT]?.let { it !in BodyMetrics.PLAUSIBLE_HEIGHT_CM } == true) {
            _state.update { it.copy(invalid = it.invalid + Measure.HEIGHT) }
            return false
        }
        if (values.keys == setOf(Measure.HEIGHT) && prefilledHeight != null && !heightEdited) {
            _state.update { it.copy(heightOnlyError = true) }
            return false
        }
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
