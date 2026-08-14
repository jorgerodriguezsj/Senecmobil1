package com.vozmayores.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.sin

/**
 * Cuerpo del "dispositivo": rectángulo redondeado con acabado
 * beige, borde y sombra proyectada. Se posa sobre el fondo del
 * escritorio.
 */
@Composable
fun RetroDevice(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(elevation = 20.dp, shape = RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(VozColors.Body)
            .border(width = 2.dp, color = VozColors.BodyStroke, shape = RoundedCornerShape(28.dp))
            .padding(16.dp),
    ) {
        content()
    }
}

/**
 * Fila superior del dispositivo: cámara falsa a la izquierda y
 * "dial" rojo a la derecha, ambos puramente decorativos.
 */
@Composable
fun DeviceTopStrip(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Camera + label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VozColors.CameraGlass)
                    .border(width = 3.dp, color = Color(0xFFC79A93), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6E504A)),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cámara",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFB05656),
            )
        }
        // Dial
        Box(
            modifier = Modifier
                .shadow(elevation = 4.dp, shape = CircleShape)
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(VozColors.KnobRed, VozColors.KnobRedDark),
                        radius = 60f,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val n = 12
                val radius = size.minDimension / 2f
                for (i in 0 until n) {
                    val a = (i.toFloat() / n) * (2f * PI.toFloat())
                    val x1 = size.width / 2f + kotlin.math.cos(a) * radius * 0.55f
                    val y1 = size.height / 2f + kotlin.math.sin(a) * radius * 0.55f
                    val x2 = size.width / 2f + kotlin.math.cos(a) * radius * 0.9f
                    val y2 = size.height / 2f + kotlin.math.sin(a) * radius * 0.9f
                    drawLine(
                        color = VozColors.KnobRedDark,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2f,
                    )
                }
            }
        }
    }
}

/**
 * Marco de la "pantalla" interna: rectángulo redondeado con borde
 * oscuro grueso (bezel) y fondo claro donde va el contenido.
 */
@Composable
fun InnerScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(VozColors.ScreenBezel)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(VozColors.ScreenBg),
        ) {
            content()
        }
    }
}

/**
 * Panel rojo inferior con la onda de audio dibujada por Canvas.
 * ES el push-to-talk: mantén pulsado → habla → suelta.
 */
@Composable
fun PttWavePanel(
    pressed: Boolean,
    busy: Boolean,
    enabled: Boolean,
    level: Float,
    onPress: suspend PressGestureScope.(Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetBg = when {
        !enabled -> VozColors.PttPanelDisabled
        pressed -> VozColors.PttPanelPressed
        busy -> VozColors.PttPanelBusy
        else -> VozColors.PttPanel
    }
    val bg by animateColorAsState(targetBg, tween(300), label = "ptt-panel-bg")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(width = 2.dp, color = Color(0x33FFFFFF), shape = RoundedCornerShape(18.dp))
            .pointerInput(enabled) {
                detectTapGestures(onPress = onPress)
            },
        contentAlignment = Alignment.Center,
    ) {
        WaveformCanvas(
            level = level,
            pressed = pressed,
            color = VozColors.Waveform,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        )
    }
}

@Composable
private fun WaveformCanvas(
    level: Float,
    pressed: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // Base "ambient" cuando no se graba, sube con el nivel al hablar.
    val baseAmplitude = if (pressed) 0.35f + level * 0.65f else 0.15f
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (pressed) 1400 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-phase",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val steps = 100
        val amp = baseAmplitude * (h / 2f) * 0.9f

        val path = Path()
        for (i in 0..steps) {
            val x = w * i / steps.toFloat()
            val nx = x / w
            // Suma de tres senos a distinta frecuencia para look
            // "orgánico" en vez de un seno puro.
            val y = cy + amp * (
                sin(nx * 14f + phase) * 0.55f +
                    sin(nx * 27f + phase * 0.7f) * 0.28f +
                    sin(nx * 6f - phase * 0.4f) * 0.32f
            )
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round),
        )
    }
}

