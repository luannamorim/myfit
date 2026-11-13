package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.body.BodyPhoto
import io.github.luannamorim.myfit.domain.model.PhotoPose
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Photos behave like a gallery: tap opens the viewer, press and hold selects (pt-BR labels). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Tall screen so the photo grid is on screen; keeps the pt-BR locale of robolectric.properties.
@Config(sdk = [36], qualifiers = "+w411dp-h1600dp")
class PhotoGalleryTest {
    @get:Rule val compose = createComposeRule()

    private val day = LocalDate.of(2026, 11, 12)
    private val photos = listOf(
        BodyPhoto(1, day, "images/body/a.jpg", PhotoPose.FRONT),
        BodyPhoto(2, day.minusMonths(1), "images/body/b.jpg", PhotoPose.SIDE),
    )

    /** Shows the photos section; toggles are recorded and applied like the ViewModel does. */
    private fun show(toggled: MutableList<Long>) = compose.setContent {
        var selected by remember { mutableStateOf(emptyList<Long>()) }
        MyFitTheme {
            EvolutionContent(
                EvolutionUiState(loading = false, section = EvolutionSection.PHOTOS, photos = photos, selectedPhotos = selected, today = day, photoDate = day),
                EvolutionActions(), {}, {}, {},
                { id ->
                    toggled += id
                    selected = if (id in selected) selected - id else selected + id
                },
                {}, {},
            )
        }
    }

    @Test
    fun tap_opensTheViewer_onThatPhoto() {
        val toggled = mutableListOf<Long>()
        show(toggled)
        compose.onNodeWithContentDescription("Lado").performClick()
        compose.onNodeWithContentDescription("Fechar").assertExists() // full-screen viewer
        compose.onNodeWithText("2 / 2").assertExists()
        assertEquals(emptyList<Long>(), toggled)
        compose.onNodeWithContentDescription("Fechar").performClick()
        compose.onAllNodesWithContentDescription("Fechar").assertCountEquals(0)
    }

    @Test
    fun compare_tapOpensThePairFullScreen() {
        compose.setContent { MyFitTheme { PhotoCompareContent(PhotoCompareUiState(photos[1], photos[0]), onBack = {}) } }
        compose.onNodeWithContentDescription("Frente").performClick() // the right-hand photo
        compose.onNodeWithText("2 / 2").assertExists()
    }

    @Test
    fun pressAndHold_selects_andThenTapsToggle() {
        val toggled = mutableListOf<Long>()
        show(toggled)
        compose.onNodeWithContentDescription("Frente").performTouchInput { longClick() }
        assertEquals(listOf(1L), toggled)
        compose.onAllNodesWithContentDescription("Fechar").assertCountEquals(0) // no viewer

        // While selecting, a tap toggles instead of opening.
        compose.onNodeWithContentDescription("Lado").performClick()
        assertEquals(listOf(1L, 2L), toggled)
        compose.onAllNodesWithContentDescription("Fechar").assertCountEquals(0)
    }
}
