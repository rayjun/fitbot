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
    onPrimary = PureWhite,
    primaryContainer = OnSportyOrangeContainer,
    onPrimaryContainer = SportyOrangeContainer,
    
    // Tab selection color
    secondaryContainer = SportyOrange,
    onSecondaryContainer = PureWhite,
    
    background = PureBlack,
    surface = PureBlack,
    surfaceVariant = DarkGray,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = Color.Gray,
    
    error = SportyRed,
    tertiary = SuccessGreen,
    tertiaryContainer = SuccessGreenContainer
)

private val LightColorScheme = lightColorScheme(
    primary = SportyOrange,
    onPrimary = PureWhite,
    primaryContainer = SportyOrangeContainer,
    onPrimaryContainer = OnSportyOrangeContainer,
    
    // Tab selection color
    secondaryContainer = SportyOrange,
    onSecondaryContainer = PureWhite,
    
    background = PureWhite,
    surface = PureWhite,
    surfaceVariant = LightGray,
    onBackground = PureBlack,
    onSurface = PureBlack,
    onSurfaceVariant = Color.Gray,

    error = SportyRed,
    tertiary = SuccessGreen,
    tertiaryContainer = SuccessGreenContainer
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
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
