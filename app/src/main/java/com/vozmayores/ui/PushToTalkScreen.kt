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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

private const val TAG = "Voz.PushToTalkScreen"

@Composable
fun PushToTalkScreen() {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder() }
    val scope = rememberCoroutineScope()

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

    var pressed by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf(if (hasPermission) "Listo" else "Falta permiso de micrófono")
    }
    val systemInfo = remember {
        runCatching { WhisperEngine.nativeSystemInfo() }.getOrElse { it.message.orEmpty() }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(if (pressed) Color(0xFFB71C1C) else Color(0xFFE53935))
                    .pointerInput(hasPermission) {
                        detectTapGestures(
                            onPress = {
                                if (!hasPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
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
                                            scope.launch {
                                                val samples = recorder.stop()
                                                val ms = samples.size * 1000L /
                                                    AudioRecorder.SAMPLE_RATE_HZ
                                                status = "Capturadas ${samples.size} muestras · $ms ms"
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
                modifier = Modifier.padding(top = 32.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = systemInfo,
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
