package io.github.luannamorim.myfit.ui.feature.nutrition

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminderSchedule
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.components.TimeDialog
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalTime

data class NutritionRemindersActions(
    val onBack: () -> Unit = {},
    val onAddMeal: (Int) -> Unit = {},
    val onMealEnabled: (Long, Boolean) -> Unit = { _, _ -> },
    val onMealTime: (Long, Int) -> Unit = { _, _ -> },
    val onDeleteMeal: (Long) -> Unit = {},
    val onWaterEnabled: (Boolean) -> Unit = {},
    val onWaterEvery: (Int) -> Unit = {},
    val onWaterFrom: (Int) -> Unit = {},
    val onWaterUntil: (Int) -> Unit = {},
    val onOpenNotifications: () -> Unit = {},
)

@Composable
fun NutritionRemindersScreen(onBack: () -> Unit, viewModel: NutritionRemindersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSystem()
        onPauseOrDispose { }
    }
    // Reminders are notifications: turning one on asks for the permission (API 33+), whatever the answer.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshSystem() }
    fun askPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    NutritionRemindersContent(
        state = state,
        actions = NutritionRemindersActions(
            onBack = onBack,
            onAddMeal = { viewModel.addMeal(it); askPermission() },
            onMealEnabled = { id, on -> viewModel.setMealEnabled(id, on); if (on) askPermission() },
            onMealTime = viewModel::setMealTime,
            onDeleteMeal = viewModel::deleteMeal,
            onWaterEnabled = { on -> viewModel.setWaterEnabled(on); if (on) askPermission() },
            onWaterEvery = viewModel::setWaterEvery,
            onWaterFrom = viewModel::setWaterFrom,
            onWaterUntil = viewModel::setWaterUntil,
            onOpenNotifications = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
        ),
        is24Hour = DateFormat.is24HourFormat(context),
    )
}

