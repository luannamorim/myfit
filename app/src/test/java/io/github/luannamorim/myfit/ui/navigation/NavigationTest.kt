package io.github.luannamorim.myfit.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
class NavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun heading(text: String) = compose.onNode(hasText(text) and isHeading())

    @Test
    fun eachTab_showsItsScreen_andBackReturnsHome() {
        compose.setContent { MyFitTheme { MyFitNavHost() } }
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
        compose.setContent { MyFitTheme { MyFitNavHost() } }
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/nav_expanded_dark.png")
    }
}
