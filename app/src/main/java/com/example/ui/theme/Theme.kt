package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AccentPreset
import com.example.data.model.ThemeMode

@Immutable
data class OnyxExtendedColors(
    val background: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val hairlineBorder: Color,
    val subtleBorder: Color,
    val accent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

val LocalOnyxColors = staticCompositionLocalOf {
    OnyxExtendedColors(
        background = AmoledBlack,
        cardSurface = AmoledSurfaceLow,
        elevatedSurface = AmoledSurface,
        hairlineBorder = AmoledOutline,
        subtleBorder = AmoledOutlineSubtle,
        accent = AccentCyan,
        accentSoft = AccentCyan.copy(alpha = 0.14f),
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary
    )
}

private val OnyxShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OnyxFilesTheme(
    themeMode: ThemeMode = ThemeMode.PURE_AMOLED,
    accentPreset: AccentPreset = AccentPreset.CYAN,
    content: @Composable () -> Unit
) {
    val accent = accentPreset.color
    val onAccent = if (accentPreset == AccentPreset.PLATINUM || accentPreset == AccentPreset.CYAN || accentPreset == AccentPreset.EMERALD || accentPreset == AccentPreset.AMBER) {
        Color(0xFF05070B)
    } else {
        Color.White
    }

    val isPureAmoled = themeMode == ThemeMode.PURE_AMOLED

    val bg = if (isPureAmoled) AmoledBlack else ObsidianBackground
    val card = if (isPureAmoled) AmoledSurfaceLow else ObsidianSurfaceLow
    val surface = if (isPureAmoled) AmoledSurface else ObsidianSurface
    val surfaceHigh = if (isPureAmoled) AmoledSurfaceHigh else ObsidianSurfaceHigh
    val outline = if (isPureAmoled) AmoledOutline else ObsidianOutline
    val outlineSubtle = if (isPureAmoled) AmoledOutlineSubtle else ObsidianOutline.copy(alpha = 0.5f)

    val colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accent.copy(alpha = 0.18f),
        onPrimaryContainer = accent,
        secondary = accent.copy(alpha = 0.85f),
        onSecondary = onAccent,
        secondaryContainer = surfaceHigh,
        onSecondaryContainer = TextPrimary,
        tertiary = TypeVideoColor,
        onTertiary = Color.White,
        background = bg,
        onBackground = TextPrimary,
        surface = card,
        onSurface = TextPrimary,
        surfaceVariant = surface,
        onSurfaceVariant = TextSecondary,
        surfaceContainerLowest = bg,
        surfaceContainerLow = card,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = AmoledSurfaceHighest,
        outline = outline,
        outlineVariant = outlineSubtle,
        error = ErrorCrimson,
        onError = Color.White
    )

    val extended = OnyxExtendedColors(
        background = bg,
        cardSurface = card,
        elevatedSurface = surface,
        hairlineBorder = outline,
        subtleBorder = outlineSubtle,
        accent = accent,
        accentSoft = accent.copy(alpha = 0.14f),
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textTertiary = TextTertiary
    )

    CompositionLocalProvider(LocalOnyxColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OnyxTypography,
            shapes = OnyxShapes,
            content = content
        )
    }
}
