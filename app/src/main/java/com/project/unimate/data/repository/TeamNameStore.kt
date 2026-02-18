package com.project.unimate.data.repository

import android.content.Context

/** 팀플명 수정값 저장. 더미/사용자 팀 구분 없이 teamId 기준으로 저장해 재시작 후에도 반영. */
object TeamNameStore {
    private const val PREFS_NAME = "team_names"
    private const val PREFIX = "name_"

    fun save(context: Context, teamId: String, name: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREFIX + teamId, name).apply()
    }

    fun get(context: Context, teamId: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFIX + teamId, null)

    fun getAll(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.keys
            .filter { it.startsWith(PREFIX) }
            .mapNotNull { key ->
                val id = key.removePrefix(PREFIX)
                prefs.getString(key, null)?.takeIf { it.isNotBlank() }?.let { id to it }
            }.toMap()
    }
}
