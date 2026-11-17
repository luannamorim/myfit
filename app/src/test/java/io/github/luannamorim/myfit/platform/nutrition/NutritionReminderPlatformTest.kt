package io.github.luannamorim.myfit.platform.nutrition

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.data.repository.NutritionTracker
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR")
class NutritionReminderPlatformTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun ms(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    // Tuesday, 2026-11-17, 10:00 in Sao Paulo.
    private val time = FakeTimeSource(Mark(wallMs = ms("2026-11-17T10:00"), elapsedMs = 1_000, bootCount = 1), zone)
    private val today = LocalDate.of(2026, 11, 17)
    private val db = inMemoryDb()
    private val settings = testSettings()
    private val nutrition = NutritionRepository(db, time, BackupTrigger { })
    private val body = BodyRepository(db.bodyDao(), time, BackupTrigger { })
    private val tracker = NutritionTracker(
        nutrition, body, HistoryRepository(db, time, BackupTrigger { }), ActivityRepository(db, time, BackupTrigger { }), settings,
    )
    private val scheduler = NutritionReminderScheduler(context, time)
    private val notifications = NutritionNotifications(context)
    private val handler = NutritionReminderHandler(settings, scheduler, notifications, tracker, nutrition, time)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val alarmShadow get() = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)
    private val notificationShadow get() = shadowOf(notificationManager)

    private val reminders = NutritionReminders(
        meals = listOf(MealReminder(1, 12 * 60), MealReminder(2, 20 * 60), MealReminder(3, 7 * 60, enabled = false)),
        water = WaterReminder(enabled = true, everyMinutes = 120, fromMinute = 8 * 60, untilMinute = 22 * 60),
    )

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Suppress("DEPRECATION")
    private val ShadowAlarmManager.ScheduledAlarm.intent: Intent get() = shadowOf(operation!!).savedIntent

    private fun mealAlarm(id: Long) = alarmShadow.scheduledAlarms.singleOrNull {
        it.intent.action == NutritionReminderReceiver.ACTION_MEAL && it.intent.getLongExtra(NutritionReminderReceiver.EXTRA_MEAL_ID, -1) == id
    }

    private fun waterAlarm() = alarmShadow.scheduledAlarms.singleOrNull { it.intent.action == NutritionReminderReceiver.ACTION_WATER }

    private fun setNow(text: String) {
        time.current = time.current.copy(wallMs = ms(text))
    }

    private suspend fun store(r: NutritionReminders = reminders) = settings.update { it.copy(nutritionReminders = r) }

    @Test
    fun rescheduleAll_armsEnabledMeals_andTheNextWaterSlot_cancellingTheRest() = runBlocking {
        scheduler.rescheduleAll(reminders)
        assertEquals(ms("2026-11-17T12:00"), mealAlarm(1)!!.triggerAtMs)
        assertEquals(ms("2026-11-17T20:00"), mealAlarm(2)!!.triggerAtMs)
        assertEquals(null, mealAlarm(3)) // off
        assertEquals(ms("2026-11-17T12:00"), waterAlarm()!!.triggerAtMs)
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, waterAlarm()!!.windowLengthMs)

        scheduler.rescheduleAll(reminders.copy(meals = reminders.meals.take(1), water = reminders.water.copy(enabled = false)))
        assertEquals(null, mealAlarm(2))
        assertEquals(null, waterAlarm())
        assertEquals(1, alarmShadow.scheduledAlarms.size)
    }

    @Test
    fun mealAlarm_postsTimeToEat_withLogMeal_andArmsTomorrow() = runBlocking {
        store()
        setNow("2026-11-17T12:00:01")
        handler.onMealAlarm(1, ms("2026-11-17T12:00"))

        val posted = notificationShadow.allNotifications.single()
        assertEquals(NutritionNotifications.CHANNEL_NUTRITION, posted.channelId)
        assertEquals("Nutrição", notificationManager.getNotificationChannel(NutritionNotifications.CHANNEL_NUTRITION).name)
        assertEquals("Hora de comer", shadowOf(posted).contentTitle)
        val action = posted.actions.single()
        assertEquals("Registrar refeição", action.title)
        assertEquals(MainActivity.ACTION_ADD_MEAL, shadowOf(action.actionIntent).savedIntent.action)
        assertEquals(MainActivity.ACTION_OPEN_NUTRITION, shadowOf(posted.contentIntent).savedIntent.action)
        assertEquals(ms("2026-11-18T12:00"), mealAlarm(1)!!.triggerAtMs)
    }

    @Test
    fun waterAlarm_postsWithTodaysProgress_andAPlus250Button() = runBlocking {
        store()
        body.save(BodyAssessment(date = today.minusDays(2), values = mapOf(Measure.WEIGHT to 80.0)))
        nutrition.addWater(today, 1500)
        setNow("2026-11-17T12:00:01")
        handler.onWaterAlarm(ms("2026-11-17T12:00"))

        val posted = notificationShadow.allNotifications.single()
        assertEquals("Hora de beber água", shadowOf(posted).contentTitle)
        assertEquals("1,5 de 2,8 L hoje", shadowOf(posted).contentText)
        val action = posted.actions.single()
        assertEquals("+250 ml", action.title)
        assertEquals(NutritionReminderReceiver.ACTION_ADD_WATER, shadowOf(action.actionIntent).savedIntent.action)
        assertEquals(ms("2026-11-17T14:00"), waterAlarm()!!.triggerAtMs)
    }

    @Test
    fun waterAlarm_skipsTheSlot_whenTodaysTargetIsReached() = runBlocking {
        store()
        body.save(BodyAssessment(date = today.minusDays(2), values = mapOf(Measure.WEIGHT to 80.0)))
        nutrition.addWater(today, 2800)
        setNow("2026-11-17T12:00:01")
        handler.onWaterAlarm(ms("2026-11-17T12:00"))
        assertTrue(notificationShadow.allNotifications.isEmpty())
        assertEquals(ms("2026-11-17T14:00"), waterAlarm()!!.triggerAtMs)
    }

    @Test
    fun plus250_addsWaterToday_andClearsTheNotification() = runBlocking {
        store()
        setNow("2026-11-17T12:00:01")
        handler.onWaterAlarm(ms("2026-11-17T12:00"))
        assertEquals(1, notificationShadow.allNotifications.size)

        handler.onAddWater()
        assertEquals(250, nutrition.observeDay(today).first().waterMl)
        assertTrue(notificationShadow.allNotifications.isEmpty())
    }

    @Test
    fun alarmsOfRemindersTurnedOffMeanwhile_areIgnored() = runBlocking {
        store(reminders.copy(meals = emptyList(), water = reminders.water.copy(enabled = false)))
        handler.onMealAlarm(1, ms("2026-11-17T12:00"))
        handler.onWaterAlarm(ms("2026-11-17T12:00"))
        assertTrue(notificationShadow.allNotifications.isEmpty())
        assertTrue(alarmShadow.scheduledAlarms.isEmpty())
    }

    @Test
    fun sync_armsAtStart_andFollowsChanges() = runBlocking {
        store()
        NutritionReminderSync(settings, scheduler, notifications, scope).start()
        awaitAlarms { it.size == 3 }
        assertNotNull(notificationManager.getNotificationChannel(NutritionNotifications.CHANNEL_NUTRITION))
        store(reminders.copy(water = reminders.water.copy(enabled = false)))
        awaitAlarms { it.size == 2 && waterAlarm() == null }
    }

    @Test
    fun receiver_isRegisteredForBootClockAndUpdateBroadcasts() {
        NutritionReminderReceiver.RESCHEDULE_ACTIONS.forEach { action ->
            val receivers = context.packageManager.queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
            assertTrue(action, receivers.any { it.activityInfo.name == NutritionReminderReceiver::class.java.name })
        }
    }

    private suspend fun awaitAlarms(predicate: (List<ShadowAlarmManager.ScheduledAlarm>) -> Boolean) {
        withTimeout(5_000) {
            while (!predicate(alarmShadow.scheduledAlarms)) delay(10)
        }
    }

}
