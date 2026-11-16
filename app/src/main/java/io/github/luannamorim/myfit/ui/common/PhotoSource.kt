package io.github.luannamorim.myfit.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.io.File

/** Asks where a new photo comes from (camera or gallery). Get one with [rememberPhotoSource], then [choose]. */
@Stable
class PhotoSource internal constructor() {
    internal var choosing by mutableStateOf(false)

    fun choose() {
        choosing = true
    }
}

/**
 * Camera or gallery: [choose] opens a sheet with both; the photo arrives in [onPicked] as a content
 * Uri for [io.github.luannamorim.myfit.data.images.ImageStore.import] (which shrinks it and applies
 * the EXIF rotation). Camera shots go to a temporary file in the cache; no camera permission needed.
 */
@Composable
fun rememberPhotoSource(onPicked: (Uri) -> Unit): PhotoSource {
    val context = LocalContext.current
    val picked by rememberUpdatedState(onPicked)
    val source = remember { PhotoSource() }
    // Survives the activity being recreated while the camera app is in front.
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) picked(uri)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingCapture?.toUri()
        pendingCapture = null
        if (saved && uri != null) picked(uri)
    }
    if (source.choosing) {
        PhotoSourceSheet(
            onDismiss = { source.choosing = false },
            onCamera = {
                source.choosing = false
                val uri = CameraCapture.newUri(context)
                pendingCapture = uri.toString()
                try {
                    camera.launch(uri)
                } catch (_: ActivityNotFoundException) {
                    pendingCapture = null
                    Toast.makeText(context, R.string.photo_no_camera, Toast.LENGTH_SHORT).show()
                }
            },
            onGallery = {
                source.choosing = false
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
    return source
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MyFitTheme.colors.surface,
    ) {
        PhotoSourceOptions(onCamera, onGallery, Modifier.navigationBarsPadding().padding(bottom = Spacing.l))
    }
}

/** The two choices of the photo sheet. */
@Composable
internal fun PhotoSourceOptions(onCamera: () -> Unit, onGallery: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        SourceRow(Icons.Rounded.PhotoCamera, stringResource(R.string.photo_take), onCamera)
        SourceRow(Icons.Rounded.Image, stringResource(R.string.photo_from_gallery), onGallery)
    }
}

@Composable
private fun SourceRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    val c = MyFitTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.screen, vertical = Spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.textSecondary)
        Spacer(Modifier.width(Spacing.l))
        Text(label, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
    }
}

/** Temporary files the camera app writes to, shared through the app's FileProvider. */
internal object CameraCapture {
    private const val DIR = "camera"

    /** A new, empty capture target; earlier captures (already imported) are deleted. */
    fun newUri(context: Context): Uri {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "capture-${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.camera", file)
    }
}
