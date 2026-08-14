package com.vozmayores.audio

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "Voz.ModelInstaller"

object ModelInstaller {
    const val WHISPER_MODEL_FILE = "ggml-small-q5_1.bin"
    const val LLM_MODEL_FILE = "qwen3-0.6b-instruct-q4_k_m.gguf"

    private const val WHISPER_ASSET = "models/$WHISPER_MODEL_FILE"
    private const val LLM_ASSET = "models/$LLM_MODEL_FILE"
    private const val COPY_BUFFER_BYTES = 1 shl 20 // 1 MiB

    fun ensureWhisperInstalled(context: Context): File =
        ensureAssetInstalled(context, WHISPER_ASSET, WHISPER_MODEL_FILE)

    fun ensureLlmInstalled(context: Context): File =
        ensureAssetInstalled(context, LLM_ASSET, LLM_MODEL_FILE)

    fun isWhisperAssetPresent(context: Context): Boolean = isAssetPresent(context, WHISPER_ASSET)
    fun isLlmAssetPresent(context: Context): Boolean = isAssetPresent(context, LLM_ASSET)

    private fun ensureAssetInstalled(context: Context, assetPath: String, outName: String): File {
        val out = File(context.filesDir, outName)
        if (out.exists() && out.length() > 0) {
            Log.d(TAG, "$outName ya instalado (${out.length()} bytes)")
            return out
        }
        Log.d(TAG, "copiando $assetPath a ${out.absolutePath}")
        try {
            context.assets.open(assetPath).use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output, COPY_BUFFER_BYTES)
                }
            }
        } catch (t: Throwable) {
            out.delete()
            Log.e(TAG, "fallo copiando $assetPath", t)
            throw t
        }
        Log.d(TAG, "$outName listo (${out.length()} bytes)")
        return out
    }

    private fun isAssetPresent(context: Context, assetPath: String): Boolean = try {
        context.assets.open(assetPath).close()
        true
    } catch (_: Throwable) {
        false
    }

    // Aliases retrocompatibles con el código antiguo.
    const val MODEL_FILE_NAME: String = WHISPER_MODEL_FILE
    fun ensureInstalled(context: Context): File = ensureWhisperInstalled(context)
    fun isAssetPresent(context: Context): Boolean = isWhisperAssetPresent(context)
}
