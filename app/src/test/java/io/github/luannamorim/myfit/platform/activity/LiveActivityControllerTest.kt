package io.github.luannamorim.myfit.platform.activity

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.LiveActivityStore
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.domain.activity.GpsFix
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LiveActivityControllerTest {
    @get:Rule val tmp = TemporaryFolder()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val activities = ActivityRepository(db, time, BackupTrigger { })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file by lazy { File(tmp.root, "live.preferences_pb") }
    private val dataStore by lazy { PreferenceDataStoreFactory.create(scope = scope) { file } }

    private class FakeStarter : ActivityServiceStarter {
        var running = false
        override fun start() { running = true }
        override fun stop() { running = false }
    }

    private val starter = FakeStarter()
    private fun controller() = LiveActivityController(LiveActivityStore(dataStore), activities, time, starter, scope)

    @After fun tearDown() {
        db.close()
        scope.cancel()
    }

    private fun fix(metersNorth: Double, seconds: Long) = GpsFix(-23.55 + metersNorth / 111_195.0, -46.63, seconds * 1000)

    @Test
    fun runWithGps_pause_finish_logsTimeAndDistance() = runBlocking {
        val c = controller()
        c.start(Sport.RUNNING, null, gps = true)
        assertTrue(starter.running)
        c.onFix(fix(0.0, 0), 5f)
        time.advance(10 * 60_000)
        c.onFix(fix(1_250.0, 400), 5f) // 1.25 km in 400 s
        c.pause()
        time.advance(5 * 60_000) // paused time doesn't count
        c.resume()
        time.advance(90_000)

        val id = c.finish()!!
        val saved = activities.get(id)!!
        assertEquals(Sport.RUNNING, saved.sport)
        assertEquals(12, saved.durationMinutes) // 11.5 min rounds up
        assertEquals(1.25, saved.distanceKm!!, 0.01)
        assertEquals(time.today(), saved.date)
        assertNull(c.live.value)
        assertFalse(starter.running)
    }

    @Test
    fun survivesProcessDeath_withTheRightTime() = runBlocking {
        controller().start(Sport.SWIMMING, null, gps = true) // swimming: no GPS even if asked
        time.advance(20 * 60_000)

        val again = controller() // new process: state comes back from disk
        val live = again.current()!!
        assertEquals(Sport.SWIMMING, live.sport)
        assertFalse(live.gps)
        assertEquals(20 * 60_000L, live.elapsedMs(time.now()))
        again.resync()
        assertTrue(starter.running)
    }

    @Test
    fun startKeepsTheRunningOne_andDiscardSavesNothing() = runBlocking {
        val c = controller()
        c.start(Sport.OTHER, "  Remo  ", gps = false)
        c.start(Sport.CYCLING, null, gps = true) // already running: unchanged
        assertEquals("Remo", c.live.value!!.name)
        c.discard()
        assertNull(c.current())
        assertTrue(activities.observe(time.today().minusDays(1)..time.today()).first().isEmpty())
    }
}
