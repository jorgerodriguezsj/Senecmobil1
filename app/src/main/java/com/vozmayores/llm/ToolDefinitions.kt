package com.vozmayores.llm

/**
 * Bloque de herramientas que se inyecta en el system prompt del LLM.
 * Se documenta aquí (en Kotlin) para que el catálogo esté versionado
 * junto al resto del código y no se descuadre con [AgentLoop].
 *
 * Formato de respuesta exigido: un único objeto JSON en la forma
 * {"tool": "<nombre>", "args": {...}}
 * sin markdown, sin explicación previa ni posterior.
 */
object ToolDefinitions {
    val TOOLS_DOC = """
Herramientas:
- llamar(contacto)                  llama por teléfono
- whatsapp(contacto, mensaje)       manda WhatsApp
- sms(contacto, mensaje)            manda SMS
- alarma(hora, etiqueta?)           hora en formato HH:MM (24h)
- responder(texto)                  cuando no hay acción, solo respuesta hablada
""".trim()
}
