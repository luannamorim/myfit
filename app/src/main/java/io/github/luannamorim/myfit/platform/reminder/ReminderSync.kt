package io.github.luannamorim.myfit.platform.reminder

import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import io.github.luannamorim.myfit.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the alarms in step with the stored reminders: re-arms all of them at app start and whenever the
 * list changes (editor, list switches, backup restore). Started from the Application.
 */
@Singleton
class ReminderSync @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val notifications: ReminderNotifications,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch {
            notifications.ensureChannel()
            repository.reminders.collect { scheduler.rescheduleAll(it) }
        }
    }
}
