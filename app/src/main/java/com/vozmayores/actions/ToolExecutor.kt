package com.vozmayores.actions

import android.content.Context
import android.util.Log
import com.vozmayores.intent.IntentAction
import com.vozmayores.nav.AppNav

private const val TAG = "Voz.ToolExecutor"

class ToolExecutor(
    private val context: Context,
    private val tts: Tts,
    private val contactResolver: ContactResolver,
    private val nav: AppNav,
) {
    /**
     * Anuncia por TTS la acción y la ejecuta. Devuelve un mensaje corto
     * para mostrar en la UI. En acciones sobre contactos, si no hay
     * teléfono resuelto, avisa por TTS y no ejecuta nada.
     *
     * Con `simulate=true` anuncia lo que HARÍA sin tocar el sistema.
     * Útil para probar el flujo sin llamar a nadie ni mandar SMS reales.
     */
    suspend fun execute(action: IntentAction, simulate: Boolean = false): String {
        Log.d(TAG, "execute simulate=$simulate $action")
        return when (action) {
            is IntentAction.Call -> {
                val phone = contactResolver.resolvePhoneNumber(action.contact)
                when {
                    phone == null -> noContact(action.contact)
                    simulate -> sim("Llamaría a ${action.contact} al $phone.")
                    else -> {
                        tts.speak("Llamo a ${action.contact}.")
                        CallAction.perform(context, phone).fold(
                            onSuccess = { "Llamando a ${action.contact} ($phone)" },
                            onFailure = { fail("llamar", it) },
                        )
                    }
                }
            }
            is IntentAction.WhatsApp -> {
                val phone = contactResolver.resolvePhoneNumber(action.contact)
                val hasMsg = action.message.isNotBlank()
                when {
                    phone == null -> noContact(action.contact)
                    simulate -> sim(
                        if (hasMsg) "Mandaría un WhatsApp a ${action.contact} al $phone que dice: ${action.message}."
                        else "Abriría WhatsApp con ${action.contact} al $phone."
                    )
                    else -> {
                        tts.speak(
                            if (hasMsg) "Mando un WhatsApp a ${action.contact} que dice: ${action.message}."
                            else "Abro WhatsApp con ${action.contact}."
                        )
                        WhatsAppAction.perform(context, phone, action.message).fold(
                            onSuccess = {
                                if (hasMsg) "WhatsApp a ${action.contact}: \"${action.message}\""
                                else "WhatsApp con ${action.contact}"
                            },
                            onFailure = { fail("abrir WhatsApp", it) },
                        )
                    }
                }
            }
            is IntentAction.Sms -> {
                val phone = contactResolver.resolvePhoneNumber(action.contact)
                when {
                    phone == null -> noContact(action.contact)
                    simulate -> sim("Mandaría un SMS a ${action.contact} al $phone que dice: ${action.message}.")
                    else -> {
                        tts.speak("Mando un mensaje a ${action.contact} que dice: ${action.message}.")
                        SmsAction.perform(context, phone, action.message).fold(
                            onSuccess = { "SMS enviado a ${action.contact}" },
                            onFailure = { fail("enviar el SMS", it) },
                        )
                    }
                }
            }
            is IntentAction.Alarm -> {
                val hhmm = "%02d:%02d".format(action.hour, action.minute)
                if (simulate) {
                    sim("Pondría una alarma a las $hhmm.")
                } else {
                    tts.speak("Pongo una alarma a las $hhmm.")
                    AlarmAction.perform(context, action.hour, action.minute, action.label).fold(
                        onSuccess = { "Alarma a las $hhmm" },
                        onFailure = { fail("poner la alarma", it) },
                    )
                }
            }
            is IntentAction.Respond -> {
                tts.speak(action.text)
                action.text
            }
            is IntentAction.Open -> {
                if (simulate) {
                    sim("Abriría ${action.screen.title}.")
                } else {
                    tts.speak("Abro ${action.screen.title}.")
                    nav.open(action.screen)
                    "Abriendo ${action.screen.title}"
                }
            }
            IntentAction.Back -> {
                if (simulate) {
                    sim("Volvería al inicio.")
                } else {
                    tts.speak("Vuelvo al inicio.")
                    nav.home()
                    "Inicio"
                }
            }
            IntentAction.Unknown -> {
                val msg = "No te he entendido, prueba otra vez."
                tts.speak(msg); msg
            }
        }
    }

    private suspend fun noContact(name: String): String {
        val msg = "No encuentro a $name en tus contactos."
        tts.speak(msg)
        return msg
    }

    private suspend fun sim(msg: String): String {
        tts.speak("Simulación. $msg")
        return "🧪 $msg"
    }

    private suspend fun fail(what: String, t: Throwable): String {
        Log.w(TAG, "fallo al $what", t)
        val msg = "No he podido $what."
        tts.speak(msg)
        return "$msg (${t.message ?: t::class.java.simpleName})"
    }
}
