package com.vozmayores.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppAction {
    fun perform(context: Context, phone: String, message: String): Result<Unit> = runCatching {
        // wa.me acepta el numero en formato internacional sin '+'.
        val digits = phone.filter { it.isDigit() || it == '+' }.removePrefix("+")
        val encoded = URLEncoder.encode(message, "UTF-8")
        val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
