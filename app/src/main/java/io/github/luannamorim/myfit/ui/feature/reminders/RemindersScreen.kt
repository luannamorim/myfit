package io.github.luannamorim.myfit.ui.feature.reminders

import android.content.Intent
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.EmptyState
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSystem()
        onPauseOrDispose { }
    }
    RemindersContent(
        state = state,
        onBack = onBack,
        onCreate = onCreate,
        onEdit = onEdit,
        onToggle = viewModel::setEnabled,
        onOpenNotifications = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
    )
}

@Composable
fun RemindersContent(
    state: RemindersUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onOpenNotifications: () -> Unit,
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
) {
    val c = MyFitTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(R.string.reminders_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onCreate) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.reminders_new)) }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                contentPadding = PaddingValues(start = Spacing.screen, end = Spacing.screen, top = Spacing.s, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (!state.notificationsEnabled && state.rows.any { it.active }) {
                    item { NotificationsOffCard(onOpenNotifications) }
                }
                if (!state.loading && state.rows.isEmpty()) {
                    item {
                        EmptyState(
                            title = stringResource(R.string.reminders_empty_title),
                            message = stringResource(R.string.reminders_empty_message),
                            action = { PrimaryButton(stringResource(R.string.reminders_new), onClick = onCreate, icon = Icons.Rounded.Add) },
                        )
                    }
                }
                items(state.rows, key = { it.reminder.id }) { row ->
                    val id = row.reminder.id
                    ReminderCard(
                        row = row,
                        today = state.today,
                        is24Hour = is24Hour,
                        onClick = { onEdit(id) },
                        // A one-off that already went by needs a new date first.
                        onToggle = { on -> if (on && row.past) onEdit(id) else onToggle(id, on) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(row: ReminderRow, today: LocalDate, is24Hour: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val c = MyFitTheme.colors
    val reminder = row.reminder
    val switchDescription = stringResource(R.string.reminders_switch_a11y, reminder.label, timeText(reminder.minuteOfDay, is24Hour))
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                ReminderTime(
                    reminder.minuteOfDay,
                    is24Hour,
                    style = MyFitTheme.type.numericLarge,
                    color = if (row.active) c.textPrimary else c.textTertiary,
                )
                Text(
                    reminder.label,
                    style = MyFitTheme.type.bodyStrong,
                    color = if (row.active) c.textPrimary else c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(scheduleText(reminder, today), style = MyFitTheme.type.caption, color = c.textSecondary)
            }
            Spacer(Modifier.width(Spacing.m))
            Switch(
                checked = row.active,
                onCheckedChange = onToggle,
                modifier = Modifier.semantics { contentDescription = switchDescription },
                colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
            )
        }
    }
}

@Composable
private fun NotificationsOffCard(onOpen: () -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_notifications), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                Text(stringResource(R.string.reminders_notifications_off), style = MyFitTheme.type.caption, color = c.warning)
            }
            TextAction(stringResource(R.string.settings_open), onClick = onOpen)
        }
    }
}
