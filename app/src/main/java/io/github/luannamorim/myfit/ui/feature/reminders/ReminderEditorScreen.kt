package io.github.luannamorim.myfit.ui.feature.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.appLocale
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle

@Composable
fun ReminderEditorScreen(onClose: () -> Unit, viewModel: ReminderEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.done) { if (state.done) onClose() }
    // A reminder is a notification: ask for the permission (API 33+) when saving, then save whatever the answer.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.save() }
    ReminderEditorContent(
        state = state,
        actions = ReminderEditorActions(
            onSelectRoutine = viewModel::selectRoutine,
            onActivity = viewModel::setActivity,
            onTime = viewModel::setTime,
            onToggleDay = viewModel::toggleDay,
            onDate = viewModel::setDate,
            onSave = {
                if (viewModel.validate()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.save()
                    }
                }
            },
            onDelete = viewModel::delete,
            onBack = onClose,
        ),
    )
}

class ReminderEditorActions(
    val onSelectRoutine: (Long?) -> Unit = {},
    val onActivity: (String) -> Unit = {},
    val onTime: (Int) -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onDate: (LocalDate) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorContent(
    state: ReminderEditorUiState,
    actions: ReminderEditorActions,
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
) {
    val c = MyFitTheme.colors
    var pickTime by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(if (state.isNew) R.string.reminders_new else R.string.reminders_edit),
                onBack = actions.onBack,
                actions = { TextAction(stringResource(R.string.action_save), onClick = actions.onSave, enabled = !state.loading) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                SectionHeader(stringResource(R.string.reminder_what))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    state.routines.forEach { routine ->
                        MyFitChip(routine.name, selected = state.routineId == routine.id, onClick = { actions.onSelectRoutine(routine.id) })
                    }
                    MyFitChip(stringResource(R.string.reminder_other), selected = state.routineId == null, onClick = { actions.onSelectRoutine(null) })
                }
                if (state.routineId == null) {
                    MyFitTextField(
                        value = state.activity,
                        onValueChange = actions.onActivity,
                        label = stringResource(R.string.reminder_activity),
                        placeholder = stringResource(R.string.reminder_activity_hint),
                        isError = state.activityError,
                        supportingText = if (state.activityError) stringResource(R.string.reminder_activity_required) else null,
                    )
                    // Quick pick: the sports that can be logged (swimming, bike...) fill in the name.
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Sport.entries.filter { it != Sport.OTHER }.forEach { sport ->
                            val name = stringResource(sport.labelRes)
                            MyFitChip(name, selected = state.activity == name, onClick = { actions.onActivity(name) })
                        }
                    }
                }

                SectionHeader(stringResource(R.string.reminder_time), Modifier.padding(top = Spacing.s))
                MyFitCard(onClick = { pickTime = true }, modifier = Modifier.fillMaxWidth()) {
                    ReminderTime(
                        state.minuteOfDay,
                        is24Hour,
                        style = MyFitTheme.type.numericHero,
                        color = c.textPrimary,
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = Spacing.s),
                    )
                }

                SectionHeader(stringResource(R.string.reminder_repeat), Modifier.padding(top = Spacing.s))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        DayToggle(day, selected = day in state.days, onToggle = { actions.onToggleDay(day) }, modifier = Modifier.weight(1f))
                    }
                }
                Text(
                    stringResource(if (state.days.isEmpty()) R.string.reminder_once else R.string.reminder_weekly),
                    style = MyFitTheme.type.caption,
                    color = c.textSecondary,
                )
                if (state.days.isEmpty()) {
                    SecondaryButton(
                        stringResource(R.string.reminder_date, Dates.medium(state.date)),
                        onClick = { pickDate = true },
                        icon = Icons.Rounded.CalendarMonth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.pastError) {
                    Text(stringResource(R.string.reminder_past_error), style = MyFitTheme.type.caption, color = c.danger)
                }
                if (!state.isNew) {
                    SecondaryButton(
                        stringResource(R.string.action_delete),
                        onClick = { confirmDelete = true },
                        icon = Icons.Rounded.Delete,
                        contentColor = c.danger,
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.l),
                    )
                }
                Box(Modifier.padding(bottom = Spacing.xxl))
            }
        }
    }
    if (pickTime) {
        TimeDialog(
            minuteOfDay = state.minuteOfDay,
            is24Hour = is24Hour,
            onConfirm = { actions.onTime(it); pickTime = false },
            onDismiss = { pickTime = false },
        )
    }
    if (pickDate) {
        DateDialog(
            date = state.date,
            today = state.today,
            onConfirm = { actions.onDate(it); pickDate = false },
            onDismiss = { pickDate = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.reminder_delete_title),
            message = stringResource(R.string.reminder_delete_message),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Equal-width weekday chip (seven fit one row on a phone); same look as [MyFitChip]. */
@Composable
private fun DayToggle(day: DayOfWeek, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val fullName = day.getDisplayName(TextStyle.FULL, appLocale)
    Surface(
        checked = selected,
        onCheckedChange = { onToggle() },
        modifier = modifier.height(44.dp).semantics { contentDescription = fullName },
        shape = MyFitShapes.pill,
        color = if (selected) c.textPrimary else c.surface,
        contentColor = if (selected) c.background else c.textSecondary,
        border = BorderStroke(1.dp, if (selected) c.textPrimary else c.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(dayShort(day), style = MyFitTheme.type.label, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(minuteOfDay: Int, is24Hour: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val c = MyFitTheme.colors
    val pickerState = rememberTimePickerState(initialHour = minuteOfDay / 60, initialMinute = minuteOfDay % 60, is24Hour = is24Hour)
    var mode by remember { mutableStateOf(TimePickerDisplayMode.Picker) }
    // The dialog sits on surfaceRaised: give the dial and the unselected fields a contrasting surface.
    val colors = TimePickerDefaults.colors(
        clockDialColor = c.surface,
        selectorColor = c.accent,
        clockDialSelectedContentColor = c.onAccent,
        clockDialUnselectedContentColor = c.textPrimary,
        periodSelectorBorderColor = c.outlineStrong,
        periodSelectorSelectedContainerColor = c.accentMuted,
        periodSelectorSelectedContentColor = c.textPrimary,
        periodSelectorUnselectedContentColor = c.textSecondary,
        timeSelectorSelectedContainerColor = c.accentMuted,
        timeSelectorUnselectedContainerColor = c.surface,
        timeSelectorSelectedContentColor = c.textPrimary,
        timeSelectorUnselectedContentColor = c.textPrimary,
    )
    // TimePickerDialog (material3 1.4) ignores containerColor and paints colorScheme.surface with a tonal tint
    // (green, from the accent): give it surfaceRaised, untinted, like the app's other dialogs.
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = c.surfaceRaised)) {
        CompositionLocalProvider(LocalTonalElevationEnabled provides false) {
            TimePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = { TextAction(stringResource(R.string.action_save), onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) },
                dismissButton = { TextAction(stringResource(R.string.action_cancel), onClick = onDismiss, color = c.textSecondary) },
                title = { TimePickerDialogDefaults.Title(displayMode = mode) },
                modeToggleButton = {
                    TimePickerDialogDefaults.DisplayModeToggle(
                        onDisplayModeChange = { mode = if (mode == TimePickerDisplayMode.Picker) TimePickerDisplayMode.Input else TimePickerDisplayMode.Picker },
                        displayMode = mode,
                    )
                },
                containerColor = c.surfaceRaised,
            ) {
                if (mode == TimePickerDisplayMode.Picker) TimePicker(pickerState, colors = colors) else TimeInput(pickerState, colors = colors)
            }
        }
    }
}

/** Date picker in UTC millis (as the Material picker expects); days before today are disabled. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDialog(date: LocalDate, today: LocalDate, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = MyFitTheme.colors
    val todayMs = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(todayMs) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMs
                override fun isSelectableYear(year: Int) = year >= today.year
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextAction(stringResource(R.string.action_save), onClick = {
                pickerState.selectedDateMillis?.let { onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            })
        },
        dismissButton = { TextAction(stringResource(R.string.action_cancel), onClick = onDismiss, color = c.textSecondary) },
    ) { DatePicker(state = pickerState) }
}
