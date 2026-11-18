package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.domain.nutrition.TargetMissing
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** User report: on their phone the calories card showed only the ring (the detail fell under it and was cut). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // real text widths: the legacy mode measures text too narrow to show the bug
@Config(sdk = [36], qualifiers = "pt-rBR-w384dp-h800dp-xxhdpi")
class NutritionTodayCardsNarrowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun caloriesDetail_staysInsideItsCard_onA384dpPhone() {
        val today = LocalDate.of(2026, 11, 17)
        compose.setContent {
            // Same side margins as Home, so the cards have Home's width.
            MyFitTheme(ThemeMode.DARK) {
                Box(Modifier.padding(horizontal = Spacing.screen)) { NutritionTodayCards(
                    NutritionStatus(NutritionDay(today, listOf(Meal(1, today, 600, null, 1880)), 1500, 6), DailyTargets(2300, 2800, emptySet()), NutritionGoal.MAINTAIN),
                    onOpen = {},
                ) }
            }
        }
        val detail = compose.onNodeWithText("1.880 de\n2.300\u00A0k", useUnmergedTree = true)
        detail.assertIsDisplayed()
        val card = compose.onNodeWithTag("nutritionCard_kcal").getBoundsInRoot()
        val text = detail.getBoundsInRoot()
        assertTrue("detail $text outside card $card", text.bottom <= card.bottom - 8.dp && text.right <= card.right)
    }

    @Test
    fun fillInHint_goesUnderTheRing_notSqueezedBesideIt_onA360dpPhone() {
        val today = LocalDate.of(2026, 11, 17)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                Box(Modifier.width(360.dp).padding(horizontal = Spacing.screen)) { // a 360 dp phone
                    NutritionTodayCards(
                        NutritionStatus(
                            NutritionDay(today, emptyList(), 0, 0),
                            DailyTargets(null, null, setOf(TargetMissing.WEIGHT, TargetMissing.HEIGHT, TargetMissing.SEX, TargetMissing.BIRTH_DATE)),
                            NutritionGoal.MAINTAIN,
                        ),
                        onOpen = {},
                    )
                }
            }
        }
        val hint = compose.onNodeWithText("Preencha: peso, altura, sexo, data de nascimento", useUnmergedTree = true).getBoundsInRoot()
        val ring = compose.onAllNodesWithTag("nutritionRing", useUnmergedTree = true)[1].getBoundsInRoot()
        assertTrue("hint $hint should start under the ring $ring", hint.left < ring.right && hint.top >= ring.bottom)
    }

    @Test
    fun caloriesDetail_fitsInTwoLines_onA360dpPhone() {
        val today = LocalDate.of(2026, 11, 17)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                Box(Modifier.width(360.dp).padding(horizontal = Spacing.screen)) { // a 360 dp phone
                    NutritionTodayCards(
                        NutritionStatus(NutritionDay(today, listOf(Meal(1, today, 600, null, 1880)), 1500, 6), DailyTargets(2300, 2800, emptySet()), NutritionGoal.MAINTAIN),
                        onOpen = {},
                    )
                }
            }
        }
        // User report: "kcal" fell to a third line under the number; "k" keeps it on the number's line.
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("1.880 de\n2.300\u00A0k", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(2, results.single().lineCount)
    }
}
