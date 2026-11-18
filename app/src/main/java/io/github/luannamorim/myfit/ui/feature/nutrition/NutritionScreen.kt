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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsNone
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.formatThousands
import io.github.luannamorim.myfit.domain.model.AppSettings
import kotlin.math.roundToInt
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
    val onOpenReminders: () -> Unit = {},
    /** Water pencil: daily target in ml (null = recommended) and the two quick amounts. */
    val onSetWater: (Int?, List<Int>) -> Unit = { _, _ -> },
    /** Calories pencil: daily target (null = recommended). */
    val onSetKcalTarget: (Int?) -> Unit = {},
)

@Composable
fun NutritionScreen(
    onAddMeal: (LocalDate) -> Unit,
    onOpenMeal: (Long, LocalDate) -> Unit,
    onOpenBody: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenReminders: () -> Unit = {},
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The tab can stay alive overnight: coming back after midnight shows the new day.
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    NutritionContent(
        state = state,
        actions = NutritionActions(
            onPreviousDay = viewModel::previousDay,
            onNextDay = viewModel::nextDay,
            onAddWater = viewModel::addWater,
            onUndoWater = viewModel::undoWater,
            onAddMeal = { onAddMeal(state.date) },
            onOpenMeal = { id -> onOpenMeal(id, state.date) },
            onFillMissing = { missing ->
                when (missing) {
                    TargetMissing.WEIGHT, TargetMissing.HEIGHT -> onOpenBody()
                    TargetMissing.SEX, TargetMissing.BIRTH_DATE -> onOpenSettings()
                }
            },
            onOpenReminders = onOpenReminders,
            onSetWater = viewModel::setWater,
            onSetKcalTarget = viewModel::setKcalTarget,
        ),
        modifier = modifier,
    )
}

@Composable
fun NutritionContent(state: NutritionUiState, actions: NutritionActions, modifier: Modifier = Modifier) {
    TabScreen(
        title = stringResource(R.string.nav_nutrition),
        modifier = modifier,
        actions = {
            // Same quiet bell as Home's: opens the meal and water reminders.
            IconButton(onClick = actions.onOpenReminders) {
                Icon(
                    Icons.Rounded.NotificationsNone,
                    contentDescription = stringResource(R.string.nutrition_reminders_title),
                    tint = MyFitTheme.colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) {
        if (state.loading) return@TabScreen
        DaySwitcher(state, actions)
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

/** "‹ Today ›" on its own row under the title (the header keeps room for the big title and the bell). */
@Composable
private fun DaySwitcher(state: NutritionUiState, actions: NutritionActions) {
    val c = MyFitTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = actions.onPreviousDay) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.nutrition_previous_day), tint = c.textSecondary)
        }
        Text(
            dayLabel(state.date, state.today),
            style = MyFitTheme.type.bodyStrong,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = actions.onNextDay, enabled = state.canGoForward) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(R.string.nutrition_next_day),
                tint = if (state.canGoForward) c.textSecondary else c.textTertiary.copy(alpha = 0.4f),
            )
        }
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
    var editingButtons by rememberSaveable { mutableStateOf(false) }
    MyFitCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.nutrition_water), style = MyFitTheme.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text(
                if (target != null) stringResource(R.string.nutrition_liters_of, liters(drunk), liters(target)) else stringResource(R.string.nutrition_liters, liters(drunk)),
                style = MyFitTheme.type.bodyStrong,
                color = c.textPrimary,
            )
            // Edits the amounts of the two quick buttons.
            IconButton(onClick = { editingButtons = !editingButtons }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.nutrition_water_buttons_edit), tint = c.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(Spacing.s))
        ProgressBar(if (target != null && target > 0) drunk.toFloat() / target else 0f)
        state.targets.recommendedWaterMl?.let { Recommended(stringResource(R.string.nutrition_recommended_liters, liters(it))) }
        if (target == null) MissingHint(listOf(TargetMissing.WEIGHT), actions.onFillMissing)
        Spacer(Modifier.height(Spacing.m))
        // Undo stays in the card's right corner, whatever the buttons do on a narrow phone.
        Row(verticalAlignment = Alignment.CenterVertically) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                state.waterButtons.forEach { ml ->
                    MyFitChip(stringResource(R.string.nutrition_add_ml, ml), selected = false, onClick = { actions.onAddWater(ml) })
                }
                MyFitChip(stringResource(R.string.nutrition_water_other), selected = false, onClick = { askingAmount = true })
            }
            if (state.day.waterEntries > 0) {
                IconButton(onClick = actions.onUndoWater, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.nutrition_water_undo), tint = c.textSecondary)
                }
            }
        }
        if (askingAmount) {
            Spacer(Modifier.height(Spacing.m))
            WaterAmountInput(onAdd = { actions.onAddWater(it); askingAmount = false }, onCancel = { askingAmount = false })
        }
        if (editingButtons) {
            Spacer(Modifier.height(Spacing.m))
            WaterEditor(
                customMl = state.customWaterMl,
                recommendedMl = state.targets.recommendedWaterMl,
                buttons = state.waterButtons,
                onSave = { t, b -> actions.onSetWater(t, b); editingButtons = false },
                onCancel = { editingButtons = false },
            )
        }
    }
}

