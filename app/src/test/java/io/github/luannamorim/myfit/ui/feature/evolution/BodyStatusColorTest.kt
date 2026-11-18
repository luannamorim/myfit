package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.body.BodyStatus
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BodyStatusColorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cautionDot_isYellow_notTheFoodOrangeOrTheWarningAmber() {
        val seen = mutableMapOf<String, Color>()
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                val c = MyFitTheme.colors
                seen["caution"] = BodyStatus.CAUTION.color()
                seen["token"] = c.caution
                seen["food"] = c.food
                seen["warning"] = c.warning
                seen["good"] = BodyStatus.GOOD.color()
                seen["accent"] = c.accent
            }
        }
        compose.waitForIdle()
        assertEquals(seen["token"], seen["caution"])
        assertNotEquals(seen["food"], seen["caution"])
        assertNotEquals(seen["warning"], seen["caution"])
        assertEquals(seen["accent"], seen["good"])
    }
}
