package io.github.luannamorim.myfit.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Coil model for an image stored by ImageStore (relative path under filesDir/images only). */
@Composable
fun localImageModel(relativePath: String?): File? {
    val context = LocalContext.current
    return remember(relativePath) {
        relativePath?.let {
            val images = File(context.filesDir, "images").canonicalFile
            File(context.filesDir, it).canonicalFile.takeIf { f -> f.path.startsWith(images.path + File.separator) }
        }
    }
}
