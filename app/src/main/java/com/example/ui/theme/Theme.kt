package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FoliishoDarkColorScheme = darkColorScheme(
    primary = FoliishoAmber,
    onPrimary = FoliishoNavyDark,
    primaryContainer = FoliishoAmberDark,
    onPrimaryContainer = FoliishoAmberLight,
    secondary = FoliishoGreen,
    onSecondary = Color.White,
    secondaryContainer = FoliishoGreenDark,
    onSecondaryContainer = FoliishoGreenLight,
    tertiary = FoliishoCyan,
    background = FoliishoSurfaceDark,
    surface = FoliishoCardDark,
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF8FAFC),
    error = FoliishoRed
)

private val FoliishoLightColorScheme = lightColorScheme(
    primary = FoliishoAmberDark,
    onPrimary = Color.White,
    primaryContainer = FoliishoAmberLight,
    onPrimaryContainer = FoliishoNavyDark,
    secondary = FoliishoGreenDark,
    onSecondary = Color.White,
    secondaryContainer = FoliishoGreenLight,
    onSecondaryContainer = FoliishoNavyDark,
    tertiary = FoliishoCyan,
    background = FoliishoSurfaceLight,
    surface = FoliishoCardLight,
    surfaceVariant = FoliishoSurfaceVariantLight,
    onBackground = FoliishoNavyDark,
    onSurface = FoliishoNavyDark,
    error = FoliishoRed
)

@Composable
fun FoliishoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FoliishoDarkColorScheme
        else -> FoliishoLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for template compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = FoliishoTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
