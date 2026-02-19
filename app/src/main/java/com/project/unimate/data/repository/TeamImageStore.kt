package com.project.unimate.data.repository

import android.content.Context

/** 팀 사진(생성/수정 시 추가한 이미지) 저장. 앱 재시작 후 replaceTeamsWithServerData 적용 후 이 값으로 복원. */
object TeamImageStore {
    private const val PREFS_NAME = "team_images"
    private const val PREFIX = "img_"

    fun save(context: Context, teamId: String, imageResName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PREFIX + teamId, imageResName).apply()
    }

    fun get(context: Context, teamId: String): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(PREFIX + teamId, null)

    /** teamId -> imageResName (비어있지 않은 것만) */
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
