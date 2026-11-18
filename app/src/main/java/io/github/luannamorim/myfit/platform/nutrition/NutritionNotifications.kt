package io.github.luannamorim.myfit.platform.nutrition

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import javax.inject.Inject
import javax.inject.Singleton

/** "Time to eat" and "Time to drink water" notifications, on their own channel (muted apart from workout reminders). */
@Singleton
class NutritionNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_NUTRITION, context.getString(R.string.channel_nutrition), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_nutrition_desc)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** "Time to eat", with "Log meal" (opens the meal editor for today). False when notifications are off. */
    fun postMeal(mealId: Long, label: String, whenMs: Long): Boolean {
        // One PendingIntent per meal (data URI), carrying the id so MainActivity clears this notification:
        // action buttons don't auto-cancel.
        val logMeal = PendingIntent.getActivity(
            context,
            REQ_ADD_MEAL,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_ADD_MEAL)
                .setData("myfit://nutrition/log-meal/$mealId".toUri())
                .putExtra(EXTRA_MEAL_NOTIFICATION, mealId)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val action = NotificationCompat.Action(0, context.getString(R.string.nutrition_notif_meal_action), logMeal)
        val title = if (label.isBlank()) {
            context.getString(R.string.nutrition_notif_meal_title)
        } else {
            context.getString(R.string.nutrition_notif_meal_title_named, label.trim())
        }
        return post(TAG_MEAL, mealId.hashCode(), title, context.getString(R.string.nutrition_notif_meal_text), whenMs, action)
    }

    /** "Time to drink water · 1.5 of 2.8 L today", with "+250 ml" (logs without opening the app). */
    fun postWater(waterMl: Int, targetMl: Int?, buttonMl: Int, whenMs: Long): Boolean {
        val text = if (targetMl != null) {
            context.getString(R.string.nutrition_notif_water_progress, liters(waterMl), liters(targetMl))
        } else {
            context.getString(R.string.nutrition_notif_water_total, liters(waterMl))
        }
        val add = PendingIntent.getBroadcast(
            context,
            REQ_ADD_WATER,
            Intent(context, NutritionReminderReceiver::class.java).setAction(NutritionReminderReceiver.ACTION_ADD_WATER),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val action = NotificationCompat.Action(0, context.getString(R.string.nutrition_add_ml, buttonMl), add)
        return post(TAG_WATER, WATER_ID, context.getString(R.string.nutrition_notif_water_title), text, whenMs, action)
    }

    fun cancelWater() = manager.cancel(TAG_WATER, WATER_ID)

    fun cancelMeal(mealId: Long) = manager.cancel(TAG_MEAL, mealId.hashCode())

    private fun post(tag: String, id: Int, title: String, text: String, whenMs: Long, action: NotificationCompat.Action): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_NUTRITION)
            .setSmallIcon(R.drawable.ic_stat_myfit)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(activityIntent(MainActivity.ACTION_OPEN_NUTRITION, REQ_OPEN))
            .addAction(action)
            .setAutoCancel(true)
            .setWhen(whenMs)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        @Suppress("MissingPermission")
        manager.notify(tag, id, notification)
        return true
    }

    private fun activityIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).setAction(action).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun liters(ml: Int): String = DecimalInput.format(ml / 1000.0)

    companion object {
        const val CHANNEL_NUTRITION = "nutrition"
        const val TAG_MEAL = "nutrition_meal"
        const val TAG_WATER = "nutrition_water"
        /** On "Log meal": the meal reminder whose notification to clear. */
        const val EXTRA_MEAL_NOTIFICATION = "meal_notification"
        private const val WATER_ID = 1
        private const val REQ_OPEN = 40
        private const val REQ_ADD_MEAL = 41
        private const val REQ_ADD_WATER = 42
    }
}
