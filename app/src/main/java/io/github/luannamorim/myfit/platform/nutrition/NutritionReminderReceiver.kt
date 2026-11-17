package io.github.luannamorim.myfit.platform.nutrition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.data.repository.NutritionTracker
import io.github.luannamorim.myfit.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What runs when a nutrition alarm fires or "+250 ml" is tapped (kept out of the receiver for testability). */
@Singleton
class NutritionReminderHandler @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: NutritionReminderScheduler,
    private val notifications: NutritionNotifications,
    private val tracker: NutritionTracker,
    private val nutrition: NutritionRepository,
    private val time: TimeSource,
) {
    /** Notifies "time to eat", then arms tomorrow's. Alarms of meals deleted or turned off meanwhile are ignored. */
    suspend fun onMealAlarm(id: Long, triggerAtMs: Long) {
        val meal = settings.current().nutritionReminders.meals.firstOrNull { it.id == id && it.enabled } ?: return
        val nowMs = time.now().wallMs
        notifications.postMeal(meal.id, whenMs = if (triggerAtMs > 0) triggerAtMs else nowMs)
        scheduler.scheduleMealNext(meal, after(nowMs, triggerAtMs))
    }

    /** Notifies "drink water" unless today's target is already reached, then arms the next slot. */
    suspend fun onWaterAlarm(triggerAtMs: Long) {
        val water = settings.current().nutritionReminders.water.takeIf { it.enabled } ?: return
        val nowMs = time.now().wallMs
        val status = tracker.observe(time.today()).first()
        val target = status.targets.waterMl
        if (target == null || status.day.waterMl < target) {
            notifications.postWater(status.day.waterMl, target, whenMs = if (triggerAtMs > 0) triggerAtMs else nowMs)
        } else {
            // Target reached: an earlier reminder still in the shade would over-log with its "+250 ml".
            notifications.cancelWater()
        }
        scheduler.scheduleWaterNext(water, after(nowMs, triggerAtMs))
    }

    /** "+250 ml" on the notification: logs it for today and clears the notification. */
    suspend fun onAddWater() {
        nutrition.addWater(time.today(), NutritionNotifications.WATER_STEP_ML)
        notifications.cancelWater()
    }

    /** After a reboot, an app update or a clock/time zone change. */
    suspend fun rescheduleAll() = scheduler.rescheduleAll(settings.current().nutritionReminders)

    /** Never before the planned moment, so an alarm delivered a bit early doesn't fire twice. */
    private fun after(nowMs: Long, triggerAtMs: Long) = Instant.ofEpochMilli(maxOf(nowMs, triggerAtMs)).atZone(time.zone())
}

/** Nutrition alarms, the "+250 ml" action, and re-arming after boot, app updates and clock changes. */
@AndroidEntryPoint
class NutritionReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: NutritionReminderHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
        val work: suspend () -> Unit = when (action) {
            ACTION_MEAL -> intent.getLongExtra(EXTRA_MEAL_ID, -1L).takeIf { it > 0 }?.let { id -> suspend { handler.onMealAlarm(id, triggerAt) } } ?: return
            ACTION_WATER -> suspend { handler.onWaterAlarm(triggerAt) }
            ACTION_ADD_WATER -> suspend { handler.onAddWater() }
            in RESCHEDULE_ACTIONS -> suspend { handler.rescheduleAll() }
            else -> return
        }
        val pending = goAsync()
        scope.launch {
            try {
                work()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MEAL = "io.github.luannamorim.myfit.action.NUTRITION_MEAL"
        const val ACTION_WATER = "io.github.luannamorim.myfit.action.NUTRITION_WATER"
        const val ACTION_ADD_WATER = "io.github.luannamorim.myfit.action.NUTRITION_ADD_WATER"
        const val EXTRA_MEAL_ID = "meal_reminder_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

/** Re-arms the nutrition alarms at app start and whenever they change (editor, backup restore). Started from the Application. */
@Singleton
class NutritionReminderSync @Inject constructor(
    private val settings: SettingsRepository,
    private val scheduler: NutritionReminderScheduler,
    private val notifications: NutritionNotifications,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    @Synchronized
    fun start() {
        if (job != null) return
        job = scope.launch {
            notifications.ensureChannel()
            settings.settings.map { it.nutritionReminders }.distinctUntilChanged().collect { scheduler.rescheduleAll(it) }
        }
    }
}
