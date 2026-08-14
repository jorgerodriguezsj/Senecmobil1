# Voz — app de voz para personas mayores

App Android nativa, on-device, con una sola pantalla y un botón enorme
de push-to-talk. Mantener pulsado → habla → soltar. Whisper transcribe,
un LLM pequeño decide qué acción del teléfono ejecutar (llamada,
WhatsApp/SMS, alarma), TTS confirma antes de las acciones destructivas.

Todo se ejecuta local. Sin red, sin cuentas, sin nube.

## Estado

- Package: `com.vozmayores`
- AppName: `Voz`
- Branch de desarrollo: `claude/android-push-to-talk-elderly-971edu`
- Fase actual: 1 (scaffold)

## Stack

- Kotlin + Jetpack Compose
- `minSdk 26`, `targetSdk 34`, `compileSdk 34`
- NDK + CMake para código nativo
- whisper.cpp como submódulo git — STT, modelo `ggml-base-q5_1.bin`, español
- llama.cpp como submódulo git — agente, `Qwen3-0.6B-Instruct Q4_K_M`
- TTS nativo de Android (`android.speech.tts.TextToSpeech`)
- Intents Android para ejecutar acciones
- ABI objetivo inicial: `arm64-v8a` únicamente

## Arquitectura

Pipeline por cada pulsación del botón:

```
Botón pulsado
  → AudioRecorder (PCM 16 kHz mono)
  → WhisperEngine.transcribe()             → texto
  → IntentRouter.route(texto)
       1) LocalMatcher: regex para comandos frecuentes
       2) Si no matchea → LlmAgent con tool calling (Qwen3)
  → ToolExecutor ejecuta el intent Android
  → TTS confirma resultado
```

## Herramientas del agente (tool calling)

Devuelve JSON `{"tool": "...", "args": {...}}`:

- `llamar(contacto: str)`
- `whatsapp(contacto: str, mensaje: str)`
- `sms(contacto: str, mensaje: str)`
- `alarma(hora: str, etiqueta: str)`
- `responder(texto: str)` — cuando no hay acción, solo respuesta hablada

## Estructura de carpetas objetivo

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt
│   ├── whisper.cpp/          (submódulo, fase 2)
│   ├── llama.cpp/            (submódulo, fase 7)
│   ├── whisper_jni.cpp
│   └── llama_jni.cpp
├── java/com/vozmayores/
│   ├── MainActivity.kt
│   ├── audio/
│   │   ├── AudioRecorder.kt
│   │   └── WhisperEngine.kt
│   ├── llm/
│   │   ├── LlmEngine.kt
│   │   ├── AgentLoop.kt
│   │   └── ToolDefinitions.kt
│   ├── intent/
│   │   ├── IntentRouter.kt
│   │   └── LocalMatcher.kt
│   ├── actions/
│   │   ├── ToolExecutor.kt
│   │   ├── CallAction.kt
│   │   ├── WhatsAppAction.kt
│   │   ├── SmsAction.kt
│   │   ├── AlarmAction.kt
│   │   └── ContactResolver.kt
│   └── ui/
│       └── PushToTalkScreen.kt
└── AndroidManifest.xml
```

## Decisiones tomadas (no reabrir)

- Sin frameworks tipo LangChain. Loop de agente propio.
- Los modelos NO se meten en el repo. Se copian con `adb push` a
  `/sdcard/Android/data/com.vozmayores/files/models/`.
- Router híbrido regex + LLM por latencia y batería (el LLM sólo si
  el regex no matchea).
- Confirmación TTS antes de llamadas y mensajes.
- ABI objetivo inicial: `arm64-v8a` únicamente. Nada de x86, armv7, etc.
- Sin red ni telemetría. Todo el pipeline es on-device.

## Plan por fases

Se avanza fase a fase. Al terminar cada una: `./gradlew assembleDebug`,
mostrar errores si los hay, ESPERAR confirmación antes de continuar.

1. **Scaffold** — Gradle KTS + NDK + Compose. Hello World con botón
   grande centrado.
2. **whisper.cpp submódulo** — CMakeLists arm64-v8a, JNI mínimo que
   devuelve `whisper_print_system_info()`.
3. **AudioRecorder** — permiso runtime `RECORD_AUDIO`, PCM 16 kHz mono
   mientras el botón está pulsado.
4. **WhisperEngine real** — modelo desde `getExternalFilesDir()`,
   transcribe buffer del recorder, muestra texto. Idioma `es` hardcoded.
5. **LocalMatcher** — regex para "llamar a X", "wasap a X diciendo Y",
   "sms a X diciendo Y", "pon alarma a las HH". `ContactResolver` usa
   `ContactsContract`.
6. **Acciones** — `CallAction` (`ACTION_CALL`), `SmsAction` (`SmsManager`),
   `WhatsAppAction` (deep link `wa.me`), `AlarmAction`
   (`AlarmClock.ACTION_SET_ALARM`). TTS de confirmación previo.
7. **llama.cpp submódulo** — JNI que carga modelo y expone
   `generate(prompt, maxTokens) -> String`.
8. **AgentLoop** — system prompt de tool calling en JSON. Fallback del
   `IntentRouter` cuando `LocalMatcher` no matchea. Parser JSON tolerante.
9. **Pulido UX** — feedback visual del botón, estados
   "escuchando"/"pensando"/"ejecutando", errores por TTS.

## Convenciones

- Todo el UI en Compose, sin XML de layouts.
- Textos hardcoded en español (por ahora, sin `strings.xml` con
  traducciones — es una app para una persona concreta).
- Logs con TAG por clase: `private const val TAG = "Voz.NombreClase"`.
- Coroutines: `Dispatchers.IO` para audio/whisper/llm, `Main` para UI.
- Nada de reflection, nada de ProGuard rules hasta que haga falta.

## Comandos habituales

```
./gradlew assembleDebug         # compila APK de debug
./gradlew installDebug          # instala en el dispositivo conectado
adb push modelo.bin /sdcard/Android/data/com.vozmayores/files/models/
adb logcat -s Voz.\*           # ver sólo nuestros logs
```

## Cosas que NO hay que hacer

- No añadir dependencias de red (Retrofit, OkHttp para producción, etc.).
- No añadir analytics, crash reporting, Firebase.
- No meter modelos en el APK ni en `assets/` — el APK explota de tamaño.
- No hacer target de otras ABIs sin discutirlo antes.
- No introducir DI frameworks. Instancias manuales por ahora.
