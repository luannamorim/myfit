package io.github.luannamorim.myfit.ui.feature.evolution

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.formatSigned
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodySnapshot
import io.github.luannamorim.myfit.domain.body.BodyStatus
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.MeasureDiff
import io.github.luannamorim.myfit.domain.model.PhotoPose
import io.github.luannamorim.myfit.ui.common.PhotoViewer
import io.github.luannamorim.myfit.ui.common.Sparkline
import io.github.luannamorim.myfit.ui.common.ViewerPage
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.common.localImageModel
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.DateField
import io.github.luannamorim.myfit.ui.components.EmptyState
import io.github.luannamorim.myfit.ui.components.FilterChipRow
import io.github.luannamorim.myfit.ui.components.LabelWithDot
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.StatTile
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalDate

data class EvolutionActions(
    val onNewAssessment: () -> Unit = {},
    val onOpenAssessment: (Long) -> Unit = {},
    val onCompare: () -> Unit = {},
    val onComparePhotos: (Long, Long) -> Unit = { _, _ -> },
)

@Composable
fun EvolutionScreen(actions: EvolutionActions, modifier: Modifier = Modifier, viewModel: EvolutionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingPose by remember { mutableStateOf(PhotoPose.FRONT) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.addPhoto(uri, state.photoDate, pendingPose)
    }
    EvolutionContent(
        state = state,
        actions = actions,
        onSection = viewModel::setSection,
        onToggleMeasure = viewModel::toggleMeasure,
        onPeriod = viewModel::setPeriod,
        onTogglePhoto = viewModel::togglePhoto,
        onAddPhoto = { pose ->
            pendingPose = pose
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onDeletePhotos = viewModel::deleteSelectedPhotos,
        modifier = modifier,
        onToggleAllMeasures = viewModel::toggleAllMeasures,
        onPhotoDate = viewModel::setPhotoDate,
    )
}

@Composable
fun EvolutionContent(
    state: EvolutionUiState,
    actions: EvolutionActions,
    onSection: (EvolutionSection) -> Unit,
    onToggleMeasure: (Measure) -> Unit,
    onPeriod: (ChartPeriod) -> Unit,
    onTogglePhoto: (Long) -> Unit,
    onAddPhoto: (PhotoPose) -> Unit,
    onDeletePhotos: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleAllMeasures: () -> Unit = {},
    onPhotoDate: (LocalDate) -> Unit = {},
) {
    val c = MyFitTheme.colors
    TabScreen(
        title = stringResource(R.string.nav_evolution),
        modifier = modifier,
        actions = {
            FilledIconButton(
                onClick = actions.onNewAssessment,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = c.surfaceRaised, contentColor = c.textPrimary),
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.evolution_new_assessment)) }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            listOf(
                EvolutionSection.MEASURES to R.string.evolution_tab_measures,
                EvolutionSection.CHARTS to R.string.evolution_tab_charts,
                EvolutionSection.PHOTOS to R.string.evolution_tab_photos,
            ).forEach { (section, label) ->
                MyFitChip(stringResource(label), selected = state.section == section, onClick = { onSection(section) })
            }
        }
        if (state.loading) return@TabScreen
        when (state.section) {
            EvolutionSection.MEASURES -> MeasuresSection(state, actions)
            EvolutionSection.CHARTS -> ChartsSection(state, onToggleMeasure, onToggleAllMeasures, onPeriod)
            EvolutionSection.PHOTOS -> PhotosSection(state, onTogglePhoto, onAddPhoto, onPhotoDate, onDeletePhotos, actions.onComparePhotos)
        }
    }
}

@Composable
fun unitLabel(measure: Measure): String = stringResource(measure.unit.labelRes)

@Composable
fun formatMeasure(measure: Measure, value: Double?): String =
    if (value == null) "—" else "${DecimalInput.format(value)} ${unitLabel(measure)}".trim()

