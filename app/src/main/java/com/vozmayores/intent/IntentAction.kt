package com.vozmayores.intent

sealed class IntentAction {
    data class Call(val contact: String) : IntentAction()
    data class WhatsApp(val contact: String, val message: String) : IntentAction()
    data class Sms(val contact: String, val message: String) : IntentAction()
    data class Alarm(val hour: Int, val minute: Int, val label: String? = null) : IntentAction()
    data class Respond(val text: String) : IntentAction()
    data object Unknown : IntentAction()
}

fun IntentAction.pretty(): String = when (this) {
    is IntentAction.Call     -> "Llamar a $contact"
    is IntentAction.WhatsApp -> "WhatsApp a $contact: \"$message\""
    is IntentAction.Sms      -> "SMS a $contact: \"$message\""
    is IntentAction.Alarm    -> "Alarma a las %02d:%02d".format(hour, minute)
    is IntentAction.Respond  -> text
    IntentAction.Unknown     -> "No he entendido el comando"
}
