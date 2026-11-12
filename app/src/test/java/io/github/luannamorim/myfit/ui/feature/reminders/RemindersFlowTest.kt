package io.github.luannamorim.myfit.ui.feature.reminders

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/** Settings -> Lembretes -> new reminder -> saved and listed, through the real navigation and view models. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w411dp-h891dp-xxhdpi", application = HiltTestApplication::class)
class RemindersFlowTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var reminders: ReminderRepository

    @Before
    fun setUp() {
        hilt.inject()
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun waitFor(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun createOtherActivityReminder_fromSettings() {
        compose.onAllNodesWithText("Ajustes").onFirst().performClick()
        compose.onNodeWithText("Lembretes").performScrollTo().performClick()
        waitFor("Nenhum lembrete")

        compose.onAllNodesWithText("Novo lembrete").onFirst().performClick()
        waitFor("Outra atividade")
        compose.onNodeWithText("Outra atividade").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Corrida")
        // Save is enabled once the routines are loaded.
        compose.waitUntil(5_000) { compose.onAllNodes(hasText("Salvar") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Salvar").performClick()

        // Back on the list: the row's switch is labelled "<what> às <time>".
        compose.waitUntil(5_000) { compose.onAllNodes(hasContentDescription("Corrida às", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Corrida").assertExists()
        val saved = runBlocking { reminders.all() }.single()
        assertEquals("Corrida", saved.label)
        assertEquals(null, saved.routineId)
        assertEquals(true, saved.enabled)
    }
}
