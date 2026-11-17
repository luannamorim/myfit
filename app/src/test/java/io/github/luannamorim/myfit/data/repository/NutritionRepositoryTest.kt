package io.github.luannamorim.myfit.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class NutritionRepositoryTest {
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private var backupCalls = 0
    private val repo = NutritionRepository(db, time) { backupCalls++ }
    private val day = LocalDate.of(2026, 11, 17)

    @After fun tearDown() = db.close()

    @Test
    fun water_sumsPerDay_andUndoRemovesTheLastOfThatDay() = runTest {
        repo.addWater(day, 250)
        time.advance(1000)
        repo.addWater(day, 500)
        repo.addWater(day.minusDays(1), 300)

        val today = repo.observeDay(day).first()
        assertEquals(750, today.waterMl)
        assertEquals(2, today.waterEntries)

        repo.undoLastWater(day)
        assertEquals(250, repo.observeDay(day).first().waterMl)
        assertEquals(300, repo.observeDay(day.minusDays(1)).first().waterMl) // other day untouched

        repo.undoLastWater(day)
        repo.undoLastWater(day) // nothing left: no-op
        assertEquals(0, repo.observeDay(day).first().waterEntries)
        assertEquals(5, backupCalls) // 3 adds + 2 real undos
    }

    @Test
    fun water_outOfRange_isIgnored() = runTest {
        repo.addWater(day, 0)
        repo.addWater(day, 5001)
        assertEquals(0, repo.observeDay(day).first().waterEntries)
        assertEquals(0, backupCalls)
    }

    @Test
    fun meals_sortedByTime_andTrimmed() = runTest {
        repo.saveMeal(Meal(date = day, minuteOfDay = 750, description = "  Arroz, feijão, frango ", kcal = 920))
        repo.saveMeal(Meal(date = day, minuteOfDay = 490, description = "   ", kcal = 320))
        repo.saveMeal(Meal(date = day.plusDays(1), minuteOfDay = 600, description = "Outro dia", kcal = 100))

        val d = repo.observeDay(day).first()
        assertEquals(listOf(490, 750), d.meals.map { it.minuteOfDay })
        assertNull(d.meals[0].description) // blank → null
        assertEquals("Arroz, feijão, frango", d.meals[1].description)
        assertEquals(1240, d.kcal)
    }

    @Test
    fun saveMeal_updatesExisting_andDeleteRemoves() = runTest {
        val id = repo.saveMeal(Meal(date = day, minuteOfDay = 480, description = "Pão", kcal = 200))
        repo.saveMeal(repo.getMeal(id)!!.copy(kcal = 250, minuteOfDay = 500))
        val meal = repo.observeDay(day).first().meals.single()
        assertEquals(id, meal.id)
        assertEquals(250, meal.kcal)
        assertEquals(500, meal.minuteOfDay)

        repo.deleteMeal(id)
        assertEquals(0, repo.observeDay(day).first().meals.size)
        assertNull(repo.getMeal(id))
        assertEquals(3, backupCalls)
    }

    @Test
    fun saveMeal_clampsKcalAndTime() = runTest {
        val id = repo.saveMeal(Meal(date = day, minuteOfDay = 2000, description = null, kcal = 20_000))
        val meal = repo.getMeal(id)!!
        assertEquals(Meal.MAX_KCAL, meal.kcal)
        assertEquals(1439, meal.minuteOfDay)
    }
}
