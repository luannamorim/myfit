package io.github.luannamorim.myfit.data.images

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Where each kind of image goes and how much it is reduced (longest side in px, JPEG quality). */
enum class ImageKind(val dir: String, val maxSide: Int = ImageStore.MAX_SIDE, val quality: Int = 85) {
    EXERCISE("exercises"),
    BODY("body"),

    /** Workout and activity photos are mementos: smaller and lighter (~150–300 KB). */
    SESSION("sessions", maxSide = 1280, quality = 80),
}

/**
 * Stores user images inside app-private storage. Images are decoded downscaled (longest side
 * ≤ [MAX_SIDE]), EXIF orientation is applied, and they are re-encoded as JPEG (which also drops
 * GPS/EXIF metadata). The database keeps only the returned relative path.
 */
@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val root: File get() = context.filesDir

    /** Copies the picked image right away (picker grants are temporary). Returns e.g. "images/body/<uuid>.jpg". */
    suspend fun import(uri: Uri, kind: ImageKind): String = withContext(io) {
        val bitmap = decodeScaled(context.contentResolver, uri, kind.maxSide)
        val relative = "$IMAGES_DIR/${kind.dir}/${UUID.randomUUID()}.jpg"
        val target = File(root, relative)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${target.name}.tmp")
        try {
            tmp.outputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, kind.quality, out)) { "JPEG encode failed" }
            }
            if (!tmp.renameTo(target)) throw IOException("rename failed: $tmp")
        } finally {
            bitmap.recycle()
            tmp.delete()
        }
        relative
    }

    /**
     * Resolves a stored relative path. Paths that would escape filesDir/images (e.g. "../") resolve
     * to a non-existent file inside the images dir, so they can never read or delete app data.
     */
    fun file(relativePath: String): File {
        val imagesDir = File(root, IMAGES_DIR).canonicalFile
        val candidate = File(root, relativePath).canonicalFile
        return if (candidate.path.startsWith(imagesDir.path + File.separator)) candidate else File(imagesDir, INVALID)
    }

    suspend fun delete(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        withContext(io) {
            val target = file(relativePath)
            if (target.name != INVALID) target.delete()
        }
    }

    /**
     * Deletes image files not present in [referenced] (relative paths). Files modified after
     * [olderThanMs] are kept (an editor may hold a freshly picked, not yet saved image).
     */
    suspend fun deleteOrphans(referenced: Set<String>, olderThanMs: Long = Long.MAX_VALUE): Int = withContext(io) {
        val base = File(root, IMAGES_DIR)
        if (!base.exists()) return@withContext 0
        base.walkTopDown().filter { it.isFile }.count { f ->
            val relative = f.relativeTo(root).invariantSeparatorsPath
            if (relative !in referenced && f.lastModified() <= olderThanMs) f.delete() else false
        }
    }

    companion object {
        const val IMAGES_DIR = "images"
        const val MAX_SIDE = 1600
        private const val INVALID = ".invalid"
    }
}
