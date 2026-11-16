package io.github.luannamorim.myfit.ui.common

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR")
class PhotoSourceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sheet_offersCameraAndGallery() {
        var camera = 0
        var gallery = 0
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) { PhotoSourceOptions(onCamera = { camera++ }, onGallery = { gallery++ }) }
        }
        compose.onNodeWithText("Tirar foto").performClick()
        compose.onNodeWithText("Escolher da galeria").performClick()
        assertEquals(1, camera)
        assertEquals(1, gallery)
    }

    @Test
    fun capture_isAFileProviderUriInCache_andOldCapturesAreCleared() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = CameraCapture.newUri(context)
        assertEquals("${context.packageName}.camera", first.authority)
        val firstFile = File(File(context.cacheDir, "camera"), first.lastPathSegment!!)
        firstFile.writeText("jpeg")
        assertTrue(firstFile.exists())
        CameraCapture.newUri(context)
        assertFalse(firstFile.exists()) // one capture at a time: the previous temp file goes
    }
}
