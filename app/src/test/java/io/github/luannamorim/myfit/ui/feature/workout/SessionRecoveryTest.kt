package io.github.luannamorim.myfit.ui.feature.workout

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.domain.session.Rest
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/** An active session in the database (e.g. after the app was killed) is offered for resume. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w411dp-h891dp-xxhdpi", application = HiltTestApplication::class)
class SessionRecoveryTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var db: MyFitDatabase
    @Inject lateinit var sessions: SessionRepository
    @Inject lateinit var time: TimeSource

    @Before
    fun setUp() {
        hilt.inject()
        runBlocking {
            val exercises = db.exerciseDao().getAll().take(2).map { it.id }
            val routine = db.insertRoutine("Costas", exercises)
            sessions.start(routine, time.now(), time.zone())
            // One set done and a rest that already expired while the app was dead.
            val now = time.now()
            val done = SessionEngine.completeSet(sessions.getActive()!!, 12, 40.0, now)
            sessions.save(done.copy(rest = Rest(now.copy(wallMs = now.wallMs - 600_000, elapsedMs = now.elapsedMs - 600_000), 90_000)))
        }
    }

    private fun waitFor(text: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun activeSession_showsResumeBar_andOpensWorkoutAtSameSet() {
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("Treino em andamento")
            compose.onNodeWithText("Retomar").performClick()
            waitFor("SÉRIE 2 DE 3")
            waitFor("Concluir série")
            compose.onNodeWithText("Pular descanso").assertDoesNotExist()
        }
    }
}
