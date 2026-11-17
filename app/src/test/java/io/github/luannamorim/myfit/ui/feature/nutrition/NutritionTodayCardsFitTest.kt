package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import io.github.luannamorim.myfit.ui.theme.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Small phone, Portuguese, big system font (the user's phone has one): the detail drops under the ring, "108%" fits inside. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h400dp-xxhdpi", fontScale = 1.3f)
class NutritionTodayCardsFitTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun smallPhone_bigFont() {
        val today = LocalDate.of(2026, 11, 17)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                Box(Modifier.background(MyFitTheme.colors.background).padding(Spacing.screen)) {
                    NutritionTodayCards(
                        NutritionStatus(NutritionDay(today, listOf(Meal(1, today, 600, null, 2484)), 1500, 6), DailyTargets(2300, 2800, emptySet()), NutritionGoal.LOSE),
                        onOpen = {},
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/home_nutrition_pt_360_big_font.png")
    }
}
