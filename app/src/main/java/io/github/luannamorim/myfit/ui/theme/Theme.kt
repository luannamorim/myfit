package io.github.luannamorim.myfit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

enum class ThemeMode { DARK, LIGHT, SYSTEM }

object MyFitTheme {
    val colors: MyFitColors
        @Composable @ReadOnlyComposable get() = LocalMyFitColors.current
    val type: MyFitType
        @Composable @ReadOnlyComposable get() = LocalMyFitType.current
}

@Composable
fun MyFitTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(
        LocalMyFitColors provides colors,
        LocalMyFitType provides DefaultType,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = DefaultType.toMaterialTypography(),
            shapes = MaterialShapes,
            content = content,
        )
    }
}

private fun MyFitColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent, primaryContainer = accentMuted, onPrimaryContainer = textPrimary,
        secondary = textSecondary, onSecondary = background,
        background = background, onBackground = textPrimary,
        surface = background, onSurface = textPrimary,
        surfaceVariant = surfaceRaised, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised, surfaceContainerHighest = surfaceRaised,
        outline = outlineStrong, outlineVariant = outline,
        error = danger, onError = background,
        inverseSurface = textPrimary, inverseOnSurface = background, inversePrimary = accent,
        secondaryContainer = surfaceRaised, onSecondaryContainer = textPrimary,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent, primaryContainer = accentMuted, onPrimaryContainer = textPrimary,
        secondary = textSecondary, onSecondary = background,
        background = background, onBackground = textPrimary,
        surface = background, onSurface = textPrimary,
        surfaceVariant = surfaceRaised, onSurfaceVariant = textSecondary,
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised, surfaceContainerHighest = surfaceRaised,
        outline = outlineStrong, outlineVariant = outline,
        error = danger, onError = surface,
        inverseSurface = textPrimary, inverseOnSurface = background, inversePrimary = accent,
        secondaryContainer = surfaceRaised, onSecondaryContainer = textPrimary,
    )
}

private fun MyFitType.toMaterialTypography() = Typography(
    displayLarge = numericHero, displayMedium = display, displaySmall = display,
    headlineLarge = headline, headlineMedium = headline, headlineSmall = title,
    titleLarge = headline, titleMedium = title, titleSmall = bodyStrong,
    bodyLarge = body, bodyMedium = body, bodySmall = caption,
    labelLarge = label, labelMedium = label, labelSmall = caption,
)
