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
    primary = ElectricLime,
    onPrimary = Color.Black,
    primaryContainer = ElectricLimeContainer,
    onPrimaryContainer = ElectricLime,
    
    secondary = GlacierBlue,
    onSecondary = Color.Black,
    secondaryContainer = GlacierBlueContainer,
    onSecondaryContainer = GlacierBlue,
    
    tertiary = SuccessGreen,
    onTertiary = Color.Black,
    
    background = MidnightBlack,
    surface = DeepNavySurface,
    surfaceVariant = NavySurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = OnNavySurfaceVariant,
    
    error = PremiumRed,
    outline = NavySurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricLimeContainer, // Darker lime for light mode readability
    onPrimary = Color.White,
    primaryContainer = ElectricLime,
    onPrimaryContainer = Color.Black,
    
    secondary = GlacierBlueContainer, // Using deep teal for light mode contrast
    onSecondary = Color.White,
    secondaryContainer = GlacierBlue,
    onSecondaryContainer = Color.Black,
    
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    
    background = PearlWhite,
    surface = SnowSurface,
    surfaceVariant = LightGrayVariant,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = OnLightGrayVariant,

    error = PremiumRed,
    outline = LightGrayVariant
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
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
