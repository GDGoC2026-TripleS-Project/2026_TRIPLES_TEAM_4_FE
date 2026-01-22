package com.project.unimate.auth

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
    private const val PREF = "unimate"
    private const val KEY = "device_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY, newId).apply()
        return newId
    }
}
