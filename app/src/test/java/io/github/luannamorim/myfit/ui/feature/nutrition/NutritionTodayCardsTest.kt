package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.ui.feature.home.Greeting
import io.github.luannamorim.myfit.ui.feature.home.HomeActions
import io.github.luannamorim.myfit.ui.feature.home.HomeContent
import io.github.luannamorim.myfit.ui.feature.home.HomeUiState
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h1600dp-xxhdpi")
class NutritionTodayCardsTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 17)

    private fun status(
        waterMl: Int = 1500,
        kcal: Int = 1880,
        targets: DailyTargets = DailyTargets(2300, 2800, emptySet()),
        goal: NutritionGoal = NutritionGoal.MAINTAIN,
    ) = NutritionStatus(NutritionDay(today, listOf(Meal(1, today, 600, "Almoço", kcal)), waterMl, 4), targets, goal)

    private fun show(status: NutritionStatus, onOpen: () -> Unit = {}) =
        compose.setContent { MyFitTheme(ThemeMode.DARK) { NutritionTodayCards(status, onOpen) } }

    @Test
    fun percentOfEachTarget_withWhatWasLogged() {
        show(status())
        compose.onNodeWithText("54%").assertIsDisplayed()
        compose.onNodeWithText("82%").assertIsDisplayed()
        compose.onNodeWithText("1,5 de\n2,8 L", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("1.880 de\n2.300\u00A0k", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun almostThere_isNever100() {
        show(status(kcal = 2290, waterMl = 2790)) // 99.6 % of each
        assertEquals(2, compose.onAllNodesWithText("99%").fetchSemanticsNodes().size)
    }

    @Test
    fun overTheTarget_goesPast100() {
        show(status(kcal = 2484, goal = NutritionGoal.LOSE))
        compose.onNodeWithText("108%").assertIsDisplayed()
    }

    @Test
    fun withoutTargets_showsDash_andWhatToFill() {
        show(status(targets = DailyTargets(null, null, setOf(TargetMissing.WEIGHT, TargetMissing.SEX))))
        assertEquals(2, compose.onAllNodesWithText("—").fetchSemanticsNodes().size)
        compose.onNodeWithText("1,5 L", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Preencha: peso", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("Preencha: peso, sexo", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun tappingACard_opensNutrition() {
        var opened = 0
        show(status(), onOpen = { opened++ })
        compose.onNodeWithText("Água", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Calorias", useUnmergedTree = true).performClick()
        assertEquals(2, opened)
    }

    @Test
    fun home_showsTodaysNutrition_beforeTheBody() {
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                HomeContent(
                    HomeUiState(loading = false, greeting = Greeting.MORNING, today = today, nutrition = status()),
                    HomeActions(),
                )
            }
        }
        val nutrition = compose.onNodeWithText("NUTRIÇÃO HOJE").getBoundsInRoot()
        val body = compose.onNodeWithText("CORPO").getBoundsInRoot()
        assertTrue(nutrition.top < body.top)
        compose.onNodeWithText("54%").assertIsDisplayed()
    }
}
