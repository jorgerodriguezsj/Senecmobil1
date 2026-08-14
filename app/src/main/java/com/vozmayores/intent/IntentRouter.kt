package com.vozmayores.intent

import android.util.Log

private const val TAG = "Voz.IntentRouter"

/**
 * Fase 5: solo local. Cuando llegue la Fase 8, el `null` del
 * LocalMatcher pasará al LlmAgent y este devolverá otra IntentAction
 * (o Respond si no hay acción). De momento devolvemos Unknown.
 */
class IntentRouter(
    private val matcher: LocalMatcher = LocalMatcher(),
) {
    fun route(text: String): IntentAction {
        val local = matcher.match(text)
        if (local != null) {
            Log.d(TAG, "match local -> $local")
            return local
        }
        Log.d(TAG, "sin match local (aquí llamará el LLM en Fase 8)")
        return IntentAction.Unknown
    }
}
