package io.github.luannamorim.myfit.ui.feature.workout

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.testutil.SessionEnv
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class WorkoutFlowTest {
    @get:Rule val compose = createComposeRule()
    private val env = SessionEnv()

    @After fun tearDown() = env.close()

    private fun waitFor(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun fullWorkout_completeAllSets_thenFinish() {
        runBlocking { env.startWorkout(2, 2) }
        val vm = WorkoutViewModel(env.controller, env.time, env.settings)
        var finishedSession: Long? = null
        compose.setContent {
            WorkoutScreen(onMinimize = {}, onFinish = { finishedSession = it }, onSessionGone = {}, viewModel = vm, ticking = false)
        }

        repeat(4) { index ->
            waitFor("Concluir série")
            compose.onNodeWithText("Concluir série").performClick()
            if (index < 3) {
                waitFor("Pular descanso")
                compose.onNodeWithText("Pular descanso").performClick()
            }
        }
        waitFor("Todas as séries concluídas")
        compose.onNodeWithText("Finalizar treino").performClick()
        compose.waitUntil(5_000) { finishedSession != null }

        val session = runBlocking { env.repo.getActive()!! }
        assertEquals(4, session.exercises.flatMap { it.sets }.count { it.status == ItemStatus.DONE })
    }
}
