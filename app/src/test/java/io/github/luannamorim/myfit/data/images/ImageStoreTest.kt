package io.github.luannamorim.myfit.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ImageStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ImageStore(context, Dispatchers.Unconfined)

    private fun sourceImage(width: Int, height: Int): Uri {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val file = File(context.cacheDir, "src_${width}x$height.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun import_downscalesLongestSideTo1600() = runTest {
        val path = store.import(sourceImage(4000, 3000), ImageKind.BODY)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(store.file(path).path, opts)
        assertEquals(1600, opts.outWidth)
        assertEquals(1200, opts.outHeight)
    }

    @Test
    fun import_keepsSmallImagesAtOriginalSize() = runTest {
        val path = store.import(sourceImage(800, 600), ImageKind.EXERCISE)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(store.file(path).path, opts)
        assertEquals(800, opts.outWidth)
    }

    @Test
    fun import_returnsRelativePathUnderKindDir() = runTest {
        val path = store.import(sourceImage(100, 100), ImageKind.EXERCISE)
        assertTrue(path, path.startsWith("images/exercises/") && path.endsWith(".jpg"))
        assertTrue(store.file(path).exists())
        assertFalse(File(store.file(path).path + ".tmp").exists())
    }

    @Test
    fun traversalPaths_neverResolveOutsideImages() = runTest {
        val secret = File(context.filesDir, "secret.txt").apply { writeText("x") }
        assertFalse(store.file("images/../secret.txt").exists())
        assertFalse(store.file("../files/secret.txt").exists())
        store.delete("images/../secret.txt")
        assertTrue(secret.exists())
    }

    @Test
    fun deleteOrphans_removesOnlyUnreferenced() = runTest {
        val keep = store.import(sourceImage(100, 100), ImageKind.BODY)
        val orphan = store.import(sourceImage(100, 100), ImageKind.EXERCISE)
        assertEquals(1, store.deleteOrphans(setOf(keep)))
        assertTrue(store.file(keep).exists())
        assertFalse(store.file(orphan).exists())
    }
}
