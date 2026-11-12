package io.github.luannamorim.myfit.data.prefs

import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Workout reminders, stored in [SettingsRepository] (so the settings backup carries them). */
@Singleton
class ReminderRepository @Inject constructor(private val settings: SettingsRepository) {

    val reminders: Flow<List<Reminder>> = settings.settings.map { it.reminders }.distinctUntilChanged()

    suspend fun all(): List<Reminder> = settings.current().reminders

    suspend fun get(id: Long): Reminder? = all().firstOrNull { it.id == id }

    /** Inserts ([Reminder.id] == 0, gets a new id) or replaces the reminder with the same id. Returns the id. */
    suspend fun save(reminder: Reminder): Long {
        var id = reminder.id
        settings.update { s ->
            if (id == 0L) id = ReminderSchedule.nextId(s.reminders)
            val saved = reminder.copy(id = id)
            val replaced = s.reminders.map { if (it.id == id) saved else it }
            s.copy(reminders = if (s.reminders.any { it.id == id }) replaced else s.reminders + saved)
        }
        return id
    }

    suspend fun delete(id: Long) = settings.update { s -> s.copy(reminders = s.reminders.filterNot { it.id == id }) }

    suspend fun setEnabled(id: Long, enabled: Boolean) = settings.update { s ->
        s.copy(reminders = s.reminders.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }
}
