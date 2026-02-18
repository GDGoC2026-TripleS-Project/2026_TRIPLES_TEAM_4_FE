package com.project.unimate.data.repository

import android.content.Context

/** 사용자가 삭제한 (서버에서 생성한) 팀 ID 저장. 앱 재시작 후 getMyTeams()에서 제외해 다시 안 보이게 함. */
object DeletedUserTeamStore {
    private const val PREFS_NAME = "deleted_user_teams"
    private const val KEY_IDS = "team_ids"

    fun add(context: Context, teamId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_IDS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(teamId)
        prefs.edit().putStringSet(KEY_IDS, current).apply()
    }

    fun getDeletedIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_IDS, null) ?: emptySet()
    }
}
