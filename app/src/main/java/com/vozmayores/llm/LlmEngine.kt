package com.vozmayores.llm

import android.util.Log

private const val TAG = "Voz.LlmEngine"

object LlmEngine {
    init {
        // Ya cargada por WhisperEngine, pero System.loadLibrary es idempotente.
        System.loadLibrary("voz-native")
    }

    @Volatile
    private var ctxPtr: Long = 0L

    private external fun nativeLoadModel(path: String): Long
    private external fun nativeFreeModel(ctxPtr: Long)
    private external fun nativeGenerate(ctxPtr: Long, prompt: String, maxTokens: Int): String

    val isLoaded: Boolean get() = ctxPtr != 0L

    fun loadModel(path: String): Boolean {
        if (ctxPtr != 0L) return true
        val p = nativeLoadModel(path)
        if (p == 0L) {
            Log.e(TAG, "nativeLoadModel devolvió 0 para $path")
            return false
        }
        ctxPtr = p
        Log.d(TAG, "modelo LLM cargado ($path)")
        return true
    }

    fun free() {
        val p = ctxPtr
        if (p != 0L) {
            ctxPtr = 0L
            nativeFreeModel(p)
        }
    }

    fun generate(prompt: String, maxTokens: Int = 128): String {
        val p = ctxPtr
        if (p == 0L) return ""
        if (prompt.isEmpty() || maxTokens <= 0) return ""
        return nativeGenerate(p, prompt, maxTokens)
    }
}
