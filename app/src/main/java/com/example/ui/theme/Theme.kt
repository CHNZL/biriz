package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BirizNavyPrimary,
    onPrimary = BirizSurface,
    primaryContainer = BirizNavyLight,
    onPrimaryContainer = BirizSurface,
    secondary = BirizAmber,
    onSecondary = BirizSurface,
    secondaryContainer = BirizAmberCream,
    onSecondaryContainer = BirizAmber,
    tertiary = BirizTurquoise,
    onTertiary = BirizSurface,
    tertiaryContainer = BirizTurquoiseSoft,
    onTertiaryContainer = BirizNavyPrimary,
    background = BirizBackground,
    onBackground = BirizTextPrimary,
    surface = BirizSurface,
    onSurface = BirizTextPrimary,
    surfaceVariant = BirizSurfaceVariant,
    onSurfaceVariant = BirizTextSecondary,
    outline = BirizBorder,
    error = BirizError,
    onError = BirizSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = BirizTurquoise,
    onPrimary = Color.White,
    primaryContainer = BirizDarkSurface,
    onPrimaryContainer = BirizDarkTextPrimary,
    secondary = BirizAmber,
    onSecondary = Color.White,
    secondaryContainer = BirizDarkSurfaceGlass,
    onSecondaryContainer = BirizAmberLight,
    tertiary = BirizTurquoiseLight,
    onTertiary = Color.White,
    tertiaryContainer = BirizDarkCardGlass,
    onTertiaryContainer = BirizTurquoise,
    background = BirizDarkBackground,
    onBackground = BirizDarkTextPrimary,
    surface = BirizDarkSurface,
    onSurface = BirizDarkTextPrimary,
    surfaceVariant = BirizDarkCardGlass,
    onSurfaceVariant = BirizDarkTextSecondary,
    outline = BirizDarkCardBorder,
    error = BirizError,
    onError = Color.White
)

@Composable
fun BirizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Preserve intentional brand identity without system wallpaper dynamic override
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = (if (darkTheme) BirizDarkBackground else BirizBackground).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
