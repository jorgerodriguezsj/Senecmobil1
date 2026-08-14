package com.vozmayores.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vozmayores.audio.AudioRecorder
import com.vozmayores.audio.WhisperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "Voz.PushToTalkScreen"
private const val MODEL_FILENAME = "ggml-base-q5_1.bin"

@Composable
fun PushToTalkScreen() {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder() }
    val scope = rememberCoroutineScope()

    val modelFile = remember {
        File(context.getExternalFilesDir(null), "models/$MODEL_FILENAME")
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) Log.w(TAG, "RECORD_AUDIO denegado")
    }

    var modelReady by remember { mutableStateOf(WhisperEngine.isLoaded) }
    var pressed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (WhisperEngine.isLoaded) {
            modelReady = true
            status = if (hasPermission) "Listo" else "Falta permiso de micrófono"
            return@LaunchedEffect
        }
        if (!modelFile.exists()) {
            status = "Falta el modelo en:\n${modelFile.absolutePath}"
            return@LaunchedEffect
        }
        status = "Cargando modelo…"
        val ok = withContext(Dispatchers.IO) {
            WhisperEngine.loadModel(modelFile.absolutePath)
        }
        modelReady = ok
        status = when {
            !ok -> "No pude cargar el modelo"
            !hasPermission -> "Falta permiso de micrófono"
            else -> "Listo"
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            pressed -> Color(0xFFB71C1C)
                            !hasPermission || !modelReady -> Color(0xFF9E9E9E)
                            else -> Color(0xFFE53935)
                        },
                    )
                    .pointerInput(hasPermission, modelReady, busy) {
                        detectTapGestures(
                            onPress = {
                                when {
                                    !hasPermission -> {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                    !modelReady -> {
                                        // El estado ya explica por qué; no hacemos nada al pulsar.
                                    }
                                    busy -> {
                                        // Transcripción en curso; ignoramos nuevas pulsaciones.
                                    }
                                    else -> {
                                        Log.d(TAG, "PTT down")
                                        val ok = recorder.start()
                                        if (!ok) {
                                            status = "No pude arrancar la grabación"
                                        } else {
                                            pressed = true
                                            status = "Escuchando…"
                                            try {
                                                tryAwaitRelease()
                                            } finally {
                                                pressed = false
                                                Log.d(TAG, "PTT up")
                                                busy = true
                                                status = "Procesando…"
                                                scope.launch {
                                                    val samples = recorder.stop()
                                                    if (samples.isEmpty()) {
                                                        status = "No se capturó audio"
                                                        busy = false
                                                        return@launch
                                                    }
                                                    status = "Transcribiendo… (${samples.size / AudioRecorder.SAMPLE_RATE_HZ}s)"
                                                    val text = withContext(Dispatchers.IO) {
                                                        WhisperEngine.transcribe(samples)
                                                    }
                                                    transcript = text.trim().ifEmpty { "(silencio)" }
                                                    status = "Listo"
                                                    busy = false
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (pressed) "…" else "HABLA",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Text(
                text = status,
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = transcript,
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 48.dp),
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
