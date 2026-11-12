package io.github.luannamorim.myfit.ui.feature.evolution

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyPhoto
import io.github.luannamorim.myfit.domain.body.BodySnapshot
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.model.PhotoPose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class EvolutionSection { MEASURES, CHARTS, PHOTOS }
enum class ChartPeriod { ALL, YEAR, SIX_MONTHS }

data class ChartUi(val measure: Measure, val points: List<Pair<LocalDate, Double>>, val change: Double?)

data class EvolutionUiState(
    val loading: Boolean = true,
    val section: EvolutionSection = EvolutionSection.MEASURES,
    val assessments: List<BodyAssessment> = emptyList(),
    val snapshot: BodySnapshot? = null,
    val selectedMeasures: Set<Measure> = DEFAULT_CHART_MEASURES,
    val period: ChartPeriod = ChartPeriod.ALL,
    val charts: List<ChartUi> = emptyList(),
    val photos: List<BodyPhoto> = emptyList(),
    /** Photos picked for comparison, in tap order (max 2). */
    val selectedPhotos: List<Long> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val photoError: Boolean = false,
)

val DEFAULT_CHART_MEASURES = setOf(Measure.WEIGHT, Measure.WAIST, Measure.ARM_RIGHT)

/** Builds one chart per selected measure, restricted to the period. */
fun buildCharts(items: List<BodyAssessment>, measures: Set<Measure>, period: ChartPeriod, today: LocalDate): List<ChartUi> {
    val from = when (period) {
        ChartPeriod.ALL -> LocalDate.MIN
        ChartPeriod.YEAR -> today.minusYears(1)
        ChartPeriod.SIX_MONTHS -> today.minusMonths(6)
    }
    return Measure.entries.filter { it in measures }.map { m ->
        val points = BodyComparison.series(items, m).filter { !it.first.isBefore(from) }
        ChartUi(m, points, if (points.size >= 2) points.last().second - points.first().second else null)
    }
}

@HiltViewModel
class EvolutionViewModel @Inject constructor(
    private val body: BodyRepository,
    private val images: ImageStore,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val ui = MutableStateFlow(EvolutionUiState(today = today))

    val state: StateFlow<EvolutionUiState> = combine(ui, body.observeAssessments(), body.observePhotos()) { s, items, photos ->
        s.copy(
            loading = false,
            assessments = items,
            snapshot = BodyComparison.snapshot(items),
            charts = buildCharts(items, s.selectedMeasures, s.period, today),
            photos = photos,
            selectedPhotos = s.selectedPhotos.filter { id -> photos.any { it.id == id } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvolutionUiState(today = today))

    fun setSection(section: EvolutionSection) = ui.update { it.copy(section = section) }

    fun toggleMeasure(measure: Measure) = ui.update {
        val next = if (measure in it.selectedMeasures) it.selectedMeasures - measure else it.selectedMeasures + measure
        it.copy(selectedMeasures = next)
    }

    fun setPeriod(period: ChartPeriod) = ui.update { it.copy(period = period) }

    fun togglePhoto(id: Long) = ui.update {
        val current = it.selectedPhotos
        val next = when {
            id in current -> current - id
            current.size >= 2 -> listOf(current.last(), id)
            else -> current + id
        }
        it.copy(selectedPhotos = next)
    }

    fun clearPhotoSelection() = ui.update { it.copy(selectedPhotos = emptyList()) }

    fun addPhoto(uri: Uri, date: LocalDate, pose: PhotoPose) {
        viewModelScope.launch {
            val path = runCatching { images.import(uri, ImageKind.BODY) }.getOrNull()
            if (path == null) {
                ui.update { it.copy(photoError = true) }
                return@launch
            }
            body.addPhoto(date, pose, path)
            ui.update { it.copy(photoError = false) }
        }
    }

    fun deleteSelectedPhotos() {
        val ids = state.value.selectedPhotos
        viewModelScope.launch {
            ids.forEach { id -> body.deletePhoto(id)?.let { images.delete(it) } }
            clearPhotoSelection()
        }
    }
}
