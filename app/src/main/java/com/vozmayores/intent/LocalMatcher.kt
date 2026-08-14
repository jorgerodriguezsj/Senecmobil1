package com.vozmayores.intent

/**
 * Matcher regex ampliado. Todo el texto pasa por [normalize] que
 * quita puntuacion y tildes y baja a minusculas, asi las regex son
 * ASCII y no tenemos que meter clases de caracteres por cada vocal.
 *
 * Cada tipo de accion tiene varias regex candidatas y se prueba en
 * orden. Toleran prefijos educados ("por favor", "puedes", "quiero
 * que", "podrias") y varios sinonimos de verbo.
 *
 * Las horas de la alarma se aceptan en cifras (7, 07, 07:30) y en
 * palabras ("siete", "siete y media", "ocho y cuarto"). Rango 0-23.
 */
class LocalMatcher {

    fun match(text: String): IntentAction? {
        val t = normalize(text)
        if (t.isEmpty()) return null

        for (p in CALL_PATTERNS) {
            p.find(t)?.let { m ->
                val name = cleanName(m.groupValues[1])
                if (name.isNotBlank()) return IntentAction.Call(name)
            }
        }
        for (p in WHATSAPP_PATTERNS) {
            p.find(t)?.let { m ->
                val name = cleanName(m.groupValues[1])
                val msg = m.groupValues[2].trim()
                if (name.isNotBlank() && msg.isNotBlank()) {
                    return IntentAction.WhatsApp(name, msg)
                }
            }
        }
        for (p in SMS_PATTERNS) {
            p.find(t)?.let { m ->
                val name = cleanName(m.groupValues[1])
                val msg = m.groupValues[2].trim()
                if (name.isNotBlank() && msg.isNotBlank()) {
                    return IntentAction.Sms(name, msg)
                }
            }
        }
        matchAlarm(t)?.let { return it }
        return null
    }

    private fun cleanName(raw: String): String = raw
        .trim()
        .trimEnd('.', ',', '?', '!')
        .replaceFirst(Regex("^(?:mi |a )"), "")
        .trim()

    private fun matchAlarm(t: String): IntentAction.Alarm? {
        // Formato con cifras
        DIGIT_ALARM.find(t)?.let {
            val h = it.groupValues[1].toIntOrNull() ?: return@let null
            val m = it.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: 0
            if (h in 0..23 && m in 0..59) return IntentAction.Alarm(h, m)
        }
        // Formato con palabras: "alarma a las siete y media"
        WORD_ALARM.find(t)?.let { match ->
            val h = HOUR_WORDS[match.groupValues[1]] ?: return@let null
            val minStr = match.groupValues[2].trim()
            val m = if (minStr.isEmpty()) 0 else parseMinutes(minStr) ?: return@let null
            if (h in 0..23 && m in 0..59) return IntentAction.Alarm(h, m)
        }
        return null
    }

    private fun parseMinutes(s: String): Int? {
        // "media" / "cuarto" / "veinte" / "cinco" / "y cinco" / "y media"
        val stripped = s.removePrefix("y ").trim()
        return MIN_WORDS[stripped]
    }

    private fun normalize(text: String): String = text
        .trim()
        .lowercase()
        .let(::stripAccents)
        .replace(Regex("[¿¡?!\\.,;:\"']"), "")
        .replace(Regex("\\s+"), " ")

