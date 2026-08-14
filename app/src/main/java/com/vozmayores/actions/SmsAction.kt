package com.vozmayores.actions

import android.content.Context
import android.os.Build
import android.telephony.SmsManager

object SmsAction {
    fun perform(context: Context, phone: String, message: String): Result<Unit> = runCatching {
        val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        val parts = sms.divideMessage(message)
        if (parts.size == 1) {
            sms.sendTextMessage(phone.trim(), null, message, null, null)
        } else {
            sms.sendMultipartTextMessage(phone.trim(), null, parts, null, null)
        }
    }
}
