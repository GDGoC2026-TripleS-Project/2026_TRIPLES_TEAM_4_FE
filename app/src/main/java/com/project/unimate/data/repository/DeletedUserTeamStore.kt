package com.project.unimate.data.repository

// 역할: 사용자가 삭제한 서버 팀 ID 저장. 재시작 후 팀 목록에서 제외

import android.content.Context

/** 삭제한 팀 ID 저장/조회. */
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
