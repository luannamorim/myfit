package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.history.ExerciseFrequency
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.history.PeriodSummary
import io.github.luannamorim.myfit.domain.history.SessionDetail
import io.github.luannamorim.myfit.domain.history.SessionExerciseDetail
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.session.SessionSet
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.history.SportFrequency
import io.github.luannamorim.myfit.domain.model.Intensity
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class HistoryScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 12)
    private fun s(id: Long, day: Int, name: String, min: Long, intensity: Intensity? = null) =
        SessionSummary(id, name, LocalDate.of(2026, 11, day), LocalTime.of(18, 30), min * 60_000, 12, listOf("Bent-over row", "Pullover"), intensity)

    @Test
    fun history_month() = compose.snapBothThemes("history_month") {
        val sessions = listOf(s(1, 2, "Back day", 52), s(2, 4, "Chest & triceps", 61), s(3, 6, "Legs", 70), s(4, 9, "Back day", 48), s(5, 10, "Chest & triceps", 55), s(6, 12, "Legs", 66, Intensity.HIGH))
        val activities = listOf(
            Activity(1, Sport.SWIMMING, date = today, durationMinutes = 45, distanceKm = 1.5, intensity = Intensity.MEDIUM),
            Activity(2, Sport.SOCCER, date = LocalDate.of(2026, 11, 7), durationMinutes = 90, intensity = Intensity.HIGH),
        )
        HistoryContent(
            state = HistoryUiState(
                month = YearMonth.of(2026, 11),
                today = today,
                selected = today,
                marked = sessions.map { it.startDate }.toSet() + activities.map { it.date },
                daySessions = sessions.filter { it.startDate == today },
                monthSessions = sessions,
                dayActivities = activities.filter { it.date == today },
                monthActivities = activities,
                summary = PeriodSummary(
                    3, 304 * 60_000L, 5.0, listOf(ExerciseFrequency("Bent-over row", 2), ExerciseFrequency("Barbell back squat", 1)),
                    activities = 2, sports = listOf(SportFrequency(Sport.SOCCER, null, 1), SportFrequency(Sport.SWIMMING, null, 1)),
                ),
                summaryRange = HistoryCalculator.weekOf(today),
                isLoading = false,
            ),
            onSelect = {}, onMonthChange = {}, onPeriod = {}, onOpenSession = {},
        )
    }

    @Test
    fun session_detail() = compose.snapBothThemes("session_detail") {
        SessionDetailContent(
            SessionDetail(
                s(1, 12, "Back day", 52, Intensity.HIGH),
                listOf(
                    SessionExerciseDetail("Bent-over row", ItemStatus.DONE, List(3) { SessionSet(it.toLong(), 12, 40.0, 12, 42.5, ItemStatus.DONE) }),
                    SessionExerciseDetail(
                        "Pullover", ItemStatus.DONE,
                        listOf(SessionSet(5, 12, 20.0, 12, 20.0, ItemStatus.DONE), SessionSet(6, 12, 20.0, 10, 20.0, ItemStatus.DONE), SessionSet(7, 12, 20.0, status = ItemStatus.SKIPPED)),
                    ),
                ),
            ),
            {}, {},
        )
    }
}
