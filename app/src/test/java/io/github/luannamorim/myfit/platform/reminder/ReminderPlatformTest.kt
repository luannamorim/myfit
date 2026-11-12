package io.github.luannamorim.myfit.platform.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReminderPlatformTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun ms(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    // Thursday, 2026-11-12, 10:00 in Sao Paulo.
    private val time = FakeTimeSource(Mark(wallMs = ms("2026-11-12T10:00"), elapsedMs = 1_000, bootCount = 1), zone)
    private val settings = testSettings()
    private val repo = ReminderRepository(settings)
    private val scheduler = ReminderScheduler(context, time)
    private val notifications = ReminderNotifications(context)
    private val handler = ReminderAlarmHandler(repo, notifications, scheduler, time)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val alarmShadow get() = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)
    private val notificationShadow get() = shadowOf(notificationManager)

    private val weekly = Reminder(id = 1, routineId = 3, label = "Costas", minuteOfDay = 18 * 60, days = setOf(THURSDAY, SATURDAY))
    private val once = Reminder(id = 2, label = "Corrida", minuteOfDay = 7 * 60, dateEpochDay = LocalDate.parse("2026-11-14").toEpochDay())

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After fun tearDown() = scope.cancel()

    /** The alarm's PendingIntent (Robolectric only exposes it as a deprecated field). */
    @Suppress("DEPRECATION")
    private val ShadowAlarmManager.ScheduledAlarm.pendingIntent: PendingIntent get() = operation!!

    private val ShadowAlarmManager.ScheduledAlarm.reminderId: Long
        get() = shadowOf(pendingIntent).savedIntent.getLongExtra(ReminderReceiver.EXTRA_ID, -1)

    private fun alarmFor(id: Long): ShadowAlarmManager.ScheduledAlarm? = alarmShadow.scheduledAlarms.singleOrNull { it.reminderId == id }

    private fun setNow(text: String) {
        time.current = time.current.copy(wallMs = ms(text))
    }

    @Test
    fun rescheduleAll_armsOneExactAlarmPerEnabledReminder() {
        val disabled = once.copy(id = 3, enabled = false)
        val past = once.copy(id = 4, dateEpochDay = LocalDate.parse("2026-11-11").toEpochDay())
        scheduler.rescheduleAll(listOf(weekly, once, disabled, past))

        assertEquals(2, alarmShadow.scheduledAlarms.size)
        val alarm = alarmFor(1)!!
        assertEquals(ms("2026-11-12T18:00"), alarm.triggerAtMs)
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.getType())
        assertTrue(alarm.isAllowWhileIdle)
        assertEquals(ShadowAlarmManager.WINDOW_EXACT, alarm.windowLengthMs)
        assertEquals(ms("2026-11-14T07:00"), alarmFor(2)!!.triggerAtMs)

        val operation = shadowOf(alarm.pendingIntent)
        assertTrue(operation.isBroadcast)
        assertTrue(operation.flags and PendingIntent.FLAG_IMMUTABLE != 0)
        assertEquals(ReminderReceiver.ACTION_REMINDER, operation.savedIntent.action)
        assertEquals(ReminderReceiver::class.java.name, operation.savedIntent.component!!.className)
        assertEquals(ms("2026-11-12T18:00"), operation.savedIntent.getLongExtra(ReminderReceiver.EXTRA_TRIGGER_AT, 0))
        // One PendingIntent per reminder.
        assertNotEquals(alarm.pendingIntent, alarmFor(2)!!.pendingIntent)
    }

    @Test
    fun rescheduleAll_cancelsRemovedAndDisabled_andMovesChangedOnes() {
        scheduler.rescheduleAll(listOf(weekly, once))
        assertEquals(2, alarmShadow.scheduledAlarms.size)

        scheduler.rescheduleAll(listOf(weekly.copy(minuteOfDay = 19 * 60)))
        assertEquals(listOf(ms("2026-11-12T19:00")), alarmShadow.scheduledAlarms.map { it.triggerAtMs })

        scheduler.rescheduleAll(listOf(weekly.copy(enabled = false)))
        assertTrue(alarmShadow.scheduledAlarms.isEmpty())
    }

    @Test
    fun withoutExactAlarmAccess_armsAnInexactAlarm() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.rescheduleAll(listOf(weekly))
        val alarm = alarmShadow.scheduledAlarms.single()
        assertEquals(ms("2026-11-12T18:00"), alarm.triggerAtMs)
        assertTrue(alarm.isAllowWhileIdle)
        assertNotEquals(ShadowAlarmManager.WINDOW_EXACT, alarm.windowLengthMs)
    }

    @Test
    fun alarm_weekly_postsNotification_andArmsTheNextOccurrence() = runBlocking {
        repo.save(weekly)
        setNow("2026-11-12T18:00:01")
        handler.onAlarm(1, ms("2026-11-12T18:00"))

        val posted = notificationShadow.allNotifications.single()
        assertEquals(ReminderNotifications.CHANNEL_REMINDERS, posted.channelId)
        assertEquals("Hora de treinar", shadowOf(posted).contentTitle)
        assertEquals("Costas", shadowOf(posted).contentText)
        assertNotNull(posted.contentIntent)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, notificationManager.getNotificationChannel(ReminderNotifications.CHANNEL_REMINDERS).importance)
        assertEquals("Lembretes", notificationManager.getNotificationChannel(ReminderNotifications.CHANNEL_REMINDERS).name)
        // Thursday 18:00 fired: next is Saturday 18:00.
        assertEquals(ms("2026-11-14T18:00"), alarmFor(1)!!.triggerAtMs)
        assertTrue(repo.get(1)!!.enabled)
    }

    @Test
    fun alarm_deliveredEarly_doesNotRearmTheSameOccurrence() = runBlocking {
        repo.save(weekly)
        setNow("2026-11-12T17:59:58")
        handler.onAlarm(1, ms("2026-11-12T18:00"))
        assertEquals(ms("2026-11-14T18:00"), alarmFor(1)!!.triggerAtMs)
    }

    @Test
    fun alarm_oneOff_postsNotification_andTurnsTheReminderOff() = runBlocking {
        repo.save(once)
        setNow("2026-11-14T07:00:01")
        handler.onAlarm(2, ms("2026-11-14T07:00"))

        assertEquals("Corrida", shadowOf(notificationShadow.allNotifications.single()).contentText)
        assertFalse(repo.get(2)!!.enabled)
        assertNull(alarmFor(2))
    }

    @Test
    fun alarm_ofDeletedOrDisabledReminder_isIgnored() = runBlocking {
        repo.save(weekly.copy(enabled = false))
        handler.onAlarm(1, ms("2026-11-12T18:00"))
        handler.onAlarm(99, ms("2026-11-12T18:00"))
        assertTrue(notificationShadow.allNotifications.isEmpty())
        assertTrue(alarmShadow.scheduledAlarms.isEmpty())
    }

    @Test
    fun alarm_withNotificationsOff_postsNothing_butKeepsTheSchedule() = runBlocking {
        notificationShadow.setNotificationsEnabled(false)
        repo.save(weekly)
        setNow("2026-11-12T18:00:01")
        handler.onAlarm(1, ms("2026-11-12T18:00"))
        assertTrue(notificationShadow.allNotifications.isEmpty())
        assertEquals(ms("2026-11-14T18:00"), alarmFor(1)!!.triggerAtMs)
    }

    @Test
    fun rescheduleAll_fromStorage_armsEveryEnabledReminder() = runBlocking {
        repo.save(weekly)
        repo.save(once)
        repo.save(once.copy(id = 3, enabled = false))
        handler.rescheduleAll()
        assertEquals(setOf(1L, 2L), alarmShadow.scheduledAlarms.map { it.reminderId }.toSet())
    }

    @Test
    fun sync_armsAtStart_followsEdits_andBackupRestore() = runBlocking {
        repo.save(weekly)
        val sync = ReminderSync(repo, scheduler, notifications, scope)
        sync.start()
        awaitAlarms { it.size == 1 }
        assertNotNull(notificationManager.getNotificationChannel(ReminderNotifications.CHANNEL_REMINDERS))

        repo.save(once)
        awaitAlarms { it.size == 2 }
        repo.setEnabled(1, false)
        awaitAlarms { it.size == 1 && alarmFor(2) != null }

        // Restoring a backup replaces the settings, reminders included.
        val other = testSettings()
        other.update { it.copy(reminders = listOf(weekly.copy(id = 7, minuteOfDay = 20 * 60))) }
        settings.importJson(other.exportJson())
        awaitAlarms { it.size == 1 && alarmFor(7)?.triggerAtMs == ms("2026-11-12T20:00") }
    }

    @Test
    fun rescheduleReceiver_isRegisteredForBootClockAndUpdateBroadcasts() {
        ReminderRescheduleReceiver.ACTIONS.forEach { action ->
            val receivers = context.packageManager.queryBroadcastReceivers(Intent(action).setPackage(context.packageName), 0)
            assertTrue(action, receivers.any { it.activityInfo.name == ReminderRescheduleReceiver::class.java.name })
        }
    }

    private suspend fun awaitAlarms(predicate: (List<ShadowAlarmManager.ScheduledAlarm>) -> Boolean) {
        withTimeout(5_000) {
            while (!predicate(alarmShadow.scheduledAlarms)) delay(10)
        }
    }
}
