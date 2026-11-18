package io.github.luannamorim.myfit.data.images

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/**
 * Decodes [uri] with its longest side ≤ [maxSide] and the EXIF orientation applied. Large images are
 * subsampled while decoding, so a 12 MP photo never lands in memory at full size.
 * Throws [IllegalArgumentException] when [uri] isn't an image, [IOException] when it can't be read.
 */
internal fun decodeScaled(resolver: ContentResolver, uri: Uri, maxSide: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val input = resolver.openInputStream(uri) ?: throw IOException("cannot open $uri")
    input.use { BitmapFactory.decodeStream(it, null, bounds) } // returns null by design (bounds only)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "not an image: $uri" }

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("decode failed: $uri")

    val exif = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it) } }.getOrNull()
    val matrix = Matrix()
    exif?.let {
        if (it.isFlipped) matrix.postScale(-1f, 1f)
        matrix.postRotate(it.rotationDegrees.toFloat())
    }
    val scale = min(1f, maxSide.toFloat() / max(decoded.width, decoded.height))
    if (scale < 1f) matrix.postScale(scale, scale)
    if (matrix.isIdentity) return decoded
    val transformed = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (transformed !== decoded) decoded.recycle()
    return transformed
}
