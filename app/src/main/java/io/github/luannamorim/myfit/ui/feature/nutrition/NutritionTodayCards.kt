package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.formatThousands
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.ui.components.LabelWithDot
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import kotlin.math.roundToInt

/** Home's "Nutrition today": water and calories as a ring with the percent of each target. Tapping opens the tab. */
@Composable
fun NutritionTodayCards(status: NutritionStatus, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val day = status.day
    val targets = status.targets
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        RingCard(
            label = stringResource(R.string.nutrition_water),
            icon = Icons.Outlined.WaterDrop,
            done = day.waterMl,
            target = targets.waterMl,
            detail = targets.waterMl?.let { stringResource(R.string.home_nutrition_water_detail, liters(day.waterMl), liters(it)) }
                ?: stringResource(R.string.nutrition_liters, liters(day.waterMl)),
            missing = if (targets.waterMl == null) listOf(TargetMissing.WEIGHT) else emptyList(),
            ringColor = c.accent,
            onOpen = onOpen,
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("nutritionCard_water"),
        )
        val over = targets.kcal != null && day.kcal > targets.kcal
        RingCard(
            label = stringResource(R.string.nutrition_calories),
            icon = Icons.Outlined.LocalFireDepartment,
            done = day.kcal,
            target = targets.kcal,
            detail = targets.kcal?.let { stringResource(R.string.home_nutrition_kcal_detail, formatThousands(day.kcal.toDouble()), formatThousands(it.toDouble())) }
                ?: stringResource(R.string.nutrition_kcal, formatThousands(day.kcal.toDouble())),
            missing = if (targets.kcal == null) targets.missing.sorted() else emptyList(),
            // Past the target matters when losing fat (same rule as the tab).
            ringColor = if (over && status.goal == NutritionGoal.LOSE) c.warning else c.accent,
            onOpen = onOpen,
            modifier = Modifier.weight(1f).fillMaxHeight().testTag("nutritionCard_kcal"),
        )
    }
}

@Composable
private fun RingCard(
    label: String,
    icon: ImageVector,
    done: Int,
    target: Int?,
    detail: String,
    missing: List<TargetMissing>,
    ringColor: Color,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val c = MyFitTheme.colors
    // Below the target it never reads 100 % (the water reminder still fires until the target is reached).
    val percent = target?.takeIf { it > 0 }?.let { if (done < it) minOf((done * 100f / it).roundToInt(), 99) else (done * 100f / it).roundToInt() }
    MyFitCard(modifier, onClick = onOpen) {
        LabelWithDot(label, dotColor = null, dotDescription = null, icon = icon)
        Spacer(Modifier.height(Spacing.m))
        // The detail always sits beside the ring and wraps into lines when narrow (it used to drop under the
        // ring and get cut by the card height on 360–384 dp phones).
        Row(verticalAlignment = Alignment.CenterVertically) {
            Ring(percent, ringColor)
            Spacer(Modifier.width(Spacing.s + Spacing.xs))
            Text(detail, style = MyFitTheme.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
        }
        // Under the ring at the card's full width: beside it (~46 dp on a 360 dp phone) it broke words mid-way.
        if (missing.isNotEmpty()) {
            Text(
                stringResource(R.string.nutrition_fill, missing.map { stringResource(missingName(it)) }.joinToString(", ")),
                style = MyFitTheme.type.caption,
                color = c.accent,
                modifier = Modifier.padding(top = Spacing.s),
            )
        }
    }
}

/** 64 dp ring filled to [percent] (full past 100), the number inside; "—" without a target. */
@Composable
private fun Ring(percent: Int?, color: Color) {
    val c = MyFitTheme.colors
    val track = c.surfaceRaised
    Box(Modifier.size(RING_SIZE).testTag("nutritionRing"), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(RING_SIZE)) {
            val stroke = RING_STROKE.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(stroke))
            val sweep = 360f * ((percent ?: 0).coerceIn(0, 100) / 100f)
            if (sweep > 0f) drawArc(color, -90f, sweep, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        val style = MyFitTheme.type.bodyStrong
        BasicText(
            text = percent?.let { stringResource(R.string.home_nutrition_percent, it) } ?: "—",
            style = style.copy(color = c.textPrimary),
            maxLines = 1,
            softWrap = false,
            // "108%" with a big system font still fits inside the ring.
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize, stepSize = 1.sp),
            modifier = Modifier.padding(horizontal = RING_STROKE + 4.dp),
        )
    }
}

private val RING_SIZE = 64.dp
private val RING_STROKE = 6.dp

private fun liters(ml: Int): String = DecimalInput.format(ml / 1000.0)
