package com.vozmayores.actions

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "Voz.Tts"

class Tts(context: Context) {
    private val ready = CompletableDeferred<Boolean>()
    private val engine: TextToSpeech = TextToSpeech(context.applicationContext) { code ->
        val ok = code == TextToSpeech.SUCCESS
        Log.d(TAG, "init code=$code ok=$ok")
        ready.complete(ok)
    }

    /**
     * Habla el texto en es-ES y espera al onDone. Si el TTS no está
     * disponible o falla, hace no-op sin romper el flujo del caller.
     */
    suspend fun speak(text: String) {
        if (text.isBlank()) return
        val ok = ready.await()
        if (!ok) {
            Log.w(TAG, "TTS no disponible, skip: $text")
            return
        }
        engine.language = Locale("es", "ES")
        suspendCancellableCoroutine { cont ->
            val id = UUID.randomUUID().toString()
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
                @Deprecated("Compatibilidad con la firma antigua")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
            })
            val r = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            if (r != TextToSpeech.SUCCESS && cont.isActive) {
                Log.w(TAG, "speak() rechazado, code=$r")
                cont.resume(Unit)
            }
        }
    }

    fun shutdown() {
        runCatching {
            engine.stop()
            engine.shutdown()
        }
    }
}
