package io.github.luannamorim.myfit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorContrastTest {

    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private fun assertContrast(name: String, a: Color, b: Color, min: Double) {
        val c = contrast(a, b)
        assertTrue("$name contrast $c < $min", c >= min)
    }

    private fun check(colors: MyFitColors) {
        val p = if (colors.isDark) "dark" else "light"
        assertContrast("$p textPrimary/background", colors.textPrimary, colors.background, 7.0)
        assertContrast("$p textPrimary/surfaceRaised", colors.textPrimary, colors.surfaceRaised, 7.0)
        assertContrast("$p textSecondary/background", colors.textSecondary, colors.background, 4.5)
        assertContrast("$p textSecondary/surfaceRaised", colors.textSecondary, colors.surfaceRaised, 4.5)
        assertContrast("$p onAccent/accent", colors.onAccent, colors.accent, 4.5)
        assertContrast("$p accent/background", colors.accent, colors.background, 3.0)
        assertContrast("$p accent/surface", colors.accent, colors.surface, 3.0)
        assertContrast("$p danger/background", colors.danger, colors.background, 3.0)
    }

    @Test
    fun darkPalette_meetsContrast() = check(DarkColors)

    @Test
    fun lightPalette_meetsContrast() = check(LightColors)
}
