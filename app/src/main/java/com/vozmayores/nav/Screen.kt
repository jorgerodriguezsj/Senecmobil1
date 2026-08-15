package com.vozmayores.nav

/**
 * Pantallas del "mini-OS" que vive dentro del dispositivo retro.
 * Cada una tiene su propio composable en `com.vozmayores.screens`
 * y se puede abrir por voz con la tool `abrir(pantalla)`.
 */
enum class Screen(
    val title: String,
    val emoji: String,
    /** Palabras clave para el reconocimiento de voz. */
    val aliases: List<String>,
) {
    Home("Inicio", "🏠", listOf("inicio", "home", "principal", "menu")),
    Contacts("Contactos", "📞", listOf("contactos", "agenda", "gente")),
    Messages("Mensajes", "✉️", listOf("mensajes", "sms", "whatsapp", "wasap", "mensajeria")),
    Alarms("Alarmas", "⏰", listOf("alarmas", "despertador", "avisos")),
    Help("Ayuda", "❓", listOf("ayuda", "help", "como se usa", "instrucciones")),
    ;

    companion object {
        /** Mapea un string libre (ya normalizado) a una Screen. */
        fun fromString(raw: String): Screen? {
            val q = raw.trim().lowercase()
            if (q.isBlank()) return null
            entries.forEach { s ->
                if (s.aliases.any { it == q }) return s
            }
            // Prefix match: "contacto" → Contacts
            entries.forEach { s ->
                if (s.aliases.any { q.contains(it) || it.contains(q) }) return s
            }
            return null
        }
    }
}
