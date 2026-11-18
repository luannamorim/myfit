package io.github.luannamorim.myfit.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w384dp-h1600dp-xxhdpi")
class HomeTodayDoneTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 17)

    private fun session(id: Long, name: String, at: LocalTime, minutes: Long) =
        SessionSummary(id, name, today, at, minutes * 60_000L, 12, listOf("Agachamento"))

    private fun show(sessions: List<SessionSummary>, activities: List<Activity> = emptyList(), actions: HomeActions = HomeActions()) =
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) { Box(Modifier.background(MyFitTheme.colors.background)) {
                HomeContent(
                    HomeUiState(loading = false, greeting = Greeting.EVENING, today = today, todaySessions = sessions, todayActivities = activities),
                    actions,
                    body = { BodyEmptyCard {} },
                )
            } }
        }

    @Test
    fun oneWorkout_staysAsBefore() {
        show(listOf(session(2, "Pernas", LocalTime.of(18, 30), 53)))
        compose.onNodeWithText("Treino de hoje concluído").assertIsDisplayed()
        compose.onNodeWithText("Pernas · 53 min").assertIsDisplayed()
    }

    @Test
    fun severalThings_showTheDayTotal_andEachOneLatestFirst_eachOpening() {
        val sessions = mutableListOf<Long>()
        val activities = mutableListOf<Long>()
        show(
            listOf(session(2, "Pernas", LocalTime.of(18, 30), 53), session(1, "Costas", LocalTime.of(7, 10), 52)),
            listOf(Activity(id = 5, sport = Sport.SWIMMING, date = today, durationMinutes = 45)),
            HomeActions(onOpenSession = { sessions += it }, onOpenActivity = { activities += it }),
        )
        compose.onNodeWithText("Hoje: 2 treinos · 1 atividade").assertIsDisplayed()
        compose.onNodeWithText("2 h 30 min no total").assertIsDisplayed()
        val legs = compose.onNodeWithText("Pernas · 53 min").getBoundsInRoot()
        val back = compose.onNodeWithText("Costas · 52 min").getBoundsInRoot()
        assertTrue(legs.top < back.top) // latest first
        compose.onNodeWithText("18:30").assertIsDisplayed()
        compose.onNodeWithText("Costas · 52 min").performClick()
        compose.onNodeWithText("Natação · 45 min").performClick()
        assertEquals(listOf(1L), sessions)
        assertEquals(listOf(5L), activities)
    }

    @Test
    fun onlyAnActivity_countsAsDoneToday() {
        show(emptyList(), listOf(Activity(id = 5, sport = Sport.SWIMMING, date = today, durationMinutes = 45)))
        compose.onNodeWithText("Atividade de hoje concluída").assertIsDisplayed()
        compose.onNodeWithText("Natação · 45 min").assertIsDisplayed()
    }

    @Test
    fun several_screenshot() {
        severalThings_showTheDayTotal_andEachOneLatestFirst_eachOpening()
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/home_today_several_pt.png")
    }
}