/** Measure name as shown on cards and in Compare (estimates are not labeled: the user asked to drop the word). */
@Composable
fun measureLabel(measure: Measure): String = stringResource(measure.labelRes)

@Composable
private fun MeasuresSection(state: EvolutionUiState, actions: EvolutionActions) {
    val c = MyFitTheme.colors
    val snapshot = state.snapshot
    if (snapshot == null) {
        EmptyState(
            title = stringResource(R.string.evolution_empty_title),
            message = stringResource(R.string.evolution_empty_message),
            action = { PrimaryButton(stringResource(R.string.evolution_new_assessment), onClick = actions.onNewAssessment, icon = Icons.Rounded.Add) },
        )
        return
    }
    BodySnapshotTiles(snapshot)
    if (state.assessments.size >= 2) {
        SecondaryButton(
            stringResource(R.string.evolution_compare),
            onClick = actions.onCompare,
            icon = Icons.AutoMirrored.Rounded.CompareArrows,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    SectionHeader(stringResource(R.string.evolution_history), Modifier.padding(top = Spacing.s))
    state.assessments.forEach { AssessmentRow(it, onClick = { actions.onOpenAssessment(it.id) }) }
}

/** Latest weight, body fat and BMI with change vs the previous assessment (neutral color: no judgment). */
@Composable
fun BodySnapshotTiles(snapshot: BodySnapshot, onOpenWeight: (() -> Unit)? = null) {
    val c = MyFitTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Text(
            Dates.medium(snapshot.latest.date) + if (snapshot.previous != null) " · " + stringResource(R.string.evolution_vs_previous) else "",
            style = MyFitTheme.type.caption,
            color = c.textSecondary,
        )
        // Weight opens Progress (Home only); BMI and body fat open their ranges table on both screens.
        var openRanges by rememberSaveable { mutableStateOf<Measure?>(null) }
        val seeRanges = stringResource(R.string.body_ranges_open)
        fun clickFor(m: Measure): Modifier = when {
            m == Measure.BMI || m == Measure.BODY_FAT -> Modifier.clickable(onClickLabel = seeRanges) { openRanges = m }
            onOpenWeight != null -> Modifier.clickable(onClick = onOpenWeight)
            else -> Modifier
        }
        // Weight leads on its own row; the others go in pairs.
        val (weight, others) = snapshot.highlights.partition { it.measure == Measure.WEIGHT }
        (weight.map { listOf(it) } + others.chunked(2)).forEach { row ->
            val single = row.singleOrNull()
            if (single != null) {
                // Alone in its row: full width, with the recent trend beside it.
                TrendTile(single, snapshot.trends[single.measure].orEmpty(), Modifier.fillMaxWidth().testTag("bodyTile_${single.measure.name}").then(clickFor(single.measure)))
            } else {
                // Same height for both cards, even when one label wraps (e.g. "Gordura corporal (estimada)").
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    row.forEach { diff ->
                        StatTile(
                            label = measureLabel(diff.measure),
                            indicatorColor = diff.status?.color(),
                            indicatorDescription = diff.status?.let { stringResource(it.labelRes) },
                            value = diff.to?.let { DecimalInput.format(it) } ?: EMPTY_VALUE,
                            unit = unitLabel(diff.measure).ifEmpty { null }.takeIf { diff.to != null },
                            delta = diff.diff?.let { formatSigned(it, unitLabel(diff.measure)) },
                            deltaPositive = null,
                            hint = diff.missing?.let { stringResource(it.labelRes) },
                            modifier = Modifier.weight(1f).fillMaxHeight().testTag("bodyTile_${diff.measure.name}").then(clickFor(diff.measure)),
                        )
                    }
                }
            }
        }
        openRanges?.let { m ->
            snapshot.highlights.firstOrNull { it.measure == m }?.let { BodyRangesSheet(it, snapshot.sex, onDismiss = { openRanges = null }) }
        }
    }
}

/** Shown in a body card that has no value yet. */
private const val EMPTY_VALUE = "—"

