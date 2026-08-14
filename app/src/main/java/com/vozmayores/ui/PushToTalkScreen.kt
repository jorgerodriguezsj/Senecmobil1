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
import androidx.compose.runtime.produceState
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
import com.vozmayores.actions.ContactResolver
import com.vozmayores.audio.AudioRecorder
import com.vozmayores.audio.ModelInstaller
import com.vozmayores.audio.WhisperEngine
import com.vozmayores.intent.IntentAction
import com.vozmayores.intent.IntentRouter
import com.vozmayores.intent.pretty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Voz.PushToTalkScreen"
private const val MAX_CONTACTS_IN_PROMPT = 60

@Composable
fun PushToTalkScreen() {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder() }
    val router = remember { IntentRouter() }
    val contactResolver = remember { ContactResolver(context) }
    val scope = rememberCoroutineScope()

    fun hasPerm(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var hasRecord by remember { mutableStateOf(hasPerm(Manifest.permission.RECORD_AUDIO)) }
    var hasContacts by remember { mutableStateOf(hasPerm(Manifest.permission.READ_CONTACTS)) }

    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasRecord = results[Manifest.permission.RECORD_AUDIO] ?: hasRecord
        hasContacts = results[Manifest.permission.READ_CONTACTS] ?: hasContacts
        if (!hasRecord) Log.w(TAG, "RECORD_AUDIO denegado")
    }

    val contactNames by produceState(initialValue = emptyList<String>(), hasContacts) {
        value = if (hasContacts) {
            withContext(Dispatchers.IO) {
                runCatching { contactResolver.getAllNames().take(MAX_CONTACTS_IN_PROMPT) }
                    .getOrElse { emptyList() }
            }
        } else emptyList()
    }

    val initialPrompt = remember(contactNames) {
        if (contactNames.isEmpty()) ""
        else "Contactos: ${contactNames.joinToString(", ")}."
    }

    var modelReady by remember { mutableStateOf(WhisperEngine.isLoaded) }
    var pressed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var intentText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (WhisperEngine.isLoaded) {
            modelReady = true
        } else {
            if (!ModelInstaller.isAssetPresent(context)) {
                status = "El APK no incluye el modelo Whisper"
                return@LaunchedEffect
            }
            status = "Preparando modelo…"
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val f = ModelInstaller.ensureInstalled(context)
                    WhisperEngine.loadModel(f.absolutePath)
                }.getOrElse {
                    Log.e(TAG, "no pude preparar el modelo", it)
                    false
                }
            }
            modelReady = ok
            if (!ok) {
                status = "No pude cargar el modelo"
                return@LaunchedEffect
            }
        }
        status = when {
            !hasRecord -> "Toca el botón para dar permiso de micrófono"
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
                            !modelReady -> Color(0xFF9E9E9E)
                            else -> Color(0xFFE53935)
                        },
                    )
                    .pointerInput(hasRecord, hasContacts, modelReady, busy, initialPrompt) {
                        detectTapGestures(
                            onPress = {
                                when {
                                    !hasRecord || !hasContacts -> {
                                        permsLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.RECORD_AUDIO,
                                                Manifest.permission.READ_CONTACTS,
                                            ),
                                        )
                                    }
                                    !modelReady -> Unit
                                    busy -> Unit
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
                                                    val secs = samples.size / AudioRecorder.SAMPLE_RATE_HZ
                                                    status = "Transcribiendo… (${secs}s)"
                                                    val text = withContext(Dispatchers.IO) {
                                                        WhisperEngine.transcribe(
                                                            samples = samples,
                                                            language = "es",
                                                            initialPrompt = initialPrompt,
                                                        )
                                                    }
                                                    transcript = text.trim().ifEmpty { "(silencio)" }
                                                    val intent = router.route(transcript)
                                                    val resolvedPhone = when (intent) {
                                                        is IntentAction.Call     -> contactResolver.resolvePhoneNumber(intent.contact)
                                                        is IntentAction.WhatsApp -> contactResolver.resolvePhoneNumber(intent.contact)
                                                        is IntentAction.Sms      -> contactResolver.resolvePhoneNumber(intent.contact)
                                                        else -> null
                                                    }
                                                    intentText = buildString {
                                                        append(intent.pretty())
                                                        if (resolvedPhone != null) {
                                                            append("\nTeléfono: $resolvedPhone")
                                                        } else if (
                                                            intent is IntentAction.Call ||
                                                            intent is IntentAction.WhatsApp ||
                                                            intent is IntentAction.Sms
                                                        ) {
                                                            append("\nSin contacto que case")
                                                        }
                                                    }
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
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp),
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = intentText,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 48.dp),
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
