package com.vozmayores.llm

import android.util.Log
import com.vozmayores.intent.IntentAction
import com.vozmayores.nav.Screen
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "Voz.AgentLoop"

/**
 * Resultado de una interpretación por el LLM. `raw` contiene el
 * texto exacto que devolvió el modelo antes del parseo — se muestra
 * en la UI para poder ver por qué falla una interpretación cuando
 * el LLM devuelve algo inesperado.
 */
data class AgentResult(val action: IntentAction, val raw: String)

class AgentLoop {

    fun routeVerbose(userInput: String, contacts: List<String>): AgentResult {
        val prompt = buildPrompt(userInput, contacts)
        Log.d(TAG, "generando para: '$userInput' (${contacts.size} contactos)")
        val raw = LlmEngine.generate(prompt, maxTokens = 96)
        Log.d(TAG, "raw: '${raw.take(300)}${if (raw.length > 300) "…" else ""}'")
        val action = parse(raw) ?: IntentAction.Unknown
        return AgentResult(action, raw)
    }

    fun route(userInput: String, contacts: List<String>): IntentAction =
        routeVerbose(userInput, contacts).action

    fun buildPrompt(userInput: String, contacts: List<String>): String {
        val contactList = contacts.take(30).joinToString(", ").ifBlank { "(sin contactos)" }
        // Qwen3 emite <think>…</think> por defecto. /no_think en el
        // user turn lo desactiva. Además, prompt más corto y directo:
        // menos ejemplos, más énfasis en "solo JSON en una línea".
        return buildString {
            append("<|im_start|>system\n")
            append(
                "Traduce lo que dice el usuario a un objeto JSON de una sola línea " +
                    "que llama a UNA herramienta. Solo JSON, nada más.\n\n",
            )
            append(ToolDefinitions.TOOLS_DOC)
            append("\n\nContactos: ")
            append(contactList)
            append("\n\nEjemplos:\n")
            append("llama a pepe → {\"tool\":\"llamar\",\"args\":{\"contacto\":\"Pepe\"}}\n")
            append("wasap a maria diciendo hola → {\"tool\":\"whatsapp\",\"args\":{\"contacto\":\"María\",\"mensaje\":\"hola\"}}\n")
            append("alarma a las 8 → {\"tool\":\"alarma\",\"args\":{\"hora\":\"08:00\"}}\n")
            append("abre contactos → {\"tool\":\"abrir\",\"args\":{\"pantalla\":\"contactos\"}}\n")
            append("vuelve al inicio → {\"tool\":\"volver\",\"args\":{}}\n")
            append("no sé qué decirte → {\"tool\":\"responder\",\"args\":{\"texto\":\"Vale.\"}}\n")
            append("<|im_end|>\n")
            append("<|im_start|>user\n")
            append(userInput.trim())
            append(" /no_think\n<|im_end|>\n")
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

            "abrir", "abre", "open", "navegar", "ir" -> {
                val target = strArg(args, "pantalla", "screen", "app", "seccion", "section")
                if (target.isNullOrBlank()) null
                else {
                    val s = Screen.fromString(target)
                    when {
                        s == null -> null
                        s == Screen.Home -> IntentAction.Back
                        else -> IntentAction.Open(s)
                    }
                }
            }

            "volver", "back", "atras", "home", "inicio" -> IntentAction.Back

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
        // Tira cualquier <think>…</think> que Qwen emita por delante.
        val cleaned = raw.replace(Regex("<think>[\\s\\S]*?</think>", RegexOption.IGNORE_CASE), "").trim()
        val start = cleaned.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until cleaned.length) {
            val c = cleaned[i]
            when {
                escape -> escape = false
                c == '\\' -> escape = true
                c == '"' && !inString -> inString = true
                c == '"' && inString -> inString = false
                !inString && c == '{' -> depth++
                !inString && c == '}' -> {
                    depth--
                    if (depth == 0) return cleaned.substring(start, i + 1)
                }
            }
        }
        return cleaned.substring(start)
    }
}