/** Health-range colors: the one place body measurements get green, amber or red (see BRAND.md). */
@Composable
internal fun BodyStatus.color(): Color = when (this) {
    BodyStatus.GOOD -> MyFitTheme.colors.accent
    BodyStatus.CAUTION -> MyFitTheme.colors.warning
    BodyStatus.POOR -> MyFitTheme.colors.danger
}

/** Full-width highlight: value and change vs the previous assessment, plus a trend line of recent ones. */
@Composable
private fun TrendTile(diff: MeasureDiff, trend: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val unit = unitLabel(diff.measure)
    MyFitCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                LabelWithDot(measureLabel(diff.measure), diff.status?.color(), diff.status?.let { stringResource(it.labelRes) })
                Spacer(Modifier.height(Spacing.s))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(diff.to?.let { DecimalInput.format(it) } ?: EMPTY_VALUE, style = MyFitTheme.type.numericLarge.copy(fontSize = 44.sp, lineHeight = 48.sp), color = c.textPrimary)
                    if (unit.isNotEmpty() && diff.to != null) Text(" $unit", style = MyFitTheme.type.label, color = c.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                }
                diff.diff?.let {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(formatSigned(it, unit), style = MyFitTheme.type.body, color = c.textSecondary)
                }
            }
            if (trend.size >= 2) {
                Column(Modifier.weight(0.9f), horizontalAlignment = Alignment.End) {
                    Sparkline(trend.map { it.second }, c.accent, Modifier.fillMaxWidth().height(56.dp))
                    Spacer(Modifier.height(Spacing.s))
                    val change = trend.last().second - trend.first().second
                    Text(
                        stringResource(R.string.body_trend_since, formatSigned(change, unit), Dates.dayMonth(trend.first().first)),
                        style = MyFitTheme.type.caption,
                        color = c.textTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssessmentRow(assessment: BodyAssessment, onClick: () -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Dates.medium(assessment.date), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                val weight = assessment.values[Measure.WEIGHT]?.let { "${DecimalInput.format(it)} kg · " }.orEmpty()
                Text(
                    weight + pluralStringResource(R.plurals.evolution_measures_count, assessment.measuredCount, assessment.measuredCount),
                    style = MyFitTheme.type.caption,
                    color = c.textSecondary,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

@Composable
private fun ChartsSection(
    state: EvolutionUiState,
    onToggleMeasure: (Measure) -> Unit,
    onToggleAll: () -> Unit,
    onPeriod: (ChartPeriod) -> Unit,
) {
    val c = MyFitTheme.colors
    if (state.assessments.isEmpty()) {
        Text(stringResource(R.string.charts_not_enough), style = MyFitTheme.type.body, color = c.textSecondary)
        return
    }
    FilterChipRow(
        options = CHART_MEASURES,
        selected = state.selectedMeasures,
        label = { stringResource(it.labelRes) },
        onToggle = onToggleMeasure,
        allLabel = stringResource(R.string.charts_all_measures),
        onToggleAll = onToggleAll,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        listOf(ChartPeriod.ALL to R.string.charts_period_all, ChartPeriod.YEAR to R.string.charts_period_year, ChartPeriod.SIX_MONTHS to R.string.charts_period_6m)
            .forEach { (p, label) -> MyFitChip(stringResource(label), selected = state.period == p, onClick = { onPeriod(p) }) }
    }
    if (state.charts.isEmpty()) {
        val hint = if (state.selectedMeasures.isEmpty()) R.string.charts_pick_measure else R.string.charts_no_data
        Text(stringResource(hint), style = MyFitTheme.type.body, color = c.textSecondary)
    }
    state.charts.forEach { chart ->
        MyFitCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(chart.measure.labelRes), style = MyFitTheme.type.caption, color = c.textSecondary)
                    Text(formatMeasure(chart.measure, chart.points.lastOrNull()?.second), style = MyFitTheme.type.numericLarge, color = c.textPrimary)
                }
                chart.change?.let {
                    Text(
                        stringResource(R.string.charts_change, formatSigned(it, unitLabel(chart.measure))),
                        style = MyFitTheme.type.caption,
                        color = c.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.m))
            if (chart.points.size >= 2) {
                MeasureChart(chart.points)
            } else {
                Text(stringResource(R.string.charts_not_enough), style = MyFitTheme.type.caption, color = c.textTertiary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotosSection(
    state: EvolutionUiState,
    onTogglePhoto: (Long) -> Unit,
    onAddPhoto: (PhotoPose) -> Unit,
    onPhotoDate: (LocalDate) -> Unit,
    onDelete: () -> Unit,
    onCompare: (Long, Long) -> Unit,
) {
    val c = MyFitTheme.colors
    var confirmDelete by remember { mutableStateOf(false) }
    var pose by remember { mutableStateOf(PhotoPose.FRONT) }
    var viewing by remember { mutableStateOf<Int?>(null) }

    MyFitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.photos_pose), style = MyFitTheme.type.caption, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.s))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            PhotoPose.entries.forEach { p -> MyFitChip(stringResource(p.labelRes), selected = pose == p, onClick = { pose = p }) }
        }
        Spacer(Modifier.height(Spacing.m))
        DateField(stringResource(R.string.photos_date), state.photoDate, onPhotoDate, Modifier.fillMaxWidth(), maxDate = state.today)
        Spacer(Modifier.height(Spacing.s))
        PrimaryButton(stringResource(R.string.photos_add), onClick = { onAddPhoto(pose) }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
        if (state.photoError) Text(stringResource(R.string.exercise_image_error), style = MyFitTheme.type.caption, color = c.danger)
    }

    if (state.photos.isEmpty()) {
        Text(stringResource(R.string.photos_empty), style = MyFitTheme.type.body, color = c.textSecondary)
        return
    }

    if (state.selectedPhotos.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SecondaryButton(
                stringResource(R.string.photos_compare),
                onClick = { onCompare(state.selectedPhotos[0], state.selectedPhotos[1]) },
                enabled = state.selectedPhotos.size == 2,
                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(stringResource(R.string.action_delete), onClick = { confirmDelete = true }, icon = Icons.Rounded.Delete, contentColor = c.danger)
        }
    } else {
        Text(stringResource(R.string.photos_select_two), style = MyFitTheme.type.caption, color = c.textTertiary)
    }

    state.photos.groupBy { it.date }.forEach { (date, photos) ->
        SectionHeader(Dates.medium(date), Modifier.padding(top = Spacing.s))
        photos.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                row.forEach { photo ->
                    val selected = photo.id in state.selectedPhotos
                    val model = localImageModel(photo.imagePath)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(3f / 4f)
                            .clip(MyFitShapes.small)
                            .then(if (selected) Modifier.border(BorderStroke(3.dp, c.accent), MyFitShapes.small) else Modifier)
                            .semantics { this.selected = selected }
                            // Like a gallery: tap opens the photo; press and hold starts selecting (then taps toggle).
                            .combinedClickable(
                                onClick = {
                                    if (state.selectedPhotos.isEmpty()) viewing = state.photos.indexOf(photo) else onTogglePhoto(photo.id)
                                },
                                onLongClick = { onTogglePhoto(photo.id) },
                            ),
                    ) {
                        AsyncImage(model, contentDescription = stringResource(photo.pose.labelRes), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        Text(
                            stringResource(photo.pose.labelRes),
                            style = MyFitTheme.type.caption,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.s),
                        )
                        if (selected) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(Spacing.s).size(24.dp).clip(MyFitShapes.pill).then(Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accent)
                            }
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    viewing?.let { index ->
        val pages = state.photos.map { ViewerPage(it.imagePath, Dates.medium(it.date) + " · " + stringResource(it.pose.labelRes)) }
        PhotoViewer(pages, initialPage = index, onDismiss = { viewing = null })
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.photos_delete_title),
            message = stringResource(R.string.photos_delete_message),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
