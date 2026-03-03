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
    primaryContainer = OnSportyOrangeContainer,
    onPrimaryContainer = SportyOrangeContainer,
    
    secondary = SportySecondaryContainer,
    onSecondary = OnSportySecondaryContainer,
    secondaryContainer = SportySecondary,
    onSecondaryContainer = SportySecondaryContainer,
    
    tertiary = SportyTertiaryContainer,
    onTertiary = OnSportyTertiaryContainer,
    
    background = CarbonBlack,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = OnDarkSurfaceVariant,
    
    error = SportyRed,
    outline = OnDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = SportyOrange,
    onPrimary = Color.White,
    primaryContainer = SportyOrangeContainer,
    onPrimaryContainer = OnSportyOrangeContainer,
    
    secondary = SportySecondary,
    onSecondary = Color.White,
    secondaryContainer = SportySecondaryContainer,
    onSecondaryContainer = OnSportySecondaryContainer,
    
    tertiary = SportyTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SportyTertiaryContainer,
    onTertiaryContainer = OnSportyTertiaryContainer,
    
    background = OffWhite,
    surface = WhiteSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = OnLightSurfaceVariant,

    error = SportyRed,
    outline = OnLightSurfaceVariant
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
            window.statusBarColor = colorScheme.surface.toArgb() // Use surface for clean TopAppBar look
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
