package io.github.luannamorim.myfit.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.activity.SportLastDay
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ActivityRepositoryTest {
    private val db = inMemoryDb()
    private val repo = ActivityRepository(db, FakeTimeSource(), BackupTrigger { })

    @After fun tearDown() = db.close()

    private suspend fun log(sport: Sport, day: Long, name: String? = null) =
        repo.save(Activity(sport = sport, name = name, date = LocalDate.ofEpochDay(day), durationMinutes = 30))

    @Test
    fun observeLastDays_keepsTheLatestDayPerSportAndName() = runBlocking {
        log(Sport.SWIMMING, 10)
        log(Sport.SWIMMING, 12)
        log(Sport.RUNNING, 11)
        log(Sport.OTHER, 9, "Escalada")
        log(Sport.OTHER, 13, "Escalada")

        assertEquals(
            setOf(
                SportLastDay(Sport.SWIMMING, null, 12),
                SportLastDay(Sport.RUNNING, null, 11),
                SportLastDay(Sport.OTHER, "Escalada", 13),
            ),
            repo.observeLastDays().first().toSet(),
        )
    }
}
