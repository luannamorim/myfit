package io.github.luannamorim.myfit.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class PhotoViewerScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun photo_viewer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "images/body/viewer.jpg").apply { parentFile?.mkdirs() }
        val bitmap = Bitmap.createBitmap(600, 800, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(60, 70, 80))
            drawCircle(300f, 300f, 160f, Paint().apply { color = Color.rgb(200, 170, 140) })
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                PhotoViewer(
                    listOf(ViewerPage("images/body/viewer.jpg", "Nov 12, 2026 · Front"), ViewerPage("images/body/viewer.jpg", "Oct 12, 2026 · Side")),
                    initialPage = 0,
                    onDismiss = {},
                )
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("$SCREENSHOT_DIR/photo_viewer.png")
    }
}
