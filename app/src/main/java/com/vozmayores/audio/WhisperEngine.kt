package com.vozmayores.audio

import android.util.Log

private const val TAG = "Voz.WhisperEngine"

object WhisperEngine {
    init {
        System.loadLibrary("voz-native")
    }

    @Volatile
    private var ctxPtr: Long = 0L

    external fun nativeSystemInfo(): String
    private external fun nativeLoadModel(path: String): Long
    private external fun nativeFreeModel(ctxPtr: Long)
    private external fun nativeTranscribe(ctxPtr: Long, samples: ShortArray, language: String): String

    val isLoaded: Boolean get() = ctxPtr != 0L

    fun loadModel(path: String): Boolean {
        if (ctxPtr != 0L) return true
        val p = nativeLoadModel(path)
        if (p == 0L) {
            Log.e(TAG, "nativeLoadModel devolvió 0 para $path")
            return false
        }
        ctxPtr = p
        Log.d(TAG, "modelo cargado ($path)")
        return true
    }

    fun free() {
        val p = ctxPtr
        if (p != 0L) {
            ctxPtr = 0L
            nativeFreeModel(p)
        }
    }

    fun transcribe(samples: ShortArray, language: String = "es"): String {
        val p = ctxPtr
        if (p == 0L) return ""
        if (samples.isEmpty()) return ""
        return nativeTranscribe(p, samples, language)
    }
}
