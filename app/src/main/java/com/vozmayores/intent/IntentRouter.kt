package com.vozmayores.intent

import android.util.Log
import com.vozmayores.llm.AgentLoop
import com.vozmayores.llm.LlmEngine

private const val TAG = "Voz.IntentRouter"

/**
 * Router híbrido:
 *  1) [LocalMatcher] con regex (barato, instantáneo).
 *  2) Si no matchea, [AgentLoop] con Qwen3 vía [LlmEngine] (mucho
 *     más lento, cubre cualquier frasing). Solo se llama si el LLM
 *     está cargado.
 *  3) Si el LLM no está listo o devuelve algo no parseable,
 *     [IntentAction.Unknown].
 */
class IntentRouter(
    private val matcher: LocalMatcher = LocalMatcher(),
    private val agent: AgentLoop? = null,
) {
    fun route(text: String, contacts: List<String> = emptyList()): IntentAction {
        val local = matcher.match(text)
        if (local != null) {
            Log.d(TAG, "match local -> $local")
            return local
        }
        if (agent != null && LlmEngine.isLoaded) {
            Log.d(TAG, "sin match local, delegando al LLM")
            return agent.route(text, contacts)
        }
        Log.d(TAG, "sin match y LLM no disponible")
        return IntentAction.Unknown
    }
}
