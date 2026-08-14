package com.vozmayores.intent

import android.util.Log
import com.vozmayores.llm.AgentLoop
import com.vozmayores.llm.LlmEngine

private const val TAG = "Voz.IntentRouter"

enum class IntentSource { LOCAL, LLM, NONE }

/**
 * Resultado del router con metadatos para depurar en la UI:
 *  - source: qué componente decidió la acción.
 *  - llmRaw: si fue LLM, el texto exacto que emitió Qwen3.
 */
data class RouteResult(
    val action: IntentAction,
    val source: IntentSource,
    val llmRaw: String? = null,
)

class IntentRouter(
    private val matcher: LocalMatcher = LocalMatcher(),
    private val agent: AgentLoop? = null,
) {
    fun route(text: String, contacts: List<String> = emptyList()): RouteResult {
        val local = matcher.match(text)
        if (local != null) {
            Log.d(TAG, "match local -> $local")
            return RouteResult(local, IntentSource.LOCAL)
        }
        if (agent != null && LlmEngine.isLoaded) {
            Log.d(TAG, "sin match local, delegando al LLM")
            val r = agent.routeVerbose(text, contacts)
            return RouteResult(r.action, IntentSource.LLM, r.raw)
        }
        Log.d(TAG, "sin match y LLM no disponible")
        return RouteResult(IntentAction.Unknown, IntentSource.NONE)
    }
}
