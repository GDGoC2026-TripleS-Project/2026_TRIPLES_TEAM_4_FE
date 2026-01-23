package com.project.unimate.auth

import android.content.Context

object JwtStore {
    private const val PREF = "unimate"
    private const val KEY = "jwt"

    fun save(context: Context, jwt: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, jwt)
            .apply()
    }

    fun load(context: Context): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
