package com.vozmayores.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

object AlarmAction {
    fun perform(
        context: Context,
        hour: Int,
        minute: Int,
        label: String? = null,
    ): Result<Unit> = runCatching {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
