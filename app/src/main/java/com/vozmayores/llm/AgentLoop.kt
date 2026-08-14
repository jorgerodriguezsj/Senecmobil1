package com.vozmayores.llm

import android.util.Log
import com.vozmayores.intent.IntentAction
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "Voz.AgentLoop"

/**
 * Fallback del [com.vozmayores.intent.IntentRouter] cuando el
 * matcher local no reconoce la frase. Envuelve al [LlmEngine] con:
 *
 *  1) construcción del prompt ChatML de Qwen3 con las herramientas,
 *     la lista de contactos y unos pocos ejemplos few-shot;
 *  2) parser JSON tolerante que ignora texto pre/post y admite
 *     claves en español y en inglés (por si el LLM decide que "hoy
 *     hablo en inglés");
 *  3) mapeo del tool JSON a un [IntentAction].
 *
 * No mantiene contexto entre llamadas — cada frase es una petición
 * independiente. La KV cache se limpia dentro del JNI en cada
 * `LlmEngine.generate`.
 */
class AgentLoop {

    fun route(userInput: String, contacts: List<String>): IntentAction {
        val prompt = buildPrompt(userInput, contacts)
        Log.d(TAG, "generando para: '$userInput' (${contacts.size} contactos)")
        val raw = LlmEngine.generate(prompt, maxTokens = 128)
        Log.d(TAG, "raw: '${raw.take(200)}${if (raw.length > 200) "…" else ""}'")
        return parse(raw) ?: IntentAction.Unknown
    }

    fun buildPrompt(userInput: String, contacts: List<String>): String {
        val contactList = contacts.take(30).joinToString(", ").ifBlank { "(sin contactos)" }
        return buildString {
            append("<|im_start|>system\n")
            append(
                "Eres el asistente de voz de un móvil de una persona mayor. " +
                    "Traduces lo que dice en un objeto JSON que llama a una " +
                    "herramienta. RESPONDE SOLO CON EL JSON, sin explicaciones, " +
                    "sin markdown, sin razonar en voz alta.\n\n",
            )
            append(ToolDefinitions.TOOLS_DOC)
            append("\n\nContactos disponibles: ")
            append(contactList)
            append("\n\nEjemplos:\n")
            append("Usuario: llama a pepe\n")
            append("{\"tool\":\"llamar\",\"args\":{\"contacto\":\"Pepe\"}}\n\n")
            append("Usuario: mándale un wasap a maría diciendo llego tarde\n")
            append("{\"tool\":\"whatsapp\",\"args\":{\"contacto\":\"María\",\"mensaje\":\"llego tarde\"}}\n\n")
            append("Usuario: pon una alarma para mañana a las ocho y media\n")
            append("{\"tool\":\"alarma\",\"args\":{\"hora\":\"08:30\",\"etiqueta\":\"mañana\"}}\n\n")
            append("Usuario: qué hora es\n")
            append("{\"tool\":\"responder\",\"args\":{\"texto\":\"No lo sé, mira arriba del móvil.\"}}\n")
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userInput)
            append("\n<|im_end|>\n")
            append("<|im_start|>assistant\n")
        }
    }

    fun parse(raw: String): IntentAction? {
        val jsonStr = extractJson(raw) ?: run {
            Log.w(TAG, "no encuentro JSON en la respuesta")
            return null
        }
        return try {
            val obj = JSONObject(jsonStr)
            val tool = obj.optString("tool").takeIf { it.isNotBlank() }
                ?: obj.optString("name").takeIf { it.isNotBlank() }
                ?: run {
                    Log.w(TAG, "sin campo 'tool' en JSON")
                    return null
                }
            val args = obj.optJSONObject("args")
                ?: obj.optJSONObject("arguments")
                ?: obj.optJSONObject("parameters")
                ?: JSONObject()
            actionFor(tool, args)
        } catch (e: JSONException) {
            Log.w(TAG, "JSON invalido: ${e.message} :: '$jsonStr'")
            null
        } catch (t: Throwable) {
            Log.w(TAG, "parse fallo", t)
            null
        }
    }

    private fun actionFor(tool: String, args: JSONObject): IntentAction? {
        val t = tool.lowercase().trim()
        return when (t) {
            "llamar", "call", "llamada", "phone" -> {
                val contact = strArg(args, "contacto", "contact", "name")
                if (contact.isNullOrBlank()) null
                else IntentAction.Call(contact.trim())
            }

            "whatsapp", "wasap", "wa" -> {
                val contact = strArg(args, "contacto", "contact", "name")
                val msg = strArg(args, "mensaje", "message", "text")
                if (contact.isNullOrBlank() || msg.isNullOrBlank()) null
                else IntentAction.WhatsApp(contact.trim(), msg.trim())
            }

            "sms", "mensaje", "message" -> {
                val contact = strArg(args, "contacto", "contact", "name")
                val msg = strArg(args, "mensaje", "message", "text")
                if (contact.isNullOrBlank() || msg.isNullOrBlank()) null
                else IntentAction.Sms(contact.trim(), msg.trim())
            }

            "alarma", "alarm", "timer" -> {
                val time = strArg(args, "hora", "time", "hora_minuto") ?: return null
                val parts = time.split(":", ".", limit = 2)
                val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
                val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
                if (h !in 0..23 || m !in 0..59) return null
                val label = strArg(args, "etiqueta", "label", "nota", "note")
                    ?.trim()?.takeIf { it.isNotBlank() }
                IntentAction.Alarm(h, m, label)
            }

            "responder", "respond", "reply", "speak", "say" -> {
                val text = strArg(args, "texto", "text", "mensaje", "response")
                if (text.isNullOrBlank()) null
                else IntentAction.Respond(text.trim())
            }

            else -> {
                Log.w(TAG, "tool desconocido: '$t'")
                null
            }
        }
    }

    private fun strArg(obj: JSONObject, vararg keys: String): String? {
        for (k in keys) {
            val v = obj.opt(k) ?: continue
            when (v) {
                is String -> if (v.isNotBlank()) return v
                is Number -> return v.toString()
                is Boolean -> return v.toString()
            }
        }
        return null
    }

    private fun extractJson(raw: String): String? {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('{')
        if (start < 0) return null
        // Descuenta llaves con conteo de profundidad ignorando las que
        // aparecen dentro de strings JSON.
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until trimmed.length) {
            val c = trimmed[i]
            when {
                escape -> escape = false
                c == '\\' -> escape = true
                c == '"' && !inString -> inString = true
                c == '"' && inString -> inString = false
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return trimmed.substring(start, i + 1)
                }
            }
        }
        // JSON abierto pero sin cerrar → devuelve lo que hay y que
        // JSONObject decida si lo salva o no.
        return trimmed.substring(start)
    }
}
