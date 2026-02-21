package com.project.unimate.data.repository

// 역할: 팀별 이미지 리소스명 저장. replaceTeamsWithServerData 후 복원

import android.content.Context

/** 팀별 이미지 저장/조회. */
object TeamImageStore {
    private const val PREFS_NAME = "team_images"
    private const val PREFIX = "img_"

    fun save(context: Context, teamId: String, imageResName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREFIX + teamId, imageResName).apply()
    }

    fun get(context: Context, teamId: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFIX + teamId, null)

    /** teamId → imageResName (비어있지 않은 것만) */
    fun getAll(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.keys
            .filter { it.startsWith(PREFIX) }
            .mapNotNull { key ->
                val id = key.removePrefix(PREFIX)
                val value = prefs.getString(key, null)
                if (value.isNullOrBlank()) null else id to value
            }.toMap()
    }
}
