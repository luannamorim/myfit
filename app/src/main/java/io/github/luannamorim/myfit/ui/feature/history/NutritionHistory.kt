package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.formatThousands
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoals
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.ProgressBar
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

/** True when the day has any water or meal logged. */
internal val NutritionStatus.logged: Boolean get() = day.waterEntries > 0 || day.meals.isNotEmpty()

private fun NutritionStatus.waterHit() = NutritionGoals.waterHit(day, targets)
private fun NutritionStatus.kcalHit() = NutritionGoals.kcalHit(day, targets, goal)

/** The selected day's water and calories against their targets, with a check when hit. */
@Composable
internal fun DayNutritionCard(status: NutritionStatus) {
    val c = MyFitTheme.colors
    val day = status.day
    val t = status.targets
    MyFitCard(Modifier.fillMaxWidth(), contentPadding = Spacing.m) {
        Text(stringResource(R.string.nav_nutrition), style = MyFitTheme.type.caption, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.s))
        if (day.waterEntries > 0) {
            val text = t.waterMl?.let { stringResource(R.string.nutrition_liters_of, liters(day.waterMl), liters(it)) }
                ?: stringResource(R.string.nutrition_liters, liters(day.waterMl))
            DayValue(Icons.Outlined.WaterDrop, text, status.waterHit() == true)
        }
        if (day.meals.isNotEmpty()) {
            if (day.waterEntries > 0) Spacer(Modifier.height(Spacing.xs))
            val text = t.kcal?.let { stringResource(R.string.nutrition_kcal_of, formatThousands(day.kcal.toDouble()), formatThousands(it.toDouble())) }
                ?: stringResource(R.string.nutrition_kcal, formatThousands(day.kcal.toDouble()))
            DayValue(Icons.Outlined.LocalFireDepartment, text, status.kcalHit() == true)
        }
    }
}

@Composable
private fun DayValue(icon: ImageVector, text: String, hit: Boolean) {
    val c = MyFitTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MyFitTheme.type.body, color = c.textPrimary)
        if (hit) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.history_nutrition_hit), tint = c.accent, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * "Days on target" for the summary period: a week shows one dot per day (hit, missed, nothing logged) and
 * "hits/logged days"; a month shows "X of Y days" with a bar. Hidden when nothing was logged in the period.
 */
@Composable
internal fun PeriodNutritionCard(days: List<NutritionStatus>, week: Boolean) {
    if (days.none { it.logged }) return
    val c = MyFitTheme.colors
    val water = days.map { it.waterHit() }
    val kcal = days.map { it.kcalHit() }
    MyFitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.nav_nutrition), style = MyFitTheme.type.caption, color = c.textSecondary)
        if (week) {
            Row(Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                Spacer(Modifier.width(LABEL_WIDTH))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEach {
                        Text(Dates.weekdayNarrow(it.day.date.dayOfWeek), style = MyFitTheme.type.caption, color = c.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.width(DOT))
                    }
                }
                Spacer(Modifier.width(Spacing.m + COUNT_WIDTH))
            }
            WeekRow(Icons.Outlined.WaterDrop, stringResource(R.string.nutrition_water), water)
            WeekRow(Icons.Outlined.LocalFireDepartment, stringResource(R.string.nutrition_calories), kcal)
            Spacer(Modifier.height(Spacing.s))
            Text(stringResource(R.string.history_nutrition_legend), style = MyFitTheme.type.caption, color = c.textTertiary)
        } else {
            MonthRow(Icons.Outlined.WaterDrop, stringResource(R.string.nutrition_water), water)
            MonthRow(Icons.Outlined.LocalFireDepartment, stringResource(R.string.nutrition_calories), kcal)
        }
    }
}

@Composable
private fun WeekRow(icon: ImageVector, label: String, hits: List<Boolean?>) {
    val c = MyFitTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.width(LABEL_WIDTH), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MyFitTheme.type.body, color = c.textPrimary, maxLines = 1)
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) { hits.forEach { Dot(it) } }
        Spacer(Modifier.width(Spacing.m))
        Text(
            stringResource(R.string.history_nutrition_hits, hits.count { it == true }, hits.count { it != null }),
            style = MyFitTheme.type.numeric,
            color = c.textSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(COUNT_WIDTH),
        )
    }
}

@Composable
private fun MonthRow(icon: ImageVector, label: String, hits: List<Boolean?>) {
    val c = MyFitTheme.colors
    val logged = hits.count { it != null }
    val hit = hits.count { it == true }
    Row(Modifier.fillMaxWidth().padding(top = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MyFitTheme.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text(pluralStringResource(R.plurals.history_nutrition_days, logged, hit, logged), style = MyFitTheme.type.numeric, color = c.textSecondary)
    }
    Spacer(Modifier.height(Spacing.xs))
    ProgressBar(if (logged > 0) hit.toFloat() / logged else 0f)
}

/** Filled = target hit, ring = missed, small dot = nothing logged. */
@Composable
private fun Dot(hit: Boolean?) {
    val c = MyFitTheme.colors
    when (hit) {
        true -> Box(Modifier.size(DOT).background(c.accent, CircleShape))
        false -> Box(Modifier.size(DOT).border(1.5.dp, c.textTertiary, CircleShape))
        null -> Box(Modifier.size(DOT), contentAlignment = Alignment.Center) { Box(Modifier.size(4.dp).background(c.surfaceRaised, CircleShape)) }
    }
}

private val DOT = 14.dp
private val LABEL_WIDTH = 98.dp
private val COUNT_WIDTH = 36.dp

private fun liters(ml: Int): String = DecimalInput.format(ml / 1000.0)