/**
 * Water pencil: the daily target in liters (empty = the recommendation, shown as placeholder and link) and the
 * two quick amounts (1–5000 ml each).
 */
@Composable
private fun WaterEditor(customMl: Int?, recommendedMl: Int?, buttons: List<Int>, onSave: (Int?, List<Int>) -> Unit, onCancel: () -> Unit) {
    val c = MyFitTheme.colors
    var targetText by rememberSaveable { mutableStateOf(customMl?.let { liters(it) }.orEmpty()) }
    var first by rememberSaveable { mutableStateOf(buttons.getOrNull(0)?.toString().orEmpty()) }
    var second by rememberSaveable { mutableStateOf(buttons.getOrNull(1)?.toString().orEmpty()) }
    val values = listOf(first, second).map { t -> t.trim().toIntOrNull()?.takeIf { it in 1..NutritionDay.MAX_WATER_ML } }
    val targetMl = DecimalInput.parse(targetText)?.let { (it * 1000).roundToInt() }
    val targetOk = targetText.isBlank() || targetMl in AppSettings.CUSTOM_WATER_RANGE_ML
    MyFitTextField(
        value = targetText,
        onValueChange = { targetText = it },
        label = stringResource(R.string.nutrition_daily_target),
        placeholder = recommendedMl?.let { liters(it) },
        keyboardType = KeyboardType.Decimal,
        suffix = "L",
        isError = !targetOk,
    )
    Spacer(Modifier.height(Spacing.s))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        MyFitTextField(
            value = first,
            onValueChange = { v -> first = v.filter(Char::isDigit).take(4) },
            label = stringResource(R.string.nutrition_water_button, 1),
            keyboardType = KeyboardType.Number,
            suffix = "ml",
            modifier = Modifier.weight(1f),
        )
        MyFitTextField(
            value = second,
            onValueChange = { v -> second = v.filter(Char::isDigit).take(4) },
            label = stringResource(R.string.nutrition_water_button, 2),
            keyboardType = KeyboardType.Number,
            suffix = "ml",
            modifier = Modifier.weight(1f),
        )
    }
    EditorButtons(
        useRecommended = recommendedMl?.let { stringResource(R.string.nutrition_use_recommended_liters, liters(it)) },
        onUseRecommended = { onSave(null, values.map { it ?: 0 }.takeIf { v -> v.all { it > 0 } } ?: buttons) },
        onCancel = onCancel,
        onSave = { onSave(if (targetText.isBlank()) null else targetMl, values.map { it!! }) },
        canSave = targetOk && values.all { it != null },
        cancelColor = c.textSecondary,
    )
}

