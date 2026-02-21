package com.project.unimate.data.repository

// 역할: 사용자가 삭제한 시드 팀 이름 저장. 재시작 후 목록에서 제외

import android.content.Context

/** 삭제한 시드 팀 이름 저장/조회. */
object DeletedSeedTeamStore {
    private const val PREFS_NAME = "deleted_seed_teams"
    private const val KEY_NAMES = "team_names"

    fun add(context: Context, teamName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_NAMES, null)?.toMutableSet() ?: mutableSetOf()
        current.add(teamName)
        prefs.edit().putStringSet(KEY_NAMES, current).apply()
    }

    fun getDeletedNames(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_NAMES, null) ?: emptySet()
    }
}
