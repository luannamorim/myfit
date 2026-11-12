package io.github.luannamorim.myfit.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE, application = HiltTestApplication::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun heading(text: String) = compose.onNode(hasText(text) and isHeading())

    @Test
    fun eachTab_showsItsScreen_andBackReturnsHome() {
        heading("Início").assertIsDisplayed()

        listOf("Treinos", "Histórico", "Evolução", "Ajustes").forEach { tab ->
            compose.onAllNodesWithText(tab).onFirst().performClick()
            heading(tab).assertIsDisplayed()
        }

        compose.onAllNodesWithText("Treinos").onFirst().performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        heading("Início").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w840dp-h1200dp-xhdpi")
    fun expandedWidth_usesNavigationRail_screenshot() {
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/nav_expanded_dark.png")
    }
}
