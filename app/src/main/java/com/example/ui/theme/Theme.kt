package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IlamGold,
    onPrimary = IlamBlack,
    primaryContainer = IlamDarkCard,
    onPrimaryContainer = IlamGoldLight,
    secondary = IlamGoldLight,
    onSecondary = IlamBlack,
    background = IlamBlack,
    onBackground = IlamWhite,
    surface = IlamDarkSurface,
    onSurface = IlamWhite,
    surfaceVariant = IlamDarkCard,
    onSurfaceVariant = IlamGoldLight,
    outline = IlamDarkBorder,
    error = IlamDanger,
    onError = IlamWhite
)

private val LightColorScheme = lightColorScheme(
    primary = IlamBlack,
    onPrimary = IlamWhite,
    primaryContainer = IlamDarkSurface,
    onPrimaryContainer = IlamGoldLight,
    secondary = IlamGold,
    onSecondary = IlamBlack,
    background = IlamOffWhite,
    onBackground = IlamTextPrimary,
    surface = IlamWhite,
    onSurface = IlamTextPrimary,
    surfaceVariant = IlamOffWhite,
    onSurfaceVariant = IlamTextSecondary,
    outline = IlamLightBorder,
    error = IlamDanger,
    onError = IlamWhite
)

@Composable
fun IlamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for any existing reference
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = IlamTheme(darkTheme = darkTheme, content = content)
