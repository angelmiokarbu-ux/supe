package com.supe.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonPrimaryDark,
    onPrimary = Color(0xFF680009),
    primaryContainer = CrimsonPrimaryContainer,
    onPrimaryContainer = CrimsonOnPrimaryContainer,
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410005),
    secondary = Color(0xFF775653),
    onSecondary = Color.White,
    background = Color(0xFFFFF8F7),
    onBackground = Color(0xFF221919),
    surface = Color.White,
    onSurface = Color(0xFF221919),
    surfaceVariant = Color(0xFFF4DDDB),
    onSurfaceVariant = Color(0xFF534342),
    outline = Color(0xFF857371)
)

@Composable
fun SupeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
