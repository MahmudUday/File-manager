package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    val isLight: Boolean,
    val background: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val hairlineBorder: Color,
    val subtleBorder: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val accentGlow: Color,
    val glowBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

val LocalOnyxColors = staticCompositionLocalOf {
    OnyxExtendedColors(
        isLight = false,
        background = AmoledBlack,
        cardSurface = AmoledSurfaceLow,
        elevatedSurface = AmoledSurface,
        hairlineBorder = AmoledOutline,
        subtleBorder = AmoledOutlineSubtle,
        accent = AccentCyan,
        onAccent = Color(0xFF05070B),
        accentSoft = AccentCyan.copy(alpha = 0.16f),
        accentGlow = AccentCyan.copy(alpha = 0.28f),
        glowBorder = AccentCyan.copy(alpha = 0.52f),
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
    val isLight = themeMode == ThemeMode.LIGHT
    val isPureAmoled = themeMode == ThemeMode.PURE_AMOLED

    val accent = if (isLight) {
        when (accentPreset) {
            AccentPreset.CYAN -> Color(0xFF008AA8)
            AccentPreset.EMERALD -> Color(0xFF009B5A)
            AccentPreset.AMBER -> Color(0xFFD97706)
            AccentPreset.VIOLET -> Color(0xFF7C3AED)
            AccentPreset.CORAL -> Color(0xFFE11D48)
            AccentPreset.PLATINUM -> Color(0xFF1E293B)
        }
    } else {
        accentPreset.color
    }

    val onAccent = if (isLight) {
        Color.White
    } else if (accentPreset == AccentPreset.PLATINUM || accentPreset == AccentPreset.CYAN || accentPreset == AccentPreset.EMERALD || accentPreset == AccentPreset.AMBER) {
        Color(0xFF05070B)
    } else {
        Color.White
    }

    val bg = when (themeMode) {
        ThemeMode.PURE_AMOLED -> AmoledBlack
        ThemeMode.DEEP_OBSIDIAN -> ObsidianBackground
        ThemeMode.LIGHT -> LightBackground
    }
    val card = when (themeMode) {
        ThemeMode.PURE_AMOLED -> AmoledSurfaceLow
        ThemeMode.DEEP_OBSIDIAN -> ObsidianSurfaceLow
        ThemeMode.LIGHT -> LightSurfaceLow
    }
    val surface = when (themeMode) {
        ThemeMode.PURE_AMOLED -> AmoledSurface
        ThemeMode.DEEP_OBSIDIAN -> ObsidianSurface
        ThemeMode.LIGHT -> LightSurface
    }
    val surfaceHigh = when (themeMode) {
        ThemeMode.PURE_AMOLED -> AmoledSurfaceHigh
        ThemeMode.DEEP_OBSIDIAN -> ObsidianSurfaceHigh
        ThemeMode.LIGHT -> LightSurfaceHigh
    }
    val outline = when (themeMode) {
        ThemeMode.PURE_AMOLED -> AmoledOutline
        ThemeMode.DEEP_OBSIDIAN -> ObsidianOutline
        ThemeMode.LIGHT -> LightOutline
    }
    val outlineSubtle = when (themeMode) {
        ThemeMode.PURE_AMOLED -> AmoledOutlineSubtle
        ThemeMode.DEEP_OBSIDIAN -> ObsidianOutline.copy(alpha = 0.5f)
        ThemeMode.LIGHT -> LightOutlineSubtle
    }

    val txtPrimary = if (isLight) LightTextPrimary else TextPrimary
    val txtSecondary = if (isLight) LightTextSecondary else TextSecondary
    val txtTertiary = if (isLight) LightTextTertiary else TextTertiary

    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.14f),
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.85f),
            onSecondary = onAccent,
            secondaryContainer = surfaceHigh,
            onSecondaryContainer = txtPrimary,
            tertiary = TypeVideoColor,
            onTertiary = Color.White,
            background = bg,
            onBackground = txtPrimary,
            surface = card,
            onSurface = txtPrimary,
            surfaceVariant = surface,
            onSurfaceVariant = txtSecondary,
            surfaceContainerLowest = bg,
            surfaceContainerLow = card,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = LightSurfaceHigh,
            outline = outline,
            outlineVariant = outlineSubtle,
            error = ErrorCrimson,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accent.copy(alpha = 0.20f),
            onPrimaryContainer = accent,
            secondary = accent.copy(alpha = 0.88f),
            onSecondary = onAccent,
            secondaryContainer = surfaceHigh,
            onSecondaryContainer = txtPrimary,
            tertiary = TypeVideoColor,
            onTertiary = Color.White,
            background = bg,
            onBackground = txtPrimary,
            surface = card,
            onSurface = txtPrimary,
            surfaceVariant = surface,
            onSurfaceVariant = txtSecondary,
            surfaceContainerLowest = bg,
            surfaceContainerLow = card,
            surfaceContainer = surface,
            surfaceContainerHigh = surfaceHigh,
            surfaceContainerHighest = if (isPureAmoled) AmoledSurfaceHighest else ObsidianSurfaceHigh,
            outline = outline,
            outlineVariant = outlineSubtle,
            error = ErrorCrimson,
            onError = Color.White
        )
    }

    val extended = OnyxExtendedColors(
        isLight = isLight,
        background = bg,
        cardSurface = card,
        elevatedSurface = surface,
        hairlineBorder = outline,
        subtleBorder = outlineSubtle,
        accent = accent,
        onAccent = onAccent,
        accentSoft = accent.copy(alpha = if (isLight) 0.12f else 0.16f),
        accentGlow = accent.copy(alpha = if (isLight) 0.18f else 0.28f),
        glowBorder = accent.copy(alpha = if (isLight) 0.44f else 0.54f),
        textPrimary = txtPrimary,
        textSecondary = txtSecondary,
        textTertiary = txtTertiary
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
