package com.vozmayores.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.vozmayores.actions.ContactResolver
import com.vozmayores.actions.ToolExecutor
import com.vozmayores.actions.Tts
import com.vozmayores.audio.AudioRecorder
import com.vozmayores.audio.ModelInstaller
import com.vozmayores.audio.WhisperEngine
import com.vozmayores.intent.IntentAction
import com.vozmayores.intent.IntentRouter
import com.vozmayores.intent.IntentSource
import com.vozmayores.intent.LocalMatcher
import com.vozmayores.llm.AgentLoop
import com.vozmayores.llm.LlmEngine
import com.vozmayores.nav.AppNav
import com.vozmayores.nav.Screen
import com.vozmayores.screens.AlarmsScreen
import com.vozmayores.screens.ContactsScreen
import com.vozmayores.screens.HelpScreen
import com.vozmayores.screens.HomeScreen
import com.vozmayores.screens.MessagesScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "Voz.PushToTalkScreen"
private const val MAX_CONTACTS_IN_PROMPT = 60

private val REQUESTED_PERMISSIONS = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.CALL_PHONE,
    Manifest.permission.SEND_SMS,
)

@Composable
fun PushToTalkScreen() {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder() }
    val router = remember { IntentRouter(matcher = LocalMatcher(), agent = AgentLoop()) }
    val contactResolver = remember { ContactResolver(context) }
    val tts = remember { Tts(context) }
    val nav = remember { AppNav() }
    val executor = remember { ToolExecutor(context, tts, contactResolver, nav) }
    val scope = rememberCoroutineScope()
    val haptics = remember { HapticFeedback(context) }
    val level by recorder.level.collectAsState()

    // Botón atrás de Android: si estamos dentro de una pantalla,
    // vuelve al Home; si estamos en Home dejamos que Android cierre.
    BackHandler(enabled = !nav.isAtHome) { nav.home() }

    DisposableEffect(tts) { onDispose { tts.shutdown() } }

    fun granted(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    var hasRecord by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
    var hasContacts by remember { mutableStateOf(granted(Manifest.permission.READ_CONTACTS)) }
    var hasCall by remember { mutableStateOf(granted(Manifest.permission.CALL_PHONE)) }
    var hasSms by remember { mutableStateOf(granted(Manifest.permission.SEND_SMS)) }

    val permsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        hasRecord = results[Manifest.permission.RECORD_AUDIO] ?: hasRecord
        hasContacts = results[Manifest.permission.READ_CONTACTS] ?: hasContacts
        hasCall = results[Manifest.permission.CALL_PHONE] ?: hasCall
        hasSms = results[Manifest.permission.SEND_SMS] ?: hasSms
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
        val verbs = "Llamar. Mandar. Escribir un WhatsApp. WhatsApp. Mandar un SMS. " +
            "Poner una alarma. Programar una alarma."
        val contactsPart = if (contactNames.isEmpty()) ""
        else " Contactos: ${contactNames.joinToString(", ")}."
        "$verbs$contactsPart"
    }

    var modelReady by remember { mutableStateOf(WhisperEngine.isLoaded) }
    var llmReady by remember { mutableStateOf(LlmEngine.isLoaded) }
    var pressed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<List<HistoryEntry>>(emptyList()) }
    var simulate by remember { mutableStateOf(true) }

    fun fireIntent(action: IntentAction) {
        scope.launch {
            busy = true
            status = if (simulate) "Simulando…" else "Ejecutando…"
            val msg = executor.execute(action, simulate)
            record(action, simulate, msg, IntentSource.LOCAL)
            status = "Listo"
            busy = false
        }
    }

    suspend fun record(
        intent: IntentAction,
        simulated: Boolean,
        message: String,
        source: IntentSource = IntentSource.NONE,
        llmRaw: String? = null,
    ) {
        val phone = withContext(Dispatchers.IO) {
            when (intent) {
                is IntentAction.Call     -> contactResolver.resolvePhoneNumber(intent.contact)
                is IntentAction.WhatsApp -> contactResolver.resolvePhoneNumber(intent.contact)
                is IntentAction.Sms      -> contactResolver.resolvePhoneNumber(intent.contact)
                else -> null
            }
        }
        history = (listOf(HistoryEntry(intent, phone, simulated, message, source, llmRaw)) + history).take(5)
    }

    LaunchedEffect(Unit) {
        if (!WhisperEngine.isLoaded) {
            if (!ModelInstaller.isWhisperAssetPresent(context)) {
                status = "El APK no incluye el modelo Whisper"
                return@LaunchedEffect
            }
            status = "Preparando modelo de voz…"
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val f = ModelInstaller.ensureWhisperInstalled(context)
                    WhisperEngine.loadModel(f.absolutePath)
                }.getOrElse {
                    Log.e(TAG, "no pude preparar Whisper", it)
                    false
                }
            }
            modelReady = ok
            if (!ok) {
                status = "No pude cargar el modelo de voz"
                return@LaunchedEffect
            }
        } else {
            modelReady = true
        }
        status = if (hasRecord) "Listo" else "Toca abajo para dar permisos"

        if (!LlmEngine.isLoaded && ModelInstaller.isLlmAssetPresent(context)) {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val f = ModelInstaller.ensureLlmInstalled(context)
                    LlmEngine.loadModel(f.absolutePath)
                }.getOrElse {
                    Log.e(TAG, "no pude preparar LLM", it)
                    false
                }
            }
            llmReady = ok
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            RetroDevice(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DeviceTopStrip(modifier = Modifier.height(46.dp))
                    Spacer(modifier = Modifier.height(10.dp))

                    InnerScreen(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (nav.current) {
                            Screen.Home -> HomeScreen(
                                onOpen = { nav.open(it) },
                                simulate = simulate,
                                onSimulateChange = { simulate = it },
                                settingsEnabled = !busy,
                                probeEnabled = !busy && modelReady,
                                onProbeClick = {
                                    scope.launch {
                                        busy = true
                                        val sampleName = contactNames.firstOrNull() ?: "Pepe"
                                        val battery = listOf(
                                            IntentAction.Call(sampleName),
                                            IntentAction.WhatsApp(sampleName, "hola, esto es una prueba"),
                                            IntentAction.Sms(sampleName, "hola, esto es una prueba"),
                                            IntentAction.Alarm(hour = 8, minute = 30),
                                        )
                                        transcript = "(probar todo)"
                                        battery.forEachIndexed { i, act ->
                                            status = "Probando ${i + 1}/${battery.size}…"
                                            val msg = executor.execute(act, simulate = true)
                                            record(act, simulated = true, message = msg)
                                        }
                                        status = "Listo"
                                        busy = false
                                    }
                                },
                                status = status,
                                transcript = transcript,
                                lastEntry = history.firstOrNull(),
                            )
                            Screen.Contacts -> ContactsScreen(
                                contactResolver = contactResolver,
                                onBack = { nav.home() },
                                onCall = { name -> fireIntent(IntentAction.Call(name)) },
                                onWhatsApp = { name -> fireIntent(IntentAction.WhatsApp(name, "")) },
                            )
                            Screen.Messages -> MessagesScreen(onBack = { nav.home() })
                            Screen.Alarms -> AlarmsScreen(onBack = { nav.home() })
                            Screen.Help -> HelpScreen(onBack = { nav.home() })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PttWavePanel(
                        pressed = pressed,
                        busy = busy,
                        enabled = modelReady && hasRecord,
                        level = level,
                        onPress = {
                            when {
                                !hasRecord -> permsLauncher.launch(REQUESTED_PERMISSIONS)
                                !modelReady -> Unit
                                busy -> Unit
                                else -> {
                                    Log.d(TAG, "PTT down")
                                    val ok = recorder.start()
                                    if (!ok) {
                                        status = "No pude arrancar la grabación"
                                    } else {
                                        pressed = true
                                        haptics.buzz(30)
                                        status = "Escuchando…"
                                        try {
                                            tryAwaitRelease()
                                        } finally {
                                            pressed = false
                                            haptics.buzz(60)
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
                                                status = if (llmReady) "Interpretando…" else "Buscando comando…"
                                                val routed = withContext(Dispatchers.IO) {
                                                    router.route(transcript, contactNames)
                                                }
                                                status = if (simulate) "Simulando…" else "Ejecutando…"
                                                val msg = executor.execute(routed.action, simulate)
                                                record(routed.action, simulate, msg, routed.source, routed.llmRaw)
                                                status = "Listo"
                                                busy = false
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.height(150.dp),
                    )
                }
            }
        }
    }
}

private class HapticFeedback(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun buzz(durationMs: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
