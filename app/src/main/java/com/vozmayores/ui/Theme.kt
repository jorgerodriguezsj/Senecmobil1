package com.vozmayores.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta inspirada en el mockup: dispositivo de juguete retro
// con cuerpo crema, pantalla clara y panel rojo abajo.
internal object VozColors {
    // Escritorio (fondo detrás del "dispositivo")
    val Desk = Color(0xFFB6D4CE)

    // Cuerpo de la carcasa
    val Body = Color(0xFFE9DBC6)
    val BodyStroke = Color(0xFFCFB99A)
    val BodyShadow = Color(0xFFA88E6C)

    // Cámara y decoración
    val CameraGlass = Color(0xFF1A1512)
    val KnobRed = Color(0xFFE4877E)
    val KnobRedDark = Color(0xFF9E4A44)

    // Pantalla interna
    val ScreenBezel = Color(0xFF1D1A17)
    val ScreenBg = Color(0xFFF3E7D6)
    val ScreenInk = Color(0xFF3A2E22)

    // Panel PTT
    val PttPanel = Color(0xFF9A2020)
    val PttPanelPressed = Color(0xFF6F1414)
    val PttPanelBusy = Color(0xFFC17A1E)
    val PttPanelDisabled = Color(0xFF7D6E6E)
    val Waveform = Color(0xFFF3D8CC)
}

private val LightColors = lightColorScheme(
    primary = VozColors.PttPanel,
    onPrimary = Color.White,
    secondary = VozColors.KnobRedDark,
    onSecondary = Color.White,
    background = VozColors.Desk,
    onBackground = VozColors.ScreenInk,
    surface = VozColors.ScreenBg,
    onSurface = VozColors.ScreenInk,
    surfaceVariant = VozColors.Body,
    onSurfaceVariant = Color(0xFF6A4E32),
    outline = VozColors.BodyStroke,
)

private val VozTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Black),
    displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
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
