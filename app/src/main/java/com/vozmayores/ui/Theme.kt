package com.vozmayores.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta cálida, alto contraste, pensada para vista cansada.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFF370809),

    secondary = Color(0xFF00695C),
    onSecondary = Color.White,

    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF3D2C1F),

    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF3D2C1F),
    surfaceVariant = Color(0xFFF1E6D9),
    onSurfaceVariant = Color(0xFF6D5F52),

    outline = Color(0xFFCFC0B1),
)

private val VozTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Black),
    displayMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 20.sp),
    bodyMedium = TextStyle(fontSize = 16.sp),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold),
)

@Composable
fun VozTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = VozTypography,
        content = content,
    )
}
