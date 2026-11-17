package io.github.luannamorim.myfit.platform.nutrition

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminderSchedule
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One alarm per enabled meal time and one for the next water slot, each for its next occurrence only (the
 * receiver arms the following one). Exact and allowed in Doze when the app may use exact alarms.
 */
@Singleton
class NutritionReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val time: TimeSource,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Meal ids armed by this process, so meals deleted since then get their alarms cancelled. */
    private val armedMeals = mutableSetOf<Long>()

    @Synchronized
    fun rescheduleAll(reminders: NutritionReminders) {
        val now = now()
        val keep = reminders.meals.filter { armMeal(it, now) }.map { it.id }.toSet()
        (armedMeals - keep).forEach { cancel(mealIntent(it)) }
        armedMeals.clear()
        armedMeals += keep
        armWater(reminders.water, now)
    }

    /** Arms [meal]'s next occurrence strictly after [after]. */
    @Synchronized
    fun scheduleMealNext(meal: MealReminder, after: ZonedDateTime) {
        if (armMeal(meal, after)) armedMeals += meal.id else armedMeals -= meal.id
    }

    /** Arms the water reminder's next slot strictly after [after]. */
    @Synchronized
    fun scheduleWaterNext(water: WaterReminder, after: ZonedDateTime) {
        armWater(water, after)
    }

    private fun armMeal(meal: MealReminder, after: ZonedDateTime): Boolean {
        val next = NutritionReminderSchedule.nextMeal(meal, after)
        val intent = mealIntent(meal.id)
        if (next == null) {
            cancel(intent)
            return false
        }
        arm(intent, next)
        return true
    }

    private fun armWater(water: WaterReminder, after: ZonedDateTime) {
        val next = NutritionReminderSchedule.nextWater(water, after)
        if (next == null) cancel(waterIntent()) else arm(waterIntent(), next)
    }

    private fun arm(intent: Intent, at: ZonedDateTime) {
        val atMs = at.toInstant().toEpochMilli()
        val operation = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent.putExtra(NutritionReminderReceiver.EXTRA_TRIGGER_AT, atMs),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
        }
    }

    private fun cancel(intent: Intent) {
        val existing = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    /** Unique per meal (the data URI), so each one gets its own PendingIntent. */
    private fun mealIntent(id: Long): Intent = Intent(context, NutritionReminderReceiver::class.java)
        .setAction(NutritionReminderReceiver.ACTION_MEAL)
        .setData("myfit://nutrition/meal/$id".toUri())
        .putExtra(NutritionReminderReceiver.EXTRA_MEAL_ID, id)

    private fun waterIntent(): Intent = Intent(context, NutritionReminderReceiver::class.java)
        .setAction(NutritionReminderReceiver.ACTION_WATER)
        .setData("myfit://nutrition/water".toUri())

    private fun now(): ZonedDateTime = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone())

    private companion object {
        /** The data URI tells the PendingIntents apart; one request code is enough. */
        const val REQUEST_CODE = 50
    }
}
