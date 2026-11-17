package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.formatThousands
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.ProgressBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime

/** Everything the Nutrition tab can ask its host to do. */
data class NutritionActions(
    val onPreviousDay: () -> Unit = {},
    val onNextDay: () -> Unit = {},
    val onAddWater: (Int) -> Unit = {},
    val onUndoWater: () -> Unit = {},
    val onAddMeal: () -> Unit = {},
    val onOpenMeal: (Long) -> Unit = {},
    /** Opens where the missing value is filled in: Progress (weight, height) or Settings (sex, birth date). */
    val onFillMissing: (TargetMissing) -> Unit = {},
)

@Composable
fun NutritionScreen(
    onAddMeal: (LocalDate) -> Unit,
    onOpenMeal: (Long) -> Unit,
    onOpenBody: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NutritionContent(
        state = state,
        actions = NutritionActions(
            onPreviousDay = viewModel::previousDay,
            onNextDay = viewModel::nextDay,
            onAddWater = viewModel::addWater,
            onUndoWater = viewModel::undoWater,
            onAddMeal = { onAddMeal(state.date) },
            onOpenMeal = onOpenMeal,
            onFillMissing = { missing ->
                when (missing) {
                    TargetMissing.WEIGHT, TargetMissing.HEIGHT -> onOpenBody()
                    TargetMissing.SEX, TargetMissing.BIRTH_DATE -> onOpenSettings()
                }
            },
        ),
        modifier = modifier,
    )
}

