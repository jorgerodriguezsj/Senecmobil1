package com.vozmayores.ui

import android.content.Context

/** Wrapper mínimo sobre SharedPreferences para settings de la app. */
class Prefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("voz", Context.MODE_PRIVATE)

    var simulate: Boolean
        get() = prefs.getBoolean(KEY_SIMULATE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SIMULATE, value).apply()
        }

    private companion object {
        const val KEY_SIMULATE = "simulate"
    }
}
