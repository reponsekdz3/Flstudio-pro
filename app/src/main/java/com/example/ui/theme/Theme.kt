package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FlStudioColorScheme = darkColorScheme(
    primary = FruityOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF421C00),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = FruityCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00363F),
    onSecondaryContainer = Color(0xFFBCEBFF),
    tertiary = FruityLime,
    onTertiary = Color.Black,
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioPanel,
    onSurface = TextPrimary,
    surfaceVariant = StudioPanelLight,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    error = FruityPink,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlStudioColorScheme,
        typography = Typography,
        content = content
    )
}