@Composable
fun NutritionContent(state: NutritionUiState, actions: NutritionActions, modifier: Modifier = Modifier) {
    TabScreen(
        title = stringResource(R.string.nav_nutrition),
        modifier = modifier,
        actions = { if (!state.loading) DaySwitcher(state, actions) },
    ) {
        if (state.loading) return@TabScreen
        WaterCard(state, actions)
        CaloriesCard(state, actions)
        SectionHeader(stringResource(R.string.nutrition_meals), Modifier.padding(top = Spacing.s))
        if (state.day.meals.isEmpty()) {
            Text(stringResource(R.string.nutrition_meals_empty), style = MyFitTheme.type.body, color = MyFitTheme.colors.textTertiary)
        }
        state.day.meals.forEach { meal -> MealRow(meal, onClick = { actions.onOpenMeal(meal.id) }) }
        SecondaryButton(stringResource(R.string.nutrition_add_meal), onClick = actions.onAddMeal, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DaySwitcher(state: NutritionUiState, actions: NutritionActions) {
    val c = MyFitTheme.colors
    IconButton(onClick = actions.onPreviousDay) {
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.nutrition_previous_day), tint = c.textSecondary)
    }
    Text(dayLabel(state.date, state.today), style = MyFitTheme.type.label, color = c.textPrimary)
    IconButton(onClick = actions.onNextDay, enabled = state.canGoForward) {
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(R.string.nutrition_next_day),
            tint = if (state.canGoForward) c.textSecondary else c.textTertiary.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.nutrition_today)
    today.minusDays(1) -> stringResource(R.string.nutrition_yesterday)
    else -> Dates.dayMonth(date)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaterCard(state: NutritionUiState, actions: NutritionActions) {
    val c = MyFitTheme.colors
    val drunk = state.day.waterMl
    val target = state.targets.waterMl
    var askingAmount by rememberSaveable { mutableStateOf(false) }
    MyFitCard(Modifier.fillMaxWidth()) {
        TotalRow(
            label = stringResource(R.string.nutrition_water),
            value = if (target != null) stringResource(R.string.nutrition_liters_of, liters(drunk), liters(target)) else stringResource(R.string.nutrition_liters, liters(drunk)),
        )
        Spacer(Modifier.height(Spacing.s))
        ProgressBar(if (target != null && target > 0) drunk.toFloat() / target else 0f)
        if (target == null) MissingHint(listOf(TargetMissing.WEIGHT), actions.onFillMissing)
        Spacer(Modifier.height(Spacing.m))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            WATER_STEPS_ML.forEach { ml ->
                MyFitChip(stringResource(R.string.nutrition_add_ml, ml), selected = false, onClick = { actions.onAddWater(ml) })
            }
            MyFitChip(stringResource(R.string.nutrition_water_other), selected = false, onClick = { askingAmount = true })
            if (state.day.waterEntries > 0) TextAction(stringResource(R.string.nutrition_water_undo), onClick = actions.onUndoWater, color = c.textSecondary)
        }
        if (askingAmount) {
            Spacer(Modifier.height(Spacing.m))
            WaterAmountInput(onAdd = { actions.onAddWater(it); askingAmount = false }, onCancel = { askingAmount = false })
        }
    }
}

@Composable
private fun CaloriesCard(state: NutritionUiState, actions: NutritionActions) {
    val c = MyFitTheme.colors
    val eaten = state.day.kcal
    val target = state.targets.kcal
    MyFitCard(Modifier.fillMaxWidth()) {
        TotalRow(
            label = stringResource(R.string.nutrition_calories),
            value = if (target != null) {
                stringResource(R.string.nutrition_kcal_of, formatThousands(eaten.toDouble()), formatThousands(target.toDouble()))
            } else {
                stringResource(R.string.nutrition_kcal, formatThousands(eaten.toDouble()))
            },
        )
        Spacer(Modifier.height(Spacing.s))
        ProgressBar(if (target != null && target > 0) eaten.toFloat() / target else 0f)
        if (target == null) {
            MissingHint(state.targets.missing.sorted(), actions.onFillMissing)
        } else {
            val goal = goalLabel(state.goal)
            val left = target - eaten
            Spacer(Modifier.height(Spacing.s))
            Text(
                when {
                    left > 0 -> stringResource(R.string.nutrition_left, goal, formatThousands(left.toDouble()))
                    left < 0 -> stringResource(R.string.nutrition_over, goal, formatThousands(-left.toDouble()))
                    else -> stringResource(R.string.nutrition_on_target, goal)
                },
                style = MyFitTheme.type.caption,
                // Past the target matters when losing fat; for the other goals it's just information.
                color = if (left < 0 && state.goal == NutritionGoal.LOSE) c.warning else c.textSecondary,
            )
        }
    }
}

@Composable
private fun goalLabel(goal: NutritionGoal): String = stringResource(
    when (goal) {
        NutritionGoal.LOSE -> R.string.settings_goal_lose
        NutritionGoal.MAINTAIN -> R.string.settings_goal_maintain
        NutritionGoal.GAIN -> R.string.settings_goal_gain
    },
)

@Composable
private fun TotalRow(label: String, value: String) {
    val c = MyFitTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text(label, style = MyFitTheme.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
    }
}

/** "Fill in: weight, birth date"; tapping opens where the first one is filled (body values first). */
@Composable
private fun MissingHint(missing: List<TargetMissing>, onFill: (TargetMissing) -> Unit) {
    if (missing.isEmpty()) return
    val names = missing.map { stringResource(missingName(it)) }.joinToString(", ")
    Spacer(Modifier.height(Spacing.s))
    Text(
        stringResource(R.string.nutrition_fill, names),
        style = MyFitTheme.type.caption,
        color = MyFitTheme.colors.accent,
        modifier = Modifier.clickable(role = Role.Button) { onFill(missing.first()) }.padding(vertical = Spacing.xs),
    )
}

private fun missingName(missing: TargetMissing): Int = when (missing) {
    TargetMissing.WEIGHT -> R.string.nutrition_missing_weight
    TargetMissing.HEIGHT -> R.string.nutrition_missing_height
    TargetMissing.SEX -> R.string.nutrition_missing_sex
    TargetMissing.BIRTH_DATE -> R.string.nutrition_missing_birth
}

@Composable
private fun MealRow(meal: Meal, onClick: () -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth(), onClick = onClick, contentPadding = Spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                Dates.time(LocalTime.of(meal.minuteOfDay / 60, meal.minuteOfDay % 60)),
                style = MyFitTheme.type.caption,
                color = c.textSecondary,
            )
            Spacer(Modifier.width(Spacing.m))
            Text(
                meal.description ?: stringResource(R.string.nutrition_meal_untitled),
                style = MyFitTheme.type.body,
                color = c.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(Spacing.m))
            Text(stringResource(R.string.nutrition_kcal, formatThousands(meal.kcal.toDouble())), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
        }
    }
}

/** Inline "other amount" field of the water card (1–5000 ml). */
@Composable
private fun WaterAmountInput(onAdd: (Int) -> Unit, onCancel: () -> Unit) {
    val c = MyFitTheme.colors
    var text by rememberSaveable { mutableStateOf("") }
    val ml = text.trim().toIntOrNull()?.takeIf { it in 1..NutritionDay.MAX_WATER_ML }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MyFitTextField(
            value = text,
            onValueChange = { v -> text = v.filter(Char::isDigit).take(4) },
            label = stringResource(R.string.nutrition_water_amount),
            keyboardType = KeyboardType.Number,
            suffix = "ml",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.s))
        TextAction(stringResource(R.string.nutrition_add), onClick = { ml?.let(onAdd) }, enabled = ml != null)
        TextAction(stringResource(R.string.action_cancel), onClick = onCancel, color = c.textSecondary)
    }
}

private val WATER_STEPS_ML = listOf(250, 500)

private fun liters(ml: Int): String = DecimalInput.format(ml / 1000.0)
