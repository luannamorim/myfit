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
@Config(sdk = [36], qualifiers = "pt-rBR-w411dp-h891dp-xxhdpi", application = HiltTestApplication::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun heading(text: String) = compose.onNode(hasText(text) and isHeading())

    /** Home's title is a time-of-day greeting. */
    private fun homeHeading() = compose.onNode(isHeading() and (hasText("Bom dia") or hasText("Boa tarde") or hasText("Boa noite")))

    @Test
    fun eachTab_showsItsScreen_andBackReturnsHome() {
        homeHeading().assertIsDisplayed()

        listOf("Treinos", "Histórico", "Evolução", "Ajustes").forEach { tab ->
            compose.onAllNodesWithText(tab).onFirst().performClick()
            heading(tab).assertIsDisplayed()
        }

        compose.onAllNodesWithText("Treinos").onFirst().performClick()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        homeHeading().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w840dp-h1200dp-xhdpi")
    fun expandedWidth_usesNavigationRail_inEnglish_screenshot() {
        compose.onNodeWithText("Settings").assertIsDisplayed()
        compose.onNode(isHeading() and (hasText("Good morning") or hasText("Good afternoon") or hasText("Good evening"))).assertIsDisplayed()
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/nav_expanded_dark.png")
    }
}
