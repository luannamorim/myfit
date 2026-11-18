package io.github.luannamorim.myfit.data.images

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A small copy of a meal photo for an AI app (Nutrition › *Photo to AI*). It lives in the cache, not
 * with the meal: only the latest one is kept, because the AI app may read it after the share sheet
 * closes. Copying also means we never re-grant a photo-picker Uri we don't own.
 */
@Singleton
class AiPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    /** Shrinks [source] into `cache/ai/` and returns its FileProvider Uri. Throws when [source] can't be read as an image. */
    suspend fun prepare(source: Uri): Uri = withContext(io) {
        val bitmap = decodeScaled(context.contentResolver, source, MAX_SIDE)
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "meal-${UUID.randomUUID()}.jpg")
        val tmp = File(dir, "${target.name}.tmp")
        try {
            tmp.outputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)) { "JPEG encode failed" }
            }
            if (!tmp.renameTo(target)) throw IOException("rename failed: $tmp")
        } finally {
            bitmap.recycle()
            tmp.delete()
        }
        FileProvider.getUriForFile(context, "${context.packageName}.camera", target)
    }

    companion object {
        const val DIR = "ai"
        const val MAX_SIDE = 1280
        const val QUALITY = 80
    }
}
