package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.BodyRange
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.MeasureDiff
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import kotlin.math.round

/**
 * Reference table behind a BMI or body-fat card's dot: every band with its color and word, the user's band
 * highlighted. Body fat without a sex shows both tables side by side and asks for the sex.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyRangesSheet(diff: MeasureDiff, sex: Sex?, onDismiss: () -> Unit) {
    val c = MyFitTheme.colors
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(
            // Scrolls: in landscape or with big fonts the table and notes don't fit.
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen).padding(bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(diff.measure.labelRes), style = MyFitTheme.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
                Text(formatMeasure(diff.measure, diff.to), style = MyFitTheme.type.numeric, color = c.textPrimary)
            }
            Spacer(Modifier.height(Spacing.xs))
            val own = BodyMetrics.ranges(diff.measure, sex)
            if (own.isNotEmpty()) {
                own.forEach { range -> RangeRow(range, listOf(rangeText(range)), current = diff.to?.let { it in range } == true) }
            } else if (diff.measure == Measure.BODY_FAT) {
                // No sex yet: men and women side by side, nothing highlighted.
                val menLabel = stringResource(R.string.body_ranges_men)
                val womenLabel = stringResource(R.string.body_ranges_women)
                RangeRow(null, listOf(menLabel, womenLabel), current = false, header = true)
                BodyMetrics.ranges(Measure.BODY_FAT, Sex.MALE).zip(BodyMetrics.ranges(Measure.BODY_FAT, Sex.FEMALE)).forEach { (men, women) ->
                    val m = rangeText(men)
                    val w = rangeText(women)
                    RangeRow(men, listOf(m, w), current = false, spoken = "$menLabel $m, $womenLabel $w, ${stringResource(men.status.labelRes)}")
                }
                Text(stringResource(R.string.body_missing_sex), style = MyFitTheme.type.body, color = c.textSecondary)
            }
            Spacer(Modifier.height(Spacing.xs))
            val notes = buildList {
                add(stringResource(if (diff.measure == Measure.BMI) R.string.body_ranges_bmi_note else R.string.body_ranges_fat_note))
                if (diff.measure == Measure.BODY_FAT && diff.estimated) add(stringResource(R.string.body_ranges_calculated))
            }
            Text(notes.joinToString(" "), style = MyFitTheme.type.caption, color = c.textTertiary)
        }
    }
}

/**
 * One table row: status dot, one or two range columns, status word; [range] null = header row. The status word
 * has a fixed width so the columns line up; a row is read as one item, the user's band as selected.
 */
@Composable
private fun RangeRow(range: BodyRange?, columns: List<String>, current: Boolean, header: Boolean = false, spoken: String? = null) {
    val c = MyFitTheme.colors
    val style = if (current) MyFitTheme.type.bodyStrong else MyFitTheme.type.body
    Row(
        Modifier.fillMaxWidth()
            .then(if (current) Modifier.background(c.surfaceRaised, MyFitShapes.small).testTag("range_current") else Modifier)
            .then(
                if (header) Modifier
                else Modifier.semantics(mergeDescendants = true) {
                    selected = current
                    if (spoken != null) contentDescription = spoken
                },
            )
            .padding(horizontal = Spacing.s, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).then(if (range != null) Modifier.background(range.status.color(), CircleShape) else Modifier))
        Spacer(Modifier.width(Spacing.m))
        columns.forEach { Text(it, style = style, color = if (header) c.textSecondary else c.textPrimary, modifier = Modifier.weight(1f)) }
        Text(
            range?.let { stringResource(it.status.labelRes) }.orEmpty(),
            style = style,
            color = c.textSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(STATUS_WIDTH),
        )
    }
}

/** Wide enough for "Atenção"/"Caution" at the body size. */
private val STATUS_WIDTH = 84.dp

/**
 * "Below 16", "18.5 – 24.9", "30 or more": the upper end shown is the last one-decimal value inside the band
 * (body fat too: values like 24,5 % must fall inside the row that is highlighted).
 */
@Composable
private fun rangeText(range: BodyRange): String {
    val step = 0.1
    fun fmt(v: Double) = DecimalInput.format(round(v * 10) / 10)
    return when {
        range.from == null -> stringResource(R.string.body_range_below, fmt(range.until!!))
        range.until == null -> stringResource(R.string.body_range_above, fmt(range.from))
        else -> "${fmt(range.from)} – ${fmt(range.until - step)}"
    }
}

private operator fun BodyRange.contains(value: Double) = (from == null || value >= from) && (until == null || value < until)
