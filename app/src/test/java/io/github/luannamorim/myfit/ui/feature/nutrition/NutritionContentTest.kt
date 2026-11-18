package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h1600dp-xxhdpi")
class NutritionContentTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 17)

    private val meals = listOf(
        Meal(1, today, 8 * 60 + 10, "Pão com ovo e café", 320),
        Meal(2, today, 12 * 60 + 30, null, 920),
    )

    private fun state(
        date: LocalDate = today,
        meals: List<Meal> = this.meals,
        waterMl: Int = 1500,
        waterEntries: Int = 4,
        targets: DailyTargets = DailyTargets(2300, 2800, emptySet()),
        goal: NutritionGoal = NutritionGoal.MAINTAIN,
    ) = NutritionUiState(loading = false, date = date, today = today, day = NutritionDay(date, meals, waterMl, waterEntries), targets = targets, goal = goal)

    private fun show(state: NutritionUiState, actions: NutritionActions = NutritionActions()) =
        compose.setContent { MyFitTheme(ThemeMode.DARK) { NutritionContent(state, actions) } }

    @Test
    fun totalsAgainstTargets_andWhatIsLeft() {
        show(state())
        compose.onNodeWithText("Hoje").assertIsDisplayed()
        compose.onNodeWithText("1,5 / 2,8 L").assertIsDisplayed()
        compose.onNodeWithText("1.240 / 2.300 kcal").assertIsDisplayed()
        compose.onNodeWithText("Manter peso · faltam 1.060 kcal").assertIsDisplayed()
        compose.onNodeWithText("Pão com ovo e café").assertIsDisplayed()
        compose.onNodeWithText("Refeição").assertIsDisplayed() // no description
        compose.onNodeWithText("920 kcal").assertIsDisplayed()
    }

    @Test
    fun overTheTarget_saysHowMuch() {
        show(state(meals = listOf(Meal(1, today, 600, "Pizza", 2500)), goal = NutritionGoal.LOSE))
        compose.onNodeWithText("Perder gordura · 200 kcal acima da meta").assertIsDisplayed()
    }

    @Test
    fun waterButtons_addTheAmount_andUndoOnlyWithEntries() {
        val added = mutableListOf<Int>()
        var undone = 0
        show(state(), NutritionActions(onAddWater = { added += it }, onUndoWater = { undone++ }))
        compose.onNodeWithText("+250 ml").performClick()
        compose.onNodeWithText("+500 ml").performClick()
        compose.onNodeWithText("Outro").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("330")
        compose.onNodeWithText("Adicionar").performClick()
        assertEquals(listOf(250, 500, 330), added)
        compose.onNodeWithContentDescription("Desfazer").performClick()
        assertEquals(1, undone)
    }

    @Test
    fun undo_sitsInTheCardsCorner() {
        show(state())
        val undo = compose.onNodeWithContentDescription("Desfazer").getBoundsInRoot()
        val add = compose.onNodeWithText("+250 ml").getBoundsInRoot()
        val total = compose.onNodeWithText("1,5 / 2,8 L").getBoundsInRoot()
        assertTrue(undo.right >= total.right - 4.dp) // right edge, like the total above it
        assertTrue(undo.top < add.bottom && undo.bottom > add.top) // same row as the buttons
    }

    @Test
    fun waterButtons_useTheSavedAmounts_andThePencilEditsThem() {
        val saved = mutableListOf<List<Int>>()
        val added = mutableListOf<Int>()
        show(state().copy(waterButtons = listOf(300, 600)), NutritionActions(onAddWater = { added += it }, onSetWaterButtons = { saved += it }))
        compose.onNodeWithText("+300 ml").performClick()
        compose.onNodeWithText("+600 ml").performClick()
        assertEquals(listOf(300, 600), added)

        compose.onNodeWithContentDescription("Editar botões de água").performClick()
        compose.onNodeWithText("Botão 1").performTextClearance()
        compose.onNodeWithText("Botão 1").performTextInput("200")
        compose.onNodeWithText("Salvar").performClick()
        assertEquals(listOf(listOf(200, 600)), saved)
    }

    @Test
    fun noWaterYet_hidesUndo() {
        show(state(waterMl = 0, waterEntries = 0))
        compose.onNodeWithContentDescription("Desfazer").assertDoesNotExist()
    }

    @Test
    fun missingTargets_sayWhatToFill_andOpenWhereItIs() {
        val opened = mutableListOf<TargetMissing>()
        show(
            state(targets = DailyTargets(null, null, setOf(TargetMissing.WEIGHT, TargetMissing.BIRTH_DATE))),
            NutritionActions(onFillMissing = { opened += it }),
        )
        compose.onNodeWithText("Preencha: peso, data de nascimento").assertIsDisplayed()
        compose.onNodeWithText("1,5 L").assertIsDisplayed() // water total without a target
        compose.onNodeWithText("Preencha: peso").assertIsDisplayed() // water only needs the weight
        compose.onNodeWithText("Preencha: peso, data de nascimento").performClick()
        assertEquals(listOf(TargetMissing.WEIGHT), opened) // body first, then the profile
    }

    @Test
    fun dayArrows_goBack_andNeverPastToday() {
        var back = 0
        show(state(), NutritionActions(onPreviousDay = { back++ }))
        compose.onNodeWithContentDescription("Dia anterior").performClick()
        assertEquals(1, back)
        compose.onNodeWithContentDescription("Próximo dia").assertIsNotEnabled()
    }

    @Test
    fun pastDay_isNamed_andAddMealOpensTheEditor() {
        var adds = 0
        val opened = mutableListOf<Long>()
        show(state(date = today.minusDays(1)), NutritionActions(onAddMeal = { adds++ }, onOpenMeal = { opened += it }))
        compose.onNodeWithText("Ontem").assertIsDisplayed()
        compose.onNodeWithText("Pão com ovo e café").performClick()
        compose.onNodeWithText("Adicionar refeição").performScrollTo().performClick()
        assertEquals(listOf(1L), opened)
        assertEquals(1, adds)
    }
}
