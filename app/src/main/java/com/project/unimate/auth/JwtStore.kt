package com.project.unimate.auth

import android.content.Context

object JwtStore {
    private const val PREF = "unimate_auth"
    private const val KEY = "jwt"
    private const val KEY_USER_ID = "user_id"

    fun save(context: Context, jwt: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, jwt.trim())
            .apply()
    }

    fun saveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun load(context: Context): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun loadUserId(context: Context): Long? {
        val id = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1L)
        return if (id > 0) id else null
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .remove(KEY_USER_ID)
            .apply()
    }
}
