package com.fitness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

internal val DarkColorScheme = darkColorScheme(
    primary = SportyOrange,
    onPrimary = PureWhite,
    primaryContainer = SportyOrangeContainer,
    onPrimaryContainer = OnSportyOrangeContainer,
    
    secondary = CoolGray,
    onSecondary = PureWhite,
    secondaryContainer = Graphite,
    onSecondaryContainer = CoolGray,
    
    background = DeepSpace,
    surface = DeepSpace,
    surfaceVariant = MidnightSurface,
    onBackground = PureWhite,
    onSurface = PureWhite,
    onSurfaceVariant = CoolGray,
    
    outline = Graphite,
    error = SportyRed,
    tertiary = SuccessGreen,
    tertiaryContainer = SuccessGreenContainer
)

internal val LightColorScheme = lightColorScheme(
    primary = SportyOrange,
    onPrimary = PureWhite,
    primaryContainer = SportyOrangeContainer,
    onPrimaryContainer = SportyOrange,
    
    secondary = WarmGray,
    onSecondary = PureWhite,
    secondaryContainer = GhostWhite,
    onSecondaryContainer = WarmGray,
    
    background = SoftCloud,
    surface = PureWhite,
    surfaceVariant = GhostWhite,
    onBackground = DeepSpace,
    onSurface = DeepSpace,
    onSurfaceVariant = WarmGray,

    outline = GhostWhite,
    error = SportyRed,
    tertiary = SuccessGreen,
    tertiaryContainer = SuccessGreenContainer
)

@Composable
expect fun FitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
)
