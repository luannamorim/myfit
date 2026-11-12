package io.github.luannamorim.myfit.platform.session

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.testutil.FakeServiceStarter
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.insertExercise
import io.github.luannamorim.myfit.testutil.insertRoutine
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SessionPlatformTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val repo = SessionRepository(db, BackupTrigger { })
    private val notifications = SessionNotifications(context)
    private val alarms = AndroidRestAlarmScheduler(context, notifications)
    private val starter = FakeServiceStarter()
    private val controller = SessionController(repo, time, alarms, starter, notifications)
    private val alarmShadow get() = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val notificationShadow get() = shadowOf(context.getSystemService(NotificationManager::class.java))

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifications.ensureChannels()
    }

    @After fun tearDown() = db.close()

    private fun startWorkout(): Long = runBlocking {
        val a = db.insertExercise("Remada curvada")
        val b = db.insertExercise("Pullover")
        controller.start(db.insertRoutine("Costas", listOf(a, b)))
    }

    @Test
    fun start_startsService() {
        startWorkout()
        assertTrue(starter.running)
    }

    @Test
    fun completeSet_schedulesAlarmAtRestEnd() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        val alarm = alarmShadow.peekNextScheduledAlarm()
        assertNotNull(alarm)
        assertEquals(time.now().wallMs + 90_000, alarm!!.triggerAtMs)
    }

    @Test
    fun skipRest_cancelsAlarm() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        controller.skipRest()
        assertNull(alarmShadow.peekNextScheduledAlarm())
    }

    @Test
    fun pause_cancelsAlarm_resume_reschedulesRemaining() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        time.advance(30_000)
        controller.pause()
        assertNull(alarmShadow.peekNextScheduledAlarm())
        time.advance(600_000)
        controller.resume()
        assertEquals(time.now().wallMs + 60_000, alarmShadow.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun adjustRest_reschedules() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        controller.adjustRest(15)
        assertEquals(time.now().wallMs + 105_000, alarmShadow.peekNextScheduledAlarm()!!.triggerAtMs)
    }

    @Test
    fun restEndHandler_postsRestEndNotification_onlyWhenRestIsOver() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        val handler = RestEndHandler(repo, testSettings(), notifications, time)

        handler.onRestAlarm() // too early: stale alarm, nothing posted
        assertTrue(notificationShadow.allNotifications.none { it.channelId == SessionNotifications.CHANNEL_REST_END })

        time.advance(90_000)
        handler.onRestAlarm()
        val restEnd = notificationShadow.allNotifications.single { it.channelId == SessionNotifications.CHANNEL_REST_END }
        assertEquals("Descanso concluído", shadowOf(restEnd).contentTitle)
        assertEquals("A seguir: Remada curvada · série 2 de 3", shadowOf(restEnd).contentText)
    }

    @Test
    fun restEnd_alwaysSound_usesAlarmChannel() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        notifications.postRestEnd(repo.getActive()!!, RestSound.ALWAYS)
        assertTrue(notificationShadow.allNotifications.any { it.channelId == SessionNotifications.CHANNEL_REST_END_ALARM })
    }

    @Test
    fun completeSetAsPlanned_advancesSession() = runBlocking {
        startWorkout()
        controller.completeSetAsPlanned()
        val set = repo.getActive()!!.exercises[0].sets[0]
        assertEquals(ItemStatus.DONE, set.status)
        assertEquals(12, set.actualReps)
        assertEquals(40.0, set.actualLoadKg!!, 0.0)
    }

    @Test
    fun ongoingNotification_restingCountsDown_pausedHasNoChronometer() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        val resting = notifications.ongoing(repo.getActive()!!, time.now())
        assertTrue(resting.extras.getBoolean("android.chronometerCountDown"))
        assertEquals(time.now().wallMs + 90_000, resting.`when`)
        assertEquals("Pular descanso", resting.actions.single().title)

        controller.pause()
        val paused = notifications.ongoing(repo.getActive()!!, time.now())
        assertFalse(paused.extras.getBoolean("android.showChronometer"))
        assertNull(paused.actions)
    }

    @Test
    fun resync_withActiveSession_startsService_andReschedules() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        starter.stop()
        alarms.cancel()
        controller.resync()
        assertTrue(starter.running)
        assertNotNull(alarmShadow.peekNextScheduledAlarm())
    }

    @Test
    fun finish_stopsService_andCancelsAlarm() = runBlocking {
        startWorkout()
        controller.completeSet(12, 40.0)
        val id = controller.finish(updateRoutineLoads = false)
        assertNotNull(id)
        assertFalse(starter.running)
        assertNull(alarmShadow.peekNextScheduledAlarm())
        assertNull(repo.getActive())
    }

    @Test
    fun discard_removesSession() = runBlocking {
        startWorkout()
        controller.discard()
        assertNull(repo.getActive())
        assertFalse(starter.running)
    }
}
