package com.vozmayores.audio

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "Voz.ModelInstaller"

object ModelInstaller {
    const val MODEL_FILE_NAME = "ggml-base-q5_1.bin"
    private const val ASSET_PATH = "models/$MODEL_FILE_NAME"
    private const val COPY_BUFFER_BYTES = 1 shl 20 // 1 MiB

    /**
     * Copia el modelo bundled en assets/ a filesDir la primera vez y
     * devuelve el fichero destino. Si ya está copiado, devuelve el
     * fichero existente sin tocar disco.
     *
     * Lanza si el asset no existe (build sin modelo) o si la copia falla
     * a media escritura (borra el destino parcial).
     */
    fun ensureInstalled(context: Context): File {
        val out = File(context.filesDir, MODEL_FILE_NAME)
        if (out.exists() && out.length() > 0) {
            Log.d(TAG, "modelo ya instalado (${out.length()} bytes)")
            return out
        }
        Log.d(TAG, "copiando modelo desde assets a ${out.absolutePath}")
        try {
            context.assets.open(ASSET_PATH).use { input ->
                out.outputStream().use { output ->
                    input.copyTo(output, COPY_BUFFER_BYTES)
                }
            }
        } catch (t: Throwable) {
            out.delete()
            Log.e(TAG, "fallo copiando el modelo", t)
            throw t
        }
        Log.d(TAG, "modelo listo (${out.length()} bytes)")
        return out
    }

    fun isAssetPresent(context: Context): Boolean = try {
        context.assets.open(ASSET_PATH).close()
        true
    } catch (_: Throwable) {
        false
    }
}
