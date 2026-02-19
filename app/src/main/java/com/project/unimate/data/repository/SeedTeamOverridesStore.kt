package com.project.unimate.data.repository

import android.content.Context
import com.project.unimate.data.entity.Team

/** 시드(더미) 팀에 대해 사용자가 수정한 진행/종료 상태·날짜 저장. 병합 후 해당 팀에 적용해 재시작 후에도 반영. */
object SeedTeamOverridesStore {
    private const val PREFS_NAME = "seed_team_overrides"
    private const val SUFFIX_COMPLETED = "_completed"
    private const val SUFFIX_END = "_end"
    private const val SUFFIX_START = "_start"
    private const val NULL_MILLIS = -1L

    fun save(
        context: Context,
        teamId: String,
        isCompleted: Boolean,
        workEndMillis: Long?,
        workStartMillis: Long?
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(key(teamId, SUFFIX_COMPLETED), isCompleted)
            .putLong(key(teamId, SUFFIX_END), workEndMillis ?: NULL_MILLIS)
            .putLong(key(teamId, SUFFIX_START), workStartMillis ?: NULL_MILLIS)
            .apply()
    }

    private fun key(teamId: String, suffix: String) = "override_${teamId}$suffix"

    fun getOverride(context: Context, teamId: String): Override? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(key(teamId, SUFFIX_COMPLETED))) return null
        val end = prefs.getLong(key(teamId, SUFFIX_END), NULL_MILLIS)
        val start = prefs.getLong(key(teamId, SUFFIX_START), NULL_MILLIS)
        return Override(
            isCompleted = prefs.getBoolean(key(teamId, SUFFIX_COMPLETED), false),
            workEndMillis = if (end == NULL_MILLIS) null else end,
            workStartMillis = if (start == NULL_MILLIS) null else start
        )
    }

    data class Override(
        val isCompleted: Boolean,
        val workEndMillis: Long?,
        val workStartMillis: Long?
    )

    /** 병합된 팀 목록에서 시드 팀 id에 해당하는 팀에 저장된 오버레이 적용. */
    fun applyOverrides(context: Context, teams: List<Team>, seedTeamIds: Set<String>): List<Team> {
        return teams.map { team ->
            if (team.id !in seedTeamIds) return@map team
            getOverride(context, team.id)?.let { o ->
                team.copy(
                    isCompleted = o.isCompleted,
                    workEndMillis = o.workEndMillis ?: team.workEndMillis,
                    workStartMillis = o.workStartMillis ?: team.workStartMillis,
                    completedAtMillis = if (o.isCompleted) (o.workEndMillis ?: team.workEndMillis) else null
                )
            } ?: team
        }
    }
}
