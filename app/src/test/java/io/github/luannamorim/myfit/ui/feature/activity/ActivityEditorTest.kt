package io.github.luannamorim.myfit.ui.feature.activity

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.prefs.LiveActivityStore
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.platform.activity.ActivityServiceStarter
import io.github.luannamorim.myfit.platform.activity.LiveActivityController
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.ui.navigation.ActivityEditorRoute
import java.io.File
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

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ActivityEditorTest {
    @get:Rule val main = MainDispatcherRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val activities = ActivityRepository(db, time, BackupTrigger { })
    private val images = ImageStore(context, Dispatchers.Unconfined)
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val live by lazy {
        val store = LiveActivityStore(PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "live.preferences_pb") })
        LiveActivityController(store, activities, time, object : ActivityServiceStarter {
            override fun start() = Unit
            override fun stop() = Unit
        }, scope)
    }

    @After fun tearDown() {
        db.close()
        scope.cancel()
    }

    private fun editor(route: ActivityEditorRoute = ActivityEditorRoute()) =
        ActivityEditorViewModel(SavedStateHandle(route = route), activities, images, time, live)

    @Test
    fun reviewAfterTimer_asksIntensityAndPhoto_keepingWhatWasMeasured() = runBlocking {
        val id = activities.save(Activity(sport = Sport.CYCLING, date = time.today(), durationMinutes = 52, distanceKm = 18.4))
        val vm = editor(ActivityEditorRoute(activityId = id, review = true))
        val loaded = vm.state.awaitUntil { !it.loading }
        assertTrue(loaded.review)
        vm.setIntensity(Intensity.HIGH)
        vm.setNotes("subida forte")
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        val saved = activities.get(id)!!
        assertEquals(Intensity.HIGH, saved.intensity)
        assertEquals("subida forte", saved.notes)
        assertEquals(52, saved.durationMinutes)
        assertEquals(18.4, saved.distanceKm!!, 0.0)
    }

    @Test
    fun startNow_startsTimingTheChosenSport_gpsOnlyWhenPermitted() = runBlocking {
        val vm = editor()
        vm.state.awaitUntil { !it.loading }
        vm.setSport(Sport.OTHER)
        assertFalse(vm.canStart()) // a free activity needs a name first
        vm.setName("Trilha")
        assertTrue(vm.canStart())
        vm.startLive(locationGranted = false)
        vm.state.awaitUntil { it.started }
        val running = live.current()!!
        assertEquals(Sport.OTHER, running.sport)
        assertEquals("Trilha", running.name)
        assertFalse(running.gps) // permission denied: time only
    }

    @Test
    fun newActivity_validates_thenSavesSwimmingDistanceInMeters() = runBlocking {
        val vm = editor(ActivityEditorRoute(dateEpochDay = time.today().minusDays(3).toEpochDay()))
        vm.state.awaitUntil { !it.loading }
        assertEquals(time.today().minusDays(3), vm.state.value.date) // History's selected day

        assertFalse(vm.save())
        assertTrue(vm.state.value.durationError)

        vm.setSport(Sport.SWIMMING)
        vm.setDuration("45")
        vm.setDistance("1500")
        vm.setIntensity(Intensity.HIGH)
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }

        val saved = activities.observe(time.today().minusDays(7)..time.today()).first().single()
        assertEquals(Sport.SWIMMING, saved.sport)
        assertEquals(45, saved.durationMinutes)
        assertEquals(1.5, saved.distanceKm!!, 0.0)
        assertEquals(Intensity.HIGH, saved.intensity)
    }

    @Test
    fun freeActivity_needsAName_andDistanceIsOptional() = runBlocking {
        val vm = editor()
        vm.state.awaitUntil { !it.loading }
        vm.setSport(Sport.OTHER)
        vm.setDuration("30")
        assertFalse(vm.save())
        assertTrue(vm.state.value.nameError)
        vm.setName("Remo no lago")
        vm.setDistance("abc")
        assertFalse(vm.save())
        assertTrue(vm.state.value.distanceError)
        vm.setDistance("")
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        val saved = activities.observe(time.today()..time.today()).first().single()
        assertEquals("Remo no lago", saved.name)
        assertNull(saved.distanceKm)
    }

    @Test
    fun editing_loadsValues_futureDatesAreCapped_andDeleteRemoves() = runBlocking {
        val id = activities.save(Activity(sport = Sport.RUNNING, date = time.today(), durationMinutes = 40, distanceKm = 7.25, notes = "parque"))
        val vm = editor(ActivityEditorRoute(activityId = id))
        val loaded = vm.state.awaitUntil { !it.loading }
        assertFalse(loaded.isNew)
        assertEquals("40", loaded.duration)
        assertEquals("7,25", loaded.distance) // pt-BR decimal separator in tests
        vm.setDate(time.today().plusDays(5))
        assertEquals(time.today(), vm.state.value.date)

        vm.setSport(Sport.SOCCER) // once logged, the sport can't change
        assertEquals(Sport.RUNNING, vm.state.value.sport)
        vm.setDistance("7,5")
        assertTrue(vm.save())
        vm.state.awaitUntil { it.done }
        assertEquals(Sport.RUNNING, activities.get(id)!!.sport)
        assertEquals(7.5, activities.get(id)!!.distanceKm!!, 0.0)

        val again = editor(ActivityEditorRoute(activityId = id))
        again.state.awaitUntil { !it.loading }
        again.delete()
        again.state.awaitUntil { it.done }
        assertNull(activities.get(id))
    }
}
