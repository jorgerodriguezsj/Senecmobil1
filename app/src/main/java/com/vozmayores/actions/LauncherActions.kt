package com.vozmayores.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

/**
 * Atajos para abrir apps del sistema desde los shortcuts de las
 * pantallas Alarmas y Mensajes. Todas devuelven Result<Unit> para
 * que la UI pueda mostrar un mensaje si el intent no lo resuelve
 * ninguna app (raro pero puede pasar en dispositivos capados).
 */
object LauncherActions {

    fun openSystemAlarms(context: Context): Result<Unit> = runCatching {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openWhatsApp(context: Context): Result<Unit> = runCatching {
        val launch = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
            ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        } else {
            // Fallback: intent a wa.me (abre el navegador → WhatsApp Web
            // si no está instalado).
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun openMessaging(context: Context): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_MESSAGING)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
