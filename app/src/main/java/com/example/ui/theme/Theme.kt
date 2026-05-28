package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GeometricPrimary,
    primaryContainer = GeometricPrimaryContainer,
    onPrimaryContainer = GeometricOnPrimaryContainer,
    secondary = GeometricSecondary,
    background = GeometricBg,
    surface = GeometricSurface,
    onPrimary = Color.White,
    onSecondary = GeometricOnPrimaryContainer,
    onBackground = GeometricTextDark,
    onSurface = GeometricTextDark,
    error = GeometricAlert
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Geometric Balance is optimized for crisp, elegant Light MD3
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
