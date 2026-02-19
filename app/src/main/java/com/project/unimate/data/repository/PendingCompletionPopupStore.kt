package com.project.unimate.data.repository

import android.content.Context

/** 수정 페이지에서 "종료 체크 후 저장"한 팀 ID만 저장. 해당 팀에 대해 최초 1회만 팀플 종료 알림 표시. */
object PendingCompletionPopupStore {
    private const val PREFS_NAME = "pending_completion_popup"
    private const val KEY_IDS = "team_ids"

    fun add(context: Context, teamId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_IDS, null)?.toMutableSet() ?: mutableSetOf()
        current.add(teamId)
        prefs.edit().putStringSet(KEY_IDS, current).apply()
    }

    fun getPendingIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_IDS, null) ?: emptySet()
    }

    fun remove(context: Context, teamId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_IDS, null)?.toMutableSet() ?: return
        current.remove(teamId)
        prefs.edit().putStringSet(KEY_IDS, current).apply()
    }
}
