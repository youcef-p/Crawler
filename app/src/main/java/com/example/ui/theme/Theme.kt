package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ReelScraperDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CinemaBlack,
    primaryContainer = VividVioletDark,
    onPrimaryContainer = NeonCyanLight,
    secondary = VividViolet,
    onSecondary = TextPrimary,
    secondaryContainer = CinemaSurfaceVariant,
    onSecondaryContainer = VividVioletLight,
    tertiary = CoralPink,
    onTertiary = TextPrimary,
    background = CinemaBlack,
    onBackground = TextPrimary,
    surface = CinemaSurface,
    onSurface = TextPrimary,
    surfaceVariant = CinemaSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CinemaSurfaceHighlight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep immersive cinema dark aesthetic
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CinemaBlack.toArgb()
                window.navigationBarColor = CinemaBlack.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ReelScraperDarkColorScheme,
        typography = Typography,
        content = content
    )
}
