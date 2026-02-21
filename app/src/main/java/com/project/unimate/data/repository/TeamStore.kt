package com.project.unimate.data.repository

// 역할: 팀 목록 SharedPreferences 저장/복원. 서버 sync 후 저장, 앱 재시작 시 복원

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.project.unimate.data.entity.Team

/** 팀 목록 저장/복원. sync 후 저장해 두면 다음 실행 시 복원. */
object TeamStore {
    private const val PREFS_NAME = "team_store"
    private const val KEY_TEAMS = "teams"
    private val gson = Gson()

    private data class TeamDto(
        val id: String,
        val name: String,
        val colorHex: String,
        val imageResName: String,
        val isCompleted: Boolean,
        val memberCount: Int,
        val deadlineDays: Int?,
        val intro: String,
        val workStartMillis: Long?,
        val workEndMillis: Long?,
        val completedAtMillis: Long?
    )

    fun saveTeams(context: Context, teams: List<Team>) {
        val dtos = teams.map { t ->
            TeamDto(
                id = t.id,
                name = t.name,
                colorHex = t.colorHex,
                imageResName = t.imageResName,
                isCompleted = t.isCompleted,
                memberCount = t.memberCount,
                deadlineDays = t.deadlineDays,
                intro = t.intro,
                workStartMillis = t.workStartMillis,
                workEndMillis = t.workEndMillis,
                completedAtMillis = t.completedAtMillis
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TEAMS, gson.toJson(dtos)).apply()
    }

    fun loadTeams(context: Context): List<Team>? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TEAMS, null) ?: return null
        return try {
            val type = object : TypeToken<List<TeamDto>>() {}.type
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson<List<TeamDto>>(json, type) ?: return null).map { d ->
                Team(
                    id = d.id,
                    name = d.name,
                    colorHex = d.colorHex,
                    imageResName = d.imageResName,
                    isCompleted = d.isCompleted,
                    memberCount = d.memberCount,
                    deadlineDays = d.deadlineDays,
                    intro = d.intro,
                    workStartMillis = d.workStartMillis,
                    workEndMillis = d.workEndMillis,
                    completedAtMillis = d.completedAtMillis
                )
            }
        } catch (_: Exception) { null }
    }
}
