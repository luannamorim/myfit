package io.github.luannamorim.myfit.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w384dp-h1600dp-xxhdpi")
class HomeNextActivityTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 20)
    private val routines = listOf(RoutineOverview(1, "Costas", null, 6, today.toEpochDay() - 2))

    private fun show(state: HomeUiState, actions: HomeActions = HomeActions()) = compose.setContent {
        MyFitTheme(ThemeMode.DARK) { Box(Modifier.background(MyFitTheme.colors.background)) { HomeContent(state, actions, body = {}) } }
    }

    private fun state(target: ActivityTarget, routines: List<RoutineOverview> = this.routines) = HomeUiState(
        loading = false,
        today = today,
        routines = routines,
        suggestedActivity = target,
        suggestedAt = today.atTime(LocalTime.of(18, 30)),
    )

    @Test
    fun theCardShowsTheSport_itsLastDay_andStartsIt() {
        val started = mutableListOf<Pair<Sport, String?>>()
        show(state(ActivityTarget(Sport.SWIMMING, null, today.toEpochDay() - 1)), HomeActions(onStartActivity = { s, n -> started += s to n }))
        compose.onNodeWithText("Natação").assertIsDisplayed()
        compose.onNodeWithText("Feito ontem").assertIsDisplayed()
        compose.onNodeWithText("Hoje às 18:30").assertIsDisplayed()
        compose.onNodeWithText("Iniciar").performClick()
        assertEquals(listOf(Sport.SWIMMING to null), started)
    }

    @Test
    fun aFreeActivity_showsItsName_andNeverDone() {
        val started = mutableListOf<Pair<Sport, String?>>()
        show(state(ActivityTarget(Sport.OTHER, "Beach vôlei", null)), HomeActions(onStartActivity = { s, n -> started += s to n }))
        compose.onNodeWithText("Beach vôlei").assertIsDisplayed()
        compose.onNodeWithText("Ainda não feito").assertIsDisplayed()
        compose.onNodeWithText("Iniciar").performClick()
        assertEquals(listOf(Sport.OTHER to "Beach vôlei"), started)
    }

    @Test
    fun withoutRoutines_theCardShows_besideTheEmptyState() {
        show(state(ActivityTarget(Sport.RUNNING, null, null), routines = emptyList()))
        compose.onNodeWithText("Corrida").assertIsDisplayed()
        compose.onNodeWithText("Novo treino").performScrollTo().assertIsDisplayed()
    }
}