    companion object {

        fun stripAccents(s: String): String = buildString(s.length) {
            for (c in s) append(
                when (c) {
                    'á', 'à', 'â', 'ä' -> 'a'
                    'é', 'è', 'ê', 'ë' -> 'e'
                    'í', 'ì', 'î', 'ï' -> 'i'
                    'ó', 'ò', 'ô', 'ö' -> 'o'
                    'ú', 'ù', 'û', 'ü' -> 'u'
                    'ñ' -> 'n'
                    else -> c
                },
            )
        }

        // Prefijo educado opcional que puede preceder cualquier orden.
        private const val POLITE =
            "(?:(?:por favor,? ?)?" +
                "(?:puedes? |podrias? |quiero(?: que)? |quisiera(?: que)? |vale )*)?"

        private val CALL_PATTERNS = listOf(
            // "llama a Pepe", "llamale a Pepe", "llamar a Pepe"
            Regex("^${POLITE}(?:llam(?:a|ame|ale|arle|arme|ar))\\s+a\\s+(.+)$"),
            // "haz una llamada a Pepe", "hazle una llamada a Pepe"
            Regex("^${POLITE}haz(?:le|me)?\\s+(?:una\\s+)?llamada\\s+a\\s+(.+)$"),
            // "marca a Pepe" / "marcale a Pepe"
            Regex("^${POLITE}marca(?:le|me)?\\s+a\\s+(.+)$"),
            // "ponme con Pepe" / "pon con Pepe"
            Regex("^${POLITE}pon(?:me|le)?\\s+con\\s+(.+)$"),
            // "telefono a Pepe" / "telefonea a Pepe"
            Regex("^${POLITE}(?:telefonea|telefono|llamada)\\s+a\\s+(.+)$"),
        )

        // Variantes de "wasap": wasap, whasap, guasap, whatsapp, wasa, guasa
        private const val WA_APP = "(?:g?w?h?asap|whatsapp|wasa|guasa|wa)"
        private const val SEND_VERB = "(?:manda(?:le)?|envia(?:le)?|escribe(?:le)?|di(?:le)?)"
        private const val WITH_TEXT = "(?:diciendo(?:le)?|que\\s+diga|con\\s+el\\s+mensaje|que)"

        private val WHATSAPP_PATTERNS = listOf(
            // "manda un wasap a Pepe diciendo hola"
            Regex("^${POLITE}${SEND_VERB}?\\s*(?:un\\s+)?${WA_APP}\\s+a\\s+(.+?)\\s+${WITH_TEXT}\\s+(.+)$"),
            // "escribele un wasap a Pepe: hola"
            Regex("^${POLITE}${SEND_VERB}\\s+(?:un\\s+)?${WA_APP}\\s+a\\s+(.+?)\\s*:\\s*(.+)$"),
        )

        private val SMS_PATTERNS = listOf(
            Regex("^${POLITE}${SEND_VERB}?\\s*(?:un\\s+)?(?:sms|mensaje)\\s+a\\s+(.+?)\\s+${WITH_TEXT}\\s+(.+)$"),
            Regex("^${POLITE}${SEND_VERB}\\s+(?:un\\s+)?(?:sms|mensaje)\\s+a\\s+(.+?)\\s*:\\s*(.+)$"),
        )

        private val ALARM_VERB = "(?:pon(?:me|le)?|programa(?:me)?|activa(?:me)?|configura(?:me)?)?"

        private val DIGIT_ALARM = Regex(
            "^${POLITE}${ALARM_VERB}\\s*(?:una\\s+)?alarma\\s+(?:a\\s+las?\\s+)?" +
                "(\\d{1,2})(?:[: ]y? ?(\\d{1,2}))?",
        )

        // "alarma a las siete y media" / "alarma a las ocho"
        private val WORD_ALARM = Regex(
            "^${POLITE}${ALARM_VERB}\\s*(?:una\\s+)?alarma\\s+a\\s+las?\\s+" +
                "([a-z]+)((?:\\s+y\\s+[a-z]+)?)",
        )

        private val HOUR_WORDS: Map<String, Int> = mapOf(
            "una" to 1, "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
            "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9,
            "diez" to 10, "once" to 11, "doce" to 12, "trece" to 13,
            "catorce" to 14, "quince" to 15, "dieciseis" to 16,
            "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
            "veinte" to 20, "veintiuna" to 21, "veintiuno" to 21,
            "veintidos" to 22, "veintitres" to 23, "veinticuatro" to 24,
        )

        private val MIN_WORDS: Map<String, Int> = mapOf(
            "media" to 30, "cuarto" to 15,
            "cinco" to 5, "diez" to 10, "quince" to 15,
            "veinte" to 20, "veinticinco" to 25, "treinta" to 30,
            "cuarenta" to 40, "cuarenta y cinco" to 45, "cincuenta" to 50,
        )
    }
}
