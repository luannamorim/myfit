package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.formatSigned
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyPhoto
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.common.localImageModel
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun AssessmentCompareScreen(onBack: () -> Unit, viewModel: CompareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AssessmentCompareContent(state, viewModel::setFrom, viewModel::setTo, onBack)
}

@Composable
fun AssessmentCompareContent(state: CompareUiState, onFrom: (Long) -> Unit, onTo: (Long) -> Unit, onBack: () -> Unit) {
    val c = MyFitTheme.colors
    Scaffold(containerColor = c.background, topBar = { MyFitTopBar(stringResource(R.string.compare_title), onBack = onBack) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (!state.loading && state.assessments.size < 2) {
                    Text(stringResource(R.string.compare_need_two), style = MyFitTheme.type.body, color = c.textSecondary)
                    return@Column
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    AssessmentPicker(stringResource(R.string.compare_from), state.from, state.assessments, onFrom, Modifier.weight(1f))
                    AssessmentPicker(stringResource(R.string.compare_to), state.to, state.assessments, onTo, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                    HeaderCell(stringResource(R.string.compare_indicator), Modifier.weight(1.6f), TextAlign.Start)
                    HeaderCell(stringResource(R.string.compare_from), Modifier.weight(1f))
                    HeaderCell(stringResource(R.string.compare_to), Modifier.weight(1f))
                    HeaderCell(stringResource(R.string.compare_diff), Modifier.weight(1f))
                }
                HorizontalDivider(color = c.outline)
                state.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(row.measure.labelRes), style = MyFitTheme.type.body, color = c.textPrimary, modifier = Modifier.weight(1.6f))
                        ValueCell(row.from?.let { DecimalInput.format(it) } ?: "—", Modifier.weight(1f))
                        ValueCell(row.to?.let { DecimalInput.format(it) } ?: "—", Modifier.weight(1f))
                        ValueCell(row.diff?.let { formatSigned(it, unitLabel(row.measure)) } ?: "—", Modifier.weight(1f), strong = true)
                    }
                    HorizontalDivider(color = c.outline)
                }
                Box(Modifier.padding(bottom = Spacing.xxl))
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.End) =
    Text(text.uppercase(), style = MyFitTheme.type.overline, color = MyFitTheme.colors.textSecondary, modifier = modifier, textAlign = align)

@Composable
private fun ValueCell(text: String, modifier: Modifier, strong: Boolean = false) = Text(
    text,
    style = MyFitTheme.type.numeric,
    color = if (strong) MyFitTheme.colors.textPrimary else MyFitTheme.colors.textSecondary,
    modifier = modifier.padding(vertical = Spacing.m),
    textAlign = TextAlign.End,
)

@Composable
private fun AssessmentPicker(label: String, selected: BodyAssessment?, options: List<BodyAssessment>, onPick: (Long) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column {
            Text(label, style = MyFitTheme.type.caption, color = MyFitTheme.colors.textSecondary)
            SecondaryButton(selected?.let { Dates.short(it.date) } ?: "—", onClick = { open = true }, modifier = Modifier.fillMaxWidth())
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MyFitTheme.colors.surfaceRaised) {
            options.asReversed().forEach { a ->
                DropdownMenuItem(text = { Text(Dates.medium(a.date)) }, onClick = { onPick(a.id); open = false })
            }
        }
    }
}

@Composable
fun PhotoCompareScreen(onBack: () -> Unit, viewModel: PhotoCompareViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PhotoCompareContent(state, onBack)
}

@Composable
fun PhotoCompareContent(state: PhotoCompareUiState, onBack: () -> Unit) {
    val c = MyFitTheme.colors
    Scaffold(containerColor = c.background, topBar = { MyFitTopBar(stringResource(R.string.photo_compare_title), onBack = onBack) }) { padding ->
        Row(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            listOfNotNull(state.first, state.second).forEach { PhotoPanel(it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun PhotoPanel(photo: BodyPhoto, modifier: Modifier) {
    val c = MyFitTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        AsyncImage(
            model = localImageModel(photo.imagePath),
            contentDescription = stringResource(photo.pose.labelRes),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(MyFitShapes.medium).background(c.surfaceRaised),
        )
        Text(Dates.medium(photo.date), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
        Text(stringResource(photo.pose.labelRes), style = MyFitTheme.type.caption, color = c.textSecondary)
    }
}