/** Which time the open time picker edits. */
private sealed interface Picking {
    data object NewMeal : Picking
    data class Meal(val id: Long) : Picking
    data object From : Picking
    data object Until : Picking
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionRemindersContent(state: NutritionRemindersUiState, actions: NutritionRemindersActions, is24Hour: Boolean, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val r = state.reminders
    var picking by rememberSaveable(stateSaver = PickingSaver) { mutableStateOf<Picking?>(null) }
    Scaffold(
        modifier = modifier,
        containerColor = c.background,
        topBar = { MyFitTopBar(stringResource(R.string.nutrition_reminders_title), onBack = actions.onBack) },
    ) { padding ->
        if (state.loading) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(horizontal = Spacing.screen).padding(top = Spacing.s, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val anyOn = r.water.enabled || r.meals.any { it.enabled }
                if (!state.notificationsEnabled && anyOn) {
                    MyFitCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.nutrition_reminders_notifications_off), style = MyFitTheme.type.caption, color = c.warning, modifier = Modifier.weight(1f))
                            TextAction(stringResource(R.string.settings_open), onClick = actions.onOpenNotifications)
                        }
                    }
                }

                SectionHeader(stringResource(R.string.nutrition_reminders_meals))
                if (r.meals.isEmpty()) {
                    Text(stringResource(R.string.nutrition_reminders_meals_empty), style = MyFitTheme.type.body, color = c.textTertiary)
                }
                r.meals.forEach { meal ->
                    MealRow(meal, is24Hour, onTime = { picking = Picking.Meal(meal.id) }, onEnabled = { actions.onMealEnabled(meal.id, it) }, onDelete = { actions.onDeleteMeal(meal.id) })
                }
                SecondaryButton(stringResource(R.string.nutrition_reminders_add_time), onClick = { picking = Picking.NewMeal }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())

                SectionHeader(stringResource(R.string.nutrition_water), Modifier.padding(top = Spacing.s))
                MyFitCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.nutrition_reminders_water_on), style = MyFitTheme.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = r.water.enabled,
                            onCheckedChange = actions.onWaterEnabled,
                            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
                        )
                    }
                    Spacer(Modifier.height(Spacing.m))
                    Text(stringResource(R.string.nutrition_reminders_every), style = MyFitTheme.type.caption, color = c.textSecondary)
                    Spacer(Modifier.height(Spacing.s))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        NutritionReminderSchedule.INTERVALS.forEach { minutes ->
                            MyFitChip(intervalLabel(minutes), selected = r.water.everyMinutes == minutes, onClick = { actions.onWaterEvery(minutes) })
                        }
                    }
                    Spacer(Modifier.height(Spacing.s))
                    TimeRow(stringResource(R.string.nutrition_reminders_from), r.water.fromMinute, is24Hour) { picking = Picking.From }
                    TimeRow(stringResource(R.string.nutrition_reminders_until), r.water.untilMinute, is24Hour) { picking = Picking.Until }
                    val count = NutritionReminderSchedule.waterSlots(r.water).size
                    Text(
                        pluralStringResource(R.plurals.nutrition_reminders_water_count, count, count),
                        style = MyFitTheme.type.caption,
                        color = c.textTertiary,
                    )
                }
            }
        }
    }
    picking?.let { p ->
        val initial = when (p) {
            Picking.NewMeal -> 12 * 60
            is Picking.Meal -> r.meals.firstOrNull { it.id == p.id }?.minuteOfDay ?: 12 * 60
            Picking.From -> r.water.fromMinute
            Picking.Until -> r.water.untilMinute
        }
        TimeDialog(
            minuteOfDay = initial,
            is24Hour = is24Hour,
            onConfirm = { minute ->
                when (p) {
                    Picking.NewMeal -> actions.onAddMeal(minute)
                    is Picking.Meal -> actions.onMealTime(p.id, minute)
                    Picking.From -> actions.onWaterFrom(minute)
                    Picking.Until -> actions.onWaterUntil(minute)
                }
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun MealRow(meal: MealReminder, is24Hour: Boolean, onTime: () -> Unit, onEnabled: (Boolean) -> Unit, onDelete: () -> Unit) {
    val c = MyFitTheme.colors
    val time = timeText(meal.minuteOfDay, is24Hour)
    val switchLabel = stringResource(R.string.nutrition_reminders_meal_at, time)
    val deleteLabel = stringResource(R.string.nutrition_reminders_delete, time)
    MyFitCard(Modifier.fillMaxWidth(), onClick = onTime, contentPadding = Spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                time,
                style = MyFitTheme.type.numericLarge,
                color = if (meal.enabled) c.textPrimary else c.textTertiary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = meal.enabled,
                onCheckedChange = onEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
                modifier = Modifier.semantics { contentDescription = switchLabel },
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = deleteLabel, tint = c.textSecondary)
            }
        }
    }
}

@Composable
private fun TimeRow(label: String, minuteOfDay: Int, is24Hour: Boolean, onClick: () -> Unit) {
    val c = MyFitTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MyFitTheme.type.body, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(timeText(minuteOfDay, is24Hour), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
    }
}

/** "1 h", "1 h 30 min", "2 h". */
private fun intervalLabel(minutes: Int): String = if (minutes % 60 == 0) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60} min"

private fun timeText(minuteOfDay: Int, is24Hour: Boolean): String {
    val t = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    return if (is24Hour) "%02d:%02d".format(t.hour, t.minute) else Dates.time(t)
}

private val PickingSaver = androidx.compose.runtime.saveable.Saver<Picking?, String>(
    save = { p ->
        when (p) {
            null -> ""
            Picking.NewMeal -> "new"
            is Picking.Meal -> "meal:${p.id}"
            Picking.From -> "from"
            Picking.Until -> "until"
        }
    },
    restore = { s ->
        when {
            s == "new" -> Picking.NewMeal
            s.startsWith("meal:") -> s.removePrefix("meal:").toLongOrNull()?.let { Picking.Meal(it) }
            s == "from" -> Picking.From
            s == "until" -> Picking.Until
            else -> null
        }
    },
)
