package io.github.luannamorim.myfit.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.testutil.clearFileProviderCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AiPhotoStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = AiPhotoStore(context, Dispatchers.Unconfined)
    private val aiDir get() = File(context.cacheDir, "ai")

    @Before fun resetFileProvider() = clearFileProviderCache()

    private fun sourceImage(width: Int, height: Int): Uri {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val file = File(context.cacheDir, "src_${width}x$height.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun prepare_shrinksToMaxSide1280() = runTest {
        val uri = store.prepare(sourceImage(4000, 3000))
        assertEquals("${context.packageName}.camera", uri.authority)
        val file = File(aiDir, uri.lastPathSegment!!)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, opts)
        assertEquals(1280, opts.outWidth)
        assertEquals(960, opts.outHeight)
    }

    @Test
    fun prepare_keepsOnlyTheLatestFile() = runTest {
        store.prepare(sourceImage(800, 600))
        val second = store.prepare(sourceImage(800, 600))
        val files = aiDir.listFiles()!!.map { it.name }
        assertEquals(listOf(second.lastPathSegment), files) // no .tmp left either
    }

    @Test
    fun prepare_rejectsANonImage() {
        val text = File(context.cacheDir, "not-image.txt").apply { writeText("hello") }
        assertThrows(IllegalArgumentException::class.java) { runBlocking { store.prepare(Uri.fromFile(text)) } }
    }
}
