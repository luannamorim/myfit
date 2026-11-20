package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.data.repository.NutritionTracker
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NutritionViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val env = SessionEnv()
    private val today = env.time.today()
    private val nutrition = NutritionRepository(env.db, env.time, BackupTrigger { })
    private val body = BodyRepository(env.db.bodyDao(), env.time, BackupTrigger { })
    private val activities = ActivityRepository(env.db, env.time, BackupTrigger { })

    @After fun tearDown() = env.close()

    private fun viewModel(settings: SettingsRepository, handle: SavedStateHandle = SavedStateHandle()) = NutritionViewModel(
        handle,
        nutrition,
        NutritionTracker(nutrition, body, HistoryRepository(env.db, env.time, BackupTrigger { }), activities, settings),
        settings,
        env.time,
    )

    /** 80 kg, 178 cm, male, 30 years old on [today]. */
    private suspend fun profile(goal: NutritionGoal = NutritionGoal.MAINTAIN): SettingsRepository {
        body.save(BodyAssessment(date = today.minusDays(3), values = mapOf(Measure.WEIGHT to 80.0, Measure.HEIGHT to 178.0)))
        return testSettings().apply {
            update { it.copy(sex = Sex.MALE, birthEpochDay = today.minusYears(30).toEpochDay(), nutritionGoal = goal) }
        }
    }

    @Test
    fun state_showsTodaysLog_andTargetsFromBodyAndProfile() = runBlocking {
        val settings = profile(NutritionGoal.GAIN)
        nutrition.saveMeal(Meal(date = today, minuteOfDay = 480, description = "Pão com ovo", kcal = 320))
        nutrition.addWater(today, 500)

        val state = viewModel(settings).state.awaitUntil { !it.loading && it.targets.kcal != null && it.day.kcal > 0 }
        assertEquals(today, state.date)
        assertEquals(2420, state.targets.kcal) // 1767.5 × 1.2 + 300
        assertEquals(2800, state.targets.waterMl)
        assertEquals(NutritionGoal.GAIN, state.goal)
        assertEquals(320, state.day.kcal)
        assertEquals(500, state.day.waterMl)
        assertFalse(state.canGoForward)
    }

    @Test
    fun activitiesBeforeTheDay_raiseTheTarget() = runBlocking {
        val settings = profile()
        (1L..12L).forEach { activities.save(Activity(sport = Sport.SWIMMING, date = today.minusDays(it), durationMinutes = 30)) }
        val state = viewModel(settings).state.awaitUntil { !it.loading && it.targets.kcal != null }
        assertEquals(2740, state.targets.kcal) // 3 days/week → 1.55
    }

    @Test
    fun stretchingBeforeTheDay_doesNotRaiseTheTarget() = runBlocking {
        val settings = profile()
        (1L..12L).forEach { activities.save(Activity(sport = Sport.STRETCHING, date = today.minusDays(it), durationMinutes = 30)) }
        val state = viewModel(settings).state.awaitUntil { !it.loading && it.targets.kcal != null }
        assertEquals(2120, state.targets.kcal) // still sedentary → 1.2
    }

    @Test
    fun missingProfile_isReported() = runBlocking {
        val state = viewModel(testSettings()).state.awaitUntil { !it.loading }
        assertNull(state.targets.kcal)
        assertNull(state.targets.waterMl)
        assertEquals(TargetMissing.entries.toSet(), state.targets.missing)
    }

    @Test
    fun previousDay_showsThatDay_andWaterGoesThere_neverPastToday() = runBlocking {
        val vm = viewModel(profile())
        vm.state.awaitUntil { !it.loading }
        vm.nextDay() // already today: stays
        assertEquals(today, vm.state.awaitUntil { !it.loading }.date)

        vm.previousDay()
        val yesterday = vm.state.awaitUntil { !it.loading && it.date == today.minusDays(1) }
        assertTrue(yesterday.canGoForward)
        vm.addWater(250)
        vm.addWater(250)
        assertEquals(500, vm.state.awaitUntil { it.day.waterMl == 500 }.day.waterMl)
        vm.undoWater()
        assertEquals(1, vm.state.awaitUntil { it.day.waterEntries == 1 }.day.waterEntries)

        vm.nextDay()
        val back = vm.state.awaitUntil { it.date == today && !it.loading }
        assertEquals(0, back.day.waterMl) // yesterday's water stayed on yesterday
    }

    private val dayMs = 24 * 60 * 60 * 1000L

    @Test
    fun showingToday_followsTheDateAfterMidnight_onResume() = runBlocking {
        val vm = viewModel(profile())
        vm.state.awaitUntil { !it.loading }
        env.time.advance(dayMs) // the app stayed in the background overnight
        vm.refreshToday()
        val next = vm.state.awaitUntil { it.date == today.plusDays(1) && !it.loading }
        assertEquals(today.plusDays(1), next.today)
        assertFalse(next.canGoForward)
        vm.addWater(250)
        assertEquals(250, nutrition.observeDay(today.plusDays(1)).first().waterMl)
        assertEquals(0, nutrition.observeDay(today).first().waterMl)
    }

    @Test
    fun waterAddedAfterMidnight_withoutResume_goesToTheNewDay() = runBlocking {
        val vm = viewModel(profile())
        vm.state.awaitUntil { !it.loading }
        env.time.advance(dayMs) // still on screen when midnight passes
        vm.addWater(250)
        // The write runs in the VM's scope: wait for it (CI read 0 once) — it must land on the new day, not today.
        nutrition.observeDay(today.plusDays(1)).awaitUntil { it.waterMl == 250 }
        assertEquals(0, nutrition.observeDay(today).first().waterMl)
        assertEquals(today.plusDays(1), vm.state.awaitUntil { it.day.waterMl == 250 }.date)
    }

    @Test
    fun aPastDayStaysShown_whenTheDateChanges() = runBlocking {
        val vm = viewModel(profile())
        vm.state.awaitUntil { !it.loading }
        vm.previousDay()
        env.time.advance(dayMs)
        vm.refreshToday()
        val s = vm.state.awaitUntil { it.today == today.plusDays(1) && !it.loading }
        assertEquals(today.minusDays(1), s.date)
        assertTrue(s.canGoForward)
    }

    @Test
    fun waterButtons_comeFromSettings_andCanBeChanged() = runBlocking {
        val settings = profile()
        val vm = viewModel(settings)
        assertEquals(listOf(250, 500), vm.state.awaitUntil { !it.loading }.waterButtons)
        vm.setWater(null, listOf(300, 600))
        assertEquals(listOf(300, 600), vm.state.awaitUntil { it.waterButtons == listOf(300, 600) }.waterButtons)
        assertEquals(listOf(300, 600), settings.current().waterButtonsMl)
    }

    @Test
    fun customTargets_areUsed_andRecommendedStaysVisible() = runBlocking {
        val settings = profile()
        val vm = viewModel(settings)
        vm.state.awaitUntil { !it.loading && it.targets.kcal != null }
        vm.setKcalTarget(2500)
        vm.setWater(3200, listOf(300, 600))
        val s = vm.state.awaitUntil { it.targets.kcal == 2500 && it.targets.waterMl == 3200 }
        assertEquals(2500, s.customKcal)
        assertEquals(3200, s.customWaterMl)
        assertEquals(2120, s.targets.recommendedKcal)
        assertEquals(2800, s.targets.recommendedWaterMl)
        assertEquals(listOf(300, 600), s.waterButtons)
        vm.setKcalTarget(null)
        assertEquals(2120, vm.state.awaitUntil { it.targets.kcal == 2120 }.targets.kcal)
    }

    @Test
    fun shownDay_survivesTheProcessBeingKilled() = runBlocking {
        val settings = profile()
        val handle = SavedStateHandle()
        val vm = viewModel(settings, handle)
        vm.state.awaitUntil { !it.loading }
        vm.previousDay()
        vm.state.awaitUntil { it.date == today.minusDays(1) }

        // Android recreates the tab with its saved state (e.g. after the AI app).
        val restored = viewModel(settings, handle).state.awaitUntil { !it.loading }
        assertEquals(today.minusDays(1), restored.date)
    }
}
