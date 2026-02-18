package com.project.unimate.data.repository

import android.content.Context

/** 사용자가 삭제한 시드(더미) 팀 이름 저장. 앱 재시작 후에도 해당 팀이 다시 안 보이게 함. */
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
