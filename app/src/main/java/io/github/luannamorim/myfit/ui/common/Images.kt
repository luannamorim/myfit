package io.github.luannamorim.myfit.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Coil model for an image stored by ImageStore (relative path under filesDir). */
@Composable
fun localImageModel(relativePath: String?): File? {
    val context = LocalContext.current
    return remember(relativePath) { relativePath?.let { File(context.filesDir, it) } }
}
