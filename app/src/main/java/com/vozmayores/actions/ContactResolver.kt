package com.vozmayores.actions

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

private const val TAG = "Voz.ContactResolver"

class ContactResolver(private val context: Context) {

    /**
     * Devuelve los nombres de todos los contactos. Se usa para
     * sembrar el initial_prompt de Whisper.
     */
    fun getAllNames(): List<String> {
        val out = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " ASC",
        ) ?: return emptyList()
        cursor.use {
            val col = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            if (col < 0) return emptyList()
            while (it.moveToNext()) {
                val name = it.getString(col) ?: continue
                if (name.isNotBlank()) out.add(name)
            }
        }
        Log.d(TAG, "getAllNames -> ${out.size} contactos")
        return out
    }

    /**
     * Busca el primer número que casa con `query`. Estrategia
     * (case + accent insensitive):
     *  1) exacto
     *  2) contiene la query
     *  3) alguna palabra de la query aparece en el nombre
     *     ("Pepe" → "Pepe García")
     * Devuelve null si no encuentra nada.
     */
    fun resolvePhoneNumber(query: String): String? {
        val q = fold(query)
        if (q.isEmpty()) return null

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        ) ?: return null

        var exact: String? = null
        var contains: String? = null
        var partial: String? = null
        val qTokens = q.split(' ').filter { it.length >= 3 }

        cursor.use {
            val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
            val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (nameCol < 0 || numCol < 0) return null
            while (it.moveToNext()) {
                val rawName = it.getString(nameCol) ?: continue
                val name = fold(rawName)
                val num = it.getString(numCol) ?: continue
                when {
                    name == q -> if (exact == null) exact = num
                    name.contains(q) -> if (contains == null) contains = num
                    qTokens.any { part -> name.contains(part) } ->
                        if (partial == null) partial = num
                }
            }
        }
        return exact ?: contains ?: partial
    }

    private fun fold(s: String): String = com.vozmayores.intent.LocalMatcher
        .stripAccents(s.trim().lowercase())
}
