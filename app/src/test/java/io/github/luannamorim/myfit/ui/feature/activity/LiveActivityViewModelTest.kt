package io.github.luannamorim.myfit.ui.feature.activity

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.LiveActivityStore
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.platform.activity.ActivityServiceStarter
import io.github.luannamorim.myfit.platform.activity.LiveActivityController
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class LiveActivityViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val activities = ActivityRepository(db, time, BackupTrigger { })
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val controller by lazy {
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

    @Test
    fun finish_pausesAndAsksForTheDoneStep_withoutSavingYet() = runBlocking {
        controller.start(Sport.RUNNING, null, gps = false)
        val vm = LiveActivityViewModel(controller, time)
        vm.state.awaitUntil { it.running }
        vm.finish()
        val state = vm.state.awaitUntil { it.reviewRequested }
        assertTrue(state.running) // still there, paused, until "Save"
        assertTrue(state.paused)
        assertTrue(activities.observe(time.today()..time.today()).first().isEmpty())
        vm.reviewShown()
        assertTrue(!vm.state.awaitUntil { !it.reviewRequested }.reviewRequested)
    }
}
