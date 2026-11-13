package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val TagSwipeColorScheme = darkColorScheme(
    primary = AmberAccent,
    onPrimary = ObsidianCanvas,
    primaryContainer = StudioSurfaceElevated,
    onPrimaryContainer = AmberAccent,
    secondary = TealAccent,
    onSecondary = ObsidianCanvas,
    secondaryContainer = StudioSurfaceElevated,
    onSecondaryContainer = TealAccent,
    tertiary = VioletAccent,
    onTertiary = ObsidianCanvas,
    tertiaryContainer = StudioSurfaceElevated,
    onTertiaryContainer = VioletAccent,
    background = ObsidianCanvas,
    onBackground = IvoryText,
    surface = StudioSurface,
    onSurface = IvoryText,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = SlateTextSecondary,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceBright,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.6f),
    error = CoralAccent,
    onError = ObsidianCanvas,
)

private val TagSwipeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TagSwipeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TagSwipeColorScheme,
        typography = TagSwipeTypography,
        shapes = TagSwipeShapes,
        content = content,
    )
}
