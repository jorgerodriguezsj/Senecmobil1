package com.vozmayores.actions

import android.content.Context
import android.content.Intent
import android.net.Uri

object CallAction {
    fun perform(context: Context, phone: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:${phone.trim()}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
