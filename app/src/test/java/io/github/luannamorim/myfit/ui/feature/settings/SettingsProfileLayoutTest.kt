package io.github.luannamorim.myfit.ui.feature.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
        val chips = listOf("Não informado", "Masculino", "Feminino", "Escuro", "Claro", "Sistema", "Segue o modo do celular", "Toca sempre")
        val wrapped = chips.filter { label ->
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
            results.single().lineCount > 1
        }
        assertEquals(emptyList<String>(), wrapped)
    }
}
