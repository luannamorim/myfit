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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class HistoryScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 12)
    private fun s(id: Long, day: Int, name: String, min: Long) =
        SessionSummary(id, name, LocalDate.of(2026, 11, day), LocalTime.of(18, 30), min * 60_000, 12, listOf("Remada curvada", "Pullover"))

    @Test
    fun history_month() = compose.snapBothThemes("history_month") {
        val sessions = listOf(s(1, 2, "Treino Costas", 52), s(2, 4, "Peito e Tríceps", 61), s(3, 6, "Pernas", 70), s(4, 9, "Treino Costas", 48), s(5, 10, "Peito e Tríceps", 55), s(6, 12, "Pernas", 66))
        HistoryContent(
            state = HistoryUiState(
                month = YearMonth.of(2026, 11),
                today = today,
                selected = today,
                marked = sessions.map { it.startDate }.toSet(),
                daySessions = sessions.filter { it.startDate == today },
                monthSessions = sessions,
                summary = PeriodSummary(3, 169 * 60_000L, 3.0, listOf(ExerciseFrequency("Remada curvada", 2), ExerciseFrequency("Agachamento livre", 1))),
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
                s(1, 12, "Treino Costas", 52),
                listOf(
                    SessionExerciseDetail("Remada curvada", ItemStatus.DONE, List(3) { SessionSet(it.toLong(), 12, 40.0, 12, 42.5, ItemStatus.DONE) }),
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
