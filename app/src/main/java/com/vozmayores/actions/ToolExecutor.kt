package com.vozmayores.actions

import android.content.Context
import android.util.Log
import com.vozmayores.intent.IntentAction

private const val TAG = "Voz.ToolExecutor"

class ToolExecutor(
    private val context: Context,
    private val tts: Tts,
    private val contactResolver: ContactResolver,
) {
    /**
     * Anuncia por TTS la acción y la ejecuta. Devuelve un mensaje corto
     * para mostrar en la UI. En acciones sobre contactos, si no hay
     * teléfono resuelto, avisa por TTS y no ejecuta nada.
     */
    suspend fun execute(action: IntentAction): String {
        Log.d(TAG, "execute $action")
        return when (action) {
            is IntentAction.Call -> {
                val phone = contactResolver.resolvePhoneNumber(action.contact)
                if (phone == null) {
                    val msg = "No encuentro a ${action.contact} en tus contactos."
                    tts.speak(msg); msg
                } else {
                    tts.speak("Llamo a ${action.contact}.")
                    CallAction.perform(context, phone).fold(
                        onSuccess = { "Llamando a ${action.contact} ($phone)" },
                        onFailure = { fail("llamar", it) },
                    )
                }
            }
            is IntentAction.WhatsApp -> {
                val phone = contactResolver.resolvePhoneNumber(action.contact)
                if (phone == null) {
                    val msg = "No encuentro a ${action.contact} en tus contactos."
                    tts.speak(msg); msg
                } else {
                    tts.speak("Mando un WhatsApp a ${action.contact} que dice: ${action.message}.")
                    WhatsAppAction.perform(context, phone, action.message).fold(
                        onSuccess = { "WhatsApp a ${action.contact}: \"${action.message}\"" },
                        onFailure = { fail("abrir WhatsApp", it) },
                    )
                }
            }
            is IntentAction.Sms -> {
                val phone = contactResolver.resolvePhoneNumber(action.contact)
                if (phone == null) {
                    val msg = "No encuentro a ${action.contact} en tus contactos."
                    tts.speak(msg); msg
                } else {
                    tts.speak("Mando un mensaje a ${action.contact} que dice: ${action.message}.")
                    SmsAction.perform(context, phone, action.message).fold(
                        onSuccess = { "SMS enviado a ${action.contact}" },
                        onFailure = { fail("enviar el SMS", it) },
                    )
                }
            }
            is IntentAction.Alarm -> {
                val hhmm = "%02d:%02d".format(action.hour, action.minute)
                tts.speak("Pongo una alarma a las $hhmm.")
                AlarmAction.perform(context, action.hour, action.minute, action.label).fold(
                    onSuccess = { "Alarma a las $hhmm" },
                    onFailure = { fail("poner la alarma", it) },
                )
            }
            is IntentAction.Respond -> {
                tts.speak(action.text)
                action.text
            }
            IntentAction.Unknown -> {
                val msg = "No te he entendido, prueba otra vez."
                tts.speak(msg); msg
            }
        }
    }

    private suspend fun fail(what: String, t: Throwable): String {
        Log.w(TAG, "fallo al $what", t)
        val msg = "No he podido $what."
        tts.speak(msg)
        return "$msg (${t.message ?: t::class.java.simpleName})"
    }
}
