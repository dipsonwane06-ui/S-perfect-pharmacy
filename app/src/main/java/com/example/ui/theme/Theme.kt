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

private val DarkColorScheme = darkColorScheme(
    primary = PharmacyDarkPrimary,
    onPrimary = Color(0xFF003830),
    primaryContainer = PharmacyTealDark,
    onPrimaryContainer = PharmacyDarkPrimary,
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D47A1),
    tertiary = PharmacyAmber,
    onTertiary = Color.Black,
    background = PharmacyDarkBackground,
    onBackground = Color(0xFFE0E3E1),
    surface = PharmacyDarkSurface,
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = PharmacyDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4D5D0),
    outline = PharmacyDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PharmacyTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7EFE6),
    onPrimaryContainer = PharmacyTealDark,
    secondary = PharmacyBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4F5),
    onSecondaryContainer = PharmacyNavy,
    tertiary = PharmacyAmberDark,
    onTertiary = Color.White,
    background = PharmacyBackgroundLight,
    onBackground = Color(0xFF191C1B),
    surface = PharmacySurfaceLight,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = PharmacySurfaceVariantLight,
    onSurfaceVariant = Color(0xFF3F4946),
    outline = PharmacyOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled by default to preserve the signature Imperfect Pharmacy brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
