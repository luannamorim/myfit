package io.github.luannamorim.myfit.ui.feature.evolution

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.BodyPhoto
import io.github.luannamorim.myfit.domain.body.BodySnapshot
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.model.PhotoPose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class EvolutionSection { MEASURES, CHARTS, PHOTOS }
enum class ChartPeriod { ALL, YEAR, SIX_MONTHS }

/** [hasEstimates]: some points are estimates (body fat the user didn't type). */
data class ChartUi(val measure: Measure, val points: List<Pair<LocalDate, Double>>, val change: Double?, val hasEstimates: Boolean = false)

data class EvolutionUiState(
    val loading: Boolean = true,
    val section: EvolutionSection = EvolutionSection.MEASURES,
    val assessments: List<BodyAssessment> = emptyList(),
    val snapshot: BodySnapshot? = null,
    val selectedMeasures: Set<Measure> = DEFAULT_CHART_MEASURES,
    /** Date given to the next added photo (today by default; older photos can be back-dated). */
    val photoDate: LocalDate = LocalDate.MIN,
    val period: ChartPeriod = ChartPeriod.ALL,
    val charts: List<ChartUi> = emptyList(),
    val photos: List<BodyPhoto> = emptyList(),
    /** Photos picked for comparison, in tap order (max 2). */
    val selectedPhotos: List<Long> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val photoError: Boolean = false,
)

/** Measures that can have a chart: height barely changes, so it has none. */
val CHART_MEASURES: List<Measure> = Measure.entries - Measure.HEIGHT

/** Charts open with every measure selected; [buildCharts] skips the ones never recorded. */
val DEFAULT_CHART_MEASURES: Set<Measure> = CHART_MEASURES.toSet()

/**
 * Builds one chart per selected measure, restricted to the period. Measures never recorded in the
 * period are left out, so "All" doesn't fill the screen with empty cards.
 */
fun buildCharts(items: List<BodyAssessment>, measures: Set<Measure>, period: ChartPeriod, today: LocalDate): List<ChartUi> {
    val from = when (period) {
        ChartPeriod.ALL -> LocalDate.MIN
        ChartPeriod.YEAR -> today.minusYears(1)
        ChartPeriod.SIX_MONTHS -> today.minusMonths(6)
    }
    val inPeriod = items.filter { !it.date.isBefore(from) }
    return CHART_MEASURES.filter { it in measures }.mapNotNull { m ->
        val points = BodyComparison.series(inPeriod, m)
        if (points.isEmpty()) return@mapNotNull null
        val hasEstimates = !m.derived && inPeriod.any { m in it.estimated }
        ChartUi(m, points, if (points.size >= 2) points.last().second - points.first().second else null, hasEstimates)
    }
}

@HiltViewModel
class EvolutionViewModel @Inject constructor(
    private val body: BodyRepository,
    private val images: ImageStore,
    settings: SettingsRepository,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val ui = MutableStateFlow(EvolutionUiState(today = today, photoDate = today))

    val state: StateFlow<EvolutionUiState> = combine(
        ui,
        body.observeAssessments(),
        body.observePhotos(),
        settings.settings.map { it.sex }.distinctUntilChanged(),
    ) { s, raw, photos, sex ->
        val items = BodyMetrics.derive(raw, sex)
        s.copy(
            loading = false,
            assessments = items,
            snapshot = BodyComparison.snapshot(items),
            charts = buildCharts(items, s.selectedMeasures, s.period, today),
            photos = photos,
            selectedPhotos = s.selectedPhotos.filter { id -> photos.any { it.id == id } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EvolutionUiState(today = today, photoDate = today))

    fun setSection(section: EvolutionSection) = ui.update { it.copy(section = section) }

    fun toggleMeasure(measure: Measure) = ui.update {
        val next = if (measure in it.selectedMeasures) it.selectedMeasures - measure else it.selectedMeasures + measure
        it.copy(selectedMeasures = next)
    }

    /** Selects every measure, or clears the selection when all are already selected. */
    fun toggleAllMeasures() = ui.update {
        it.copy(selectedMeasures = if (it.selectedMeasures.containsAll(CHART_MEASURES)) emptySet() else CHART_MEASURES.toSet())
    }

    fun setPeriod(period: ChartPeriod) = ui.update { it.copy(period = period) }

    fun setPhotoDate(date: LocalDate) = ui.update { it.copy(photoDate = date) }

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

    /** Adds a photo dated [date] (the chosen photo date, today unless the user picked another day). */
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
