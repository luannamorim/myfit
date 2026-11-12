package io.github.luannamorim.myfit.ui.feature.reminders

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** A reminder as listed; [past] = one-off whose moment has gone by (shown off until edited). */
data class ReminderRow(val reminder: Reminder, val past: Boolean = false) {
    val active: Boolean get() = reminder.enabled && !past
}

data class RemindersUiState(
    val loading: Boolean = true,
    val rows: List<ReminderRow> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val notificationsEnabled: Boolean = true,
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReminderRepository,
    private val time: TimeSource,
) : ViewModel() {
    private val notificationsEnabled = MutableStateFlow(readNotificationsEnabled())

    val state: StateFlow<RemindersUiState> = combine(repository.reminders, notificationsEnabled) { reminders, notifications ->
        val now = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone())
        RemindersUiState(
            loading = false,
            rows = reminders.sortedWith(ORDER).map { ReminderRow(it, past = ReminderSchedule.isPast(it, now)) },
            today = now.toLocalDate(),
            notificationsEnabled = notifications,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RemindersUiState())

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    /** Re-read on resume: the user may turn notifications on or off in system settings. */
    fun refreshSystem() {
        notificationsEnabled.value = readNotificationsEnabled()
    }

    private fun readNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    private companion object {
        /** Weekly ones first by time of day, then one-offs by date and time. */
        val ORDER = compareBy<Reminder>({ !it.isRepeating }, { it.dateEpochDay ?: 0L }, { it.minuteOfDay }, { it.id })
    }
}
