package com.fitness.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SportyOrange,
    onPrimary = Color.White,
    primaryContainer = SportyOrangeDark,
    onPrimaryContainer = Color.White,
    
    secondary = SportyTeal,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00B8D4),
    
    background = CarbonBlack,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    error = SportyRed,
    errorContainer = Color(0xFFD50000),
    onErrorContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SportyOrange,
    onPrimary = Color.White,
    primaryContainer = SportyOrangeLight,
    onPrimaryContainer = Color.Black,
    
    secondary = SportyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF82B1FF),
    
    background = OffWhite,
    surface = WhiteSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF424242),

    error = SportyRed,
    errorContainer = Color(0xFFFF8A80),
    onErrorContainer = Color.Black
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set dynamicColor to false by default so our sporty colors aren't overridden by Android 12+ wallpaper colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