/**
 * "Use recommended (…)" on its own line (long in Portuguese: on one line with them it squeezed Save to nothing),
 * then Cancel and Save on the right.
 */
@Composable
private fun EditorButtons(useRecommended: String?, onUseRecommended: () -> Unit, onCancel: () -> Unit, onSave: () -> Unit, canSave: Boolean, cancelColor: androidx.compose.ui.graphics.Color) {
    if (useRecommended != null) TextAction(useRecommended, onClick = onUseRecommended)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextAction(stringResource(R.string.action_cancel), onClick = onCancel, color = cancelColor)
        TextAction(stringResource(R.string.action_save), onClick = onSave, enabled = canSave)
    }
}

/** "Recommended: 2.8 L" under a bar: the app's estimate stays visible when the user sets their own target. */
@Composable
private fun Recommended(text: String) {
    Spacer(Modifier.height(Spacing.s))
    Text(text, style = MyFitTheme.type.caption, color = MyFitTheme.colors.textTertiary)
}

@Composable
private fun CaloriesCard(state: NutritionUiState, actions: NutritionActions) {
    val c = MyFitTheme.colors
    val eaten = state.day.kcal
    val target = state.targets.kcal
    val recommended = state.targets.recommendedKcal
    var editing by rememberSaveable { mutableStateOf(false) }
    MyFitCard(Modifier.fillMaxWidth()) {
        TotalRow(
            label = stringResource(R.string.nutrition_calories),
            value = if (target != null) {
                stringResource(R.string.nutrition_kcal_of, formatThousands(eaten.toDouble()), formatThousands(target.toDouble()))
            } else {
                stringResource(R.string.nutrition_kcal, formatThousands(eaten.toDouble()))
            },
            onEdit = { editing = !editing },
            editDescription = stringResource(R.string.nutrition_kcal_edit),
        )
        Spacer(Modifier.height(Spacing.s))
        ProgressBar(if (target != null && target > 0) eaten.toFloat() / target else 0f)
        recommended?.let { Recommended(stringResource(R.string.nutrition_recommended_kcal, formatThousands(it.toDouble()))) }
        if (editing) {
            Spacer(Modifier.height(Spacing.m))
            KcalEditor(
                customKcal = state.customKcal,
                recommendedKcal = recommended,
                onSave = { actions.onSetKcalTarget(it); editing = false },
                onCancel = { editing = false },
            )
        }
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
private fun TotalRow(label: String, value: String, onEdit: (() -> Unit)? = null, editDescription: String? = null) {
    val c = MyFitTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MyFitTheme.type.caption, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = editDescription, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Calories pencil: the daily target (empty = the recommendation, shown as placeholder and link). */
@Composable
private fun KcalEditor(customKcal: Int?, recommendedKcal: Int?, onSave: (Int?) -> Unit, onCancel: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(customKcal?.toString().orEmpty()) }
    val kcal = text.trim().toIntOrNull()
    val ok = text.isBlank() || kcal in AppSettings.CUSTOM_KCAL_RANGE
    MyFitTextField(
        value = text,
        onValueChange = { v -> text = v.filter(Char::isDigit).take(4) },
        label = stringResource(R.string.nutrition_daily_target),
        placeholder = recommendedKcal?.toString(),
        keyboardType = KeyboardType.Number,
        suffix = "kcal",
        isError = !ok,
    )
    EditorButtons(
        useRecommended = recommendedKcal?.let { stringResource(R.string.nutrition_use_recommended_kcal, formatThousands(it.toDouble())) },
        onUseRecommended = { onSave(null) },
        onCancel = onCancel,
        onSave = { onSave(if (text.isBlank()) null else kcal) },
        canSave = ok,
        cancelColor = MyFitTheme.colors.textSecondary,
    )
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

internal fun missingName(missing: TargetMissing): Int = when (missing) {
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
