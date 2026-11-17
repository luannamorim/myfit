package io.github.luannamorim.myfit.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

/** Time picker dialog in the app's style (reminders, meals); returns minutes after midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(minuteOfDay: Int, is24Hour: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
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
