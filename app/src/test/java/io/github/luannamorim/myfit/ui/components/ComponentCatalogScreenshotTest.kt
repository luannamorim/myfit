package io.github.luannamorim.myfit.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class ComponentCatalogScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun brand() = compose.snapBothThemes("brand") { BrandCatalog() }

    @Test
    fun components() = compose.snapBothThemes("components") { ComponentCatalog() }
}

@Composable
private fun BrandCatalog() {
    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        MyFitWordmark(fontSize = 40.sp())
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(96, 48, 32, 24, 16).forEach { MyFitMark(size = it.dp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Launcher icon preview: adaptive background + foreground, circular and squircle masks.
            listOf(RoundedCornerShape(50), RoundedCornerShape(28)).forEach { shape ->
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(108.dp).clip(shape).background(Color(0xFF0B0B0C)),
                )
            }
            Image(
                painter = painterResource(R.drawable.ic_launcher_monochrome),
                contentDescription = null,
                modifier = Modifier.size(108.dp).clip(RoundedCornerShape(50)).background(Color(0xFF3A4A3A)),
            )
        }
    }
}

@Composable
private fun ComponentCatalog() {
    val c = MyFitTheme.colors
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("Treinos", action = { TextAction("Ver todos", onClick = {}) })
        MyFitCard(onClick = {}) {
            Text("Treino Costas", style = MyFitTheme.type.title, color = c.textPrimary)
            Text("6 exercícios · feito há 3 dias", style = MyFitTheme.type.caption, color = c.textSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Peso", "73,2", Modifier.weight(1f), unit = "kg", delta = "+2,2 kg", deltaPositive = true)
            StatTile("Cintura", "79", Modifier.weight(1f), unit = "cm", delta = "+1 cm", deltaPositive = false)
        }
        PrimaryButton("Iniciar treino", onClick = {}, icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton("Pular série", onClick = {})
            PrimaryButton("Desativado", onClick = {}, enabled = false)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberStepper(value = "12", label = "Repetições", onDecrement = {}, onIncrement = {}, modifier = Modifier.weight(1f))
            NumberStepper(value = "42,5", label = "Carga", unit = "kg", onDecrement = {}, onIncrement = {}, modifier = Modifier.weight(1f))
        }
        FilterChipRow(
            options = listOf("Peito", "Costas", "Ombros", "Bíceps", "Pernas"),
            selected = setOf("Costas"),
            label = { it },
            onToggle = {},
        )
        RestRing(progress = 0.62f, remainingText = "0:56", caption = "Descanso", modifier = Modifier.padding(start = 60.dp))
        EmptyState("Nenhum treino ainda", "Crie seu primeiro treino para começar.", action = { PrimaryButton("Novo treino", onClick = {}) })
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
