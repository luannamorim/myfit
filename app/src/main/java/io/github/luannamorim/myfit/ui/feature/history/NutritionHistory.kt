package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.formatThousands
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoals
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.ui.components.MyFitCard
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

private fun liters(ml: Int): String = DecimalInput.format(ml / 1000.0)
