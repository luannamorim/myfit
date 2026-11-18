package io.github.luannamorim.myfit.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MyFit color tokens. Graphite neutrals plus a single green accent reserved for progress and
 * positive/primary actions. Values are validated for WCAG contrast in ColorContrastTest.
 */
@Immutable
data class MyFitColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val outline: Color,
    val outlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentMuted: Color,
    val danger: Color,
    val warning: Color,
    /** Kind colors (user request, 1.14.0): calorie target hit (orange) and water target hit (blue); workouts use [accent]. */
    val food: Color,
    val water: Color,
    /** BMI / body-fat "caution" dot: yellow, told apart from [food] orange and [warning] amber (user request, 1.15.0). */
    val caution: Color,
    val isDark: Boolean,
)

val DarkColors = MyFitColors(
    background = Color(0xFF0B0B0C),
    surface = Color(0xFF141416),
    surfaceRaised = Color(0xFF1C1C1F),
    outline = Color(0xFF2A2A2E),
    outlineStrong = Color(0xFF3A3A40),
    textPrimary = Color(0xFFF5F5F4),
    textSecondary = Color(0xFFA1A1A8),
    textTertiary = Color(0xFF6B6B73),
    accent = Color(0xFF47F55E),
    onAccent = Color(0xFF0B0B0C),
    accentMuted = Color(0x2447F55E),
    danger = Color(0xFFF2555A),
    warning = Color(0xFFF5A524),
    food = Color(0xFFFF8A3D),
    water = Color(0xFF4DA3FF),
    caution = Color(0xFFFACC15),
    isDark = true,
)

val LightColors = MyFitColors(
    background = Color(0xFFFAFAF9),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF2F2F0),
    outline = Color(0xFFE2E2DF),
    outlineStrong = Color(0xFFCBCBC7),
    textPrimary = Color(0xFF0B0B0C),
    textSecondary = Color(0xFF5A5A62),
    textTertiary = Color(0xFF8E8E95),
    accent = Color(0xFF15803D),
    onAccent = Color(0xFFFFFFFF),
    accentMuted = Color(0x1F15803D),
    danger = Color(0xFFCD2B31),
    warning = Color(0xFFB45309),
    food = Color(0xFFC2410C),
    water = Color(0xFF1D6FD1),
    caution = Color(0xFFA16207),
    isDark = false,
)

val LocalMyFitColors = staticCompositionLocalOf { DarkColors }
