package io.github.luannamorim.myfit.ui.feature.nutrition

import android.content.Intent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h800dp-xxhdpi")
class MealEditorContentTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 11, 17)

    private fun show(state: MealEditorState, onAskAi: (String) -> Unit = {}) = compose.setContent {
        MyFitTheme(ThemeMode.DARK) { MealEditorContent(state, MealEditorActions(onAskAi = onAskAi)) }
    }

    @Test
    fun askAi_sendsAPromptWithTheDescription() {
        val prompts = mutableListOf<String>()
        show(MealEditorState(loading = false, date = day, description = "Arroz, feijão e frango", minuteOfDay = 750), onAskAi = { prompts += it })
        compose.onNodeWithText("Perguntar à IA").performClick()
        val prompt = prompts.single()
        assertTrue(prompt, prompt.contains("\"Arroz, feijão e frango\""))
        assertTrue(prompt, prompt.contains("kcal"))
    }

    @Test
    fun askAi_isDisabled_withoutADescription() {
        show(MealEditorState(loading = false, date = day, description = "  ", minuteOfDay = 750))
        compose.onNodeWithText("Perguntar à IA").assertIsNotEnabled()
        compose.onNodeWithText("Salvar").assertIsNotEnabled() // no kcal yet
    }

    @Test
    fun askAiIntent_isAChooserOfPlainTextShares() {
        val chooser = askAiIntent("Quantas calorias?", "Perguntar à IA")
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals("Quantas calorias?", send.getStringExtra(Intent.EXTRA_TEXT))
    }
}
