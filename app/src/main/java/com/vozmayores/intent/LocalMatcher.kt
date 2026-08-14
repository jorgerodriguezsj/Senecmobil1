package com.vozmayores.intent

/**
 * Reglas rápidas y baratas para los comandos más frecuentes. Si nada
 * casa, devolvemos null y el IntentRouter le pasa la pelota al LLM.
 *
 * Todas las regex asumen texto en minúsculas y trim (ver [normalize]).
 * Los grupos de "diciendo/que" toleran las dos formas de dictado
 * más habituales en español.
 */
class LocalMatcher {

    fun match(text: String): IntentAction? {
        val t = normalize(text)
        if (t.isEmpty()) return null

        callRegex.find(t)?.let {
            return IntentAction.Call(it.groupValues[1].trim())
        }
        whatsAppRegex.find(t)?.let {
            return IntentAction.WhatsApp(
                contact = it.groupValues[1].trim(),
                message = it.groupValues[2].trim(),
            )
        }
        smsRegex.find(t)?.let {
            return IntentAction.Sms(
                contact = it.groupValues[1].trim(),
                message = it.groupValues[2].trim(),
            )
        }
        alarmRegex.find(t)?.let {
            val h = it.groupValues[1].toIntOrNull() ?: return null
            val m = it.groupValues[2].toIntOrNull() ?: 0
            if (h !in 0..23 || m !in 0..59) return null
            return IntentAction.Alarm(hour = h, minute = m)
        }
        return null
    }

    private fun normalize(text: String): String = text
        .trim()
        .lowercase()
        .replace(Regex("[¿¡?!\\.,]"), "")
        .replace(Regex("\\s+"), " ")

    companion object {
        // "llama a X" / "llamar a X" / "llámale a X" (con o sin tilde).
        private val callRegex = Regex(
            """^ll[aá]m(?:a|ale|ar|al[eé]|ame)? a (.+)$""",
        )

        // "wasap a X diciendo Y" / "manda un wasap a X diciendo Y" /
        // "whatsapp a X que Y". Acepta wasap/wásap/guasap/whatsapp.
        private val whatsAppRegex = Regex(
            """^(?:m[aá]nda(?:le)? un )?(?:g?w?h?[aá]s?ap|whatsapp) a (.+?) (?:diciendo|que) (.+)$""",
        )

        // "sms a X diciendo Y" / "manda un sms a X diciendo Y" /
        // "manda un mensaje a X diciendo Y".
        private val smsRegex = Regex(
            """^(?:m[aá]nda(?:le)? un )?(?:sms|mensaje) a (.+?) (?:diciendo|que) (.+)$""",
        )

        // "pon alarma a las HH" / "pon una alarma a las HH:MM" /
        // "pon una alarma a las HH y MM".
        private val alarmRegex = Regex(
            """^p[oó]n(?:me)? (?:una )?alarma a las (\d{1,2})(?:[: ]y? ?(\d{1,2}))?$""",
        )
    }
}
