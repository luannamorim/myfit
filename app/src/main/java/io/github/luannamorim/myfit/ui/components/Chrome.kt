package io.github.luannamorim.myfit.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFitTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val c = MyFitTheme.colors
    TopAppBar(
        title = { Text(title, style = MyFitTheme.type.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        modifier = modifier,
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = c.background,
            scrolledContainerColor = c.surface,
            titleContentColor = c.textPrimary,
            navigationIconContentColor = c.textPrimary,
            actionIconContentColor = c.textPrimary,
        ),
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissText: String = "Cancelar",
) {
    val c = MyFitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MyFitShapes.large,
        containerColor = c.surfaceRaised,
        title = { Text(title, style = MyFitTheme.type.title, color = c.textPrimary) },
        text = { Text(message, style = MyFitTheme.type.body, color = c.textSecondary) },
        confirmButton = {
            TextAction(confirmText, onClick = onConfirm, color = if (destructive) c.danger else c.accent)
        },
        dismissButton = { TextAction(dismissText, onClick = onDismiss, color = c.textSecondary) },
    )
}
