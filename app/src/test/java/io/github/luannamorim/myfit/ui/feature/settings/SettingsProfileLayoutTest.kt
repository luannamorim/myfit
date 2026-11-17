package io.github.luannamorim.myfit.ui.feature.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Portuguese labels on a small phone (360 dp, common on Samsung): no Settings chip may break its text. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h800dp-xxhdpi")
class SettingsProfileLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun chips_neverBreakTheirText_inPortugueseOnASmallPhone() {
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                SettingsContent(SettingsUiState(system = SystemStatus(notificationsEnabled = true, batteryUnrestricted = true)), {}, {}, {}, {}, {})
            }
        }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/settings_profile_pt_360.png")
        val chips = listOf("Não informado", "Masculino", "Feminino", "Perder gordura", "Manter peso", "Ganhar massa", "Escuro", "Claro", "Sistema", "Segue o modo do celular", "Toca sempre")
        val wrapped = chips.filter { label ->
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single().lineCount > 1
        }
        assertEquals(emptyList<String>(), wrapped)
    }

    private val system = SystemStatus(notificationsEnabled = true, batteryUnrestricted = true)

    /** Renders the screen and applies every update to [settings], like the view model does. */
    private fun render(initial: AppSettings): () -> AppSettings {
        var settings by mutableStateOf(initial)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                SettingsContent(SettingsUiState(settings = settings, system = system), { t -> settings = t(settings) }, {}, {}, {}, {})
            }
        }
        return { settings }
    }

    @Test
    fun goalChip_setsTheNutritionGoal() {
        val current = render(AppSettings())
        compose.onNodeWithText("Manter peso").assertIsSelected()
        compose.onNodeWithText("Ganhar massa").performClick()
        assertEquals(NutritionGoal.GAIN, current().nutritionGoal)
        compose.onNodeWithText("Ganhar massa").assertIsSelected()
    }

    @Test
    fun birthDate_showsNotSet_thenTheDate_andCanBeCleared() {
        val current = render(AppSettings(birthEpochDay = LocalDate.of(1996, 5, 20).toEpochDay()))
        compose.onNodeWithText("Data de nascimento").assertIsDisplayed()
        compose.onNodeWithText("20 de mai. de 1996", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Limpar").performClick()
        assertNull(current().birthEpochDay)
        compose.onNodeWithText("Não informada").assertIsDisplayed()
    }
}
