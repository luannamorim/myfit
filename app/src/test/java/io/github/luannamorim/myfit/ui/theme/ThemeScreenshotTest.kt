package io.github.luannamorim.myfit.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class ThemeScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun themeCatalog() = compose.snapBothThemes("theme_catalog") { ThemeCatalog() }
}

@Composable
private fun ThemeCatalog() {
    val c = MyFitTheme.colors
    val t = MyFitTheme.type
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            "background" to c.background, "surface" to c.surface, "surfaceRaised" to c.surfaceRaised,
            "outline" to c.outline, "textPrimary" to c.textPrimary, "textSecondary" to c.textSecondary,
            "textTertiary" to c.textTertiary, "accent" to c.accent, "accentMuted" to c.accentMuted,
            "danger" to c.danger, "warning" to c.warning,
        ).forEach { (name, color) -> Swatch(name, color) }
        Text("Display 32", style = t.display, color = c.textPrimary)
        Text("Headline 24", style = t.headline, color = c.textPrimary)
        Text("Title — Treino Costas", style = t.title, color = c.textPrimary)
        Text("Body — Remada curvada, 3 séries de 12 repetições.", style = t.body, color = c.textPrimary)
        Text("Secondary — última vez há 3 dias", style = t.body, color = c.textSecondary)
        Text("LABEL / OVERLINE", style = t.overline, color = c.textSecondary)
        Text("01:30", style = t.numericHero, color = c.textPrimary)
        Text("42,5 kg", style = t.numericLarge, color = c.textPrimary)
        Text("1:02:45 · 18 séries", style = t.numeric, color = c.accent)
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    val c = MyFitTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(28.dp).background(color, RoundedCornerShape(8.dp)),
        )
        Text(name, style = MyFitTheme.type.body, color = c.textPrimary)
    }
}
