package com.project.unimate.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.entity.TaskItem
import java.util.Calendar

/** 팀 일정·개인 일정 전체를 저장/복원. 앱 재시작 후에도 추가·수정한 일정 유지. */
object ScheduleStore {
    private const val PREFS_NAME = "schedule_store"
    private const val KEY_TASKS = "task_items"
    private const val KEY_PERSONAL = "personal_items"
    private val gson = Gson()

    private data class TaskDto(
        val id: String,
        val teamId: String,
        val title: String,
        val dateMillis: Long,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val isChecked: Boolean,
        val creatorName: String?
    )

    private data class PersonalDto(
        val id: String,
        val title: String,
        val dateMillis: Long,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val isLocked: Boolean,
        val isChecked: Boolean,
        val notificationCategory: String,
        val scheduleCategory: String
    )

    fun saveTaskItems(context: Context, items: List<TaskItem>) {
        val dtos = items.map { t ->
            TaskDto(
                id = t.id,
                teamId = t.teamId,
                title = t.title,
                dateMillis = t.date.timeInMillis,
                startTimeMillis = t.startTimeMillis,
                endTimeMillis = t.endTimeMillis,
                isChecked = t.isChecked,
                creatorName = t.creatorName
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TASKS, gson.toJson(dtos)).apply()
    }

    fun loadTaskItems(context: Context): List<TaskItem>? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TASKS, null) ?: return null
        return try {
            val type = object : TypeToken<List<TaskDto>>() {}.type
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson<List<TaskDto>>(json, type) ?: return null).map { d ->
                val cal = Calendar.getInstance().apply { timeInMillis = d.dateMillis }
                TaskItem(
                    id = d.id,
                    teamId = d.teamId,
                    title = d.title,
                    date = cal,
                    startTimeMillis = d.startTimeMillis,
                    endTimeMillis = d.endTimeMillis,
                    isChecked = d.isChecked,
                    creatorName = d.creatorName
                )
            }
        } catch (_: Exception) { null }
    }

    fun savePersonalItems(context: Context, items: List<PersonalScheduleItem>) {
        val dtos = items.map { p ->
            PersonalDto(
                id = p.id,
                title = p.title,
                dateMillis = p.date.timeInMillis,
                startTimeMillis = p.startTimeMillis,
                endTimeMillis = p.endTimeMillis,
                isLocked = p.isLocked,
                isChecked = p.isChecked,
                notificationCategory = p.notificationCategory,
                scheduleCategory = p.scheduleCategory
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_PERSONAL, gson.toJson(dtos)).apply()
    }

    fun loadPersonalItems(context: Context): List<PersonalScheduleItem>? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_PERSONAL, null) ?: return null
        return try {
            val type = object : TypeToken<List<PersonalDto>>() {}.type
            @Suppress("UNCHECKED_CAST")
            (gson.fromJson<List<PersonalDto>>(json, type) ?: return null).map { d ->
                val cal = Calendar.getInstance().apply { timeInMillis = d.dateMillis }
                PersonalScheduleItem(
                    id = d.id,
                    title = d.title,
                    date = cal,
                    startTimeMillis = d.startTimeMillis,
                    endTimeMillis = d.endTimeMillis,
                    isLocked = d.isLocked,
                    isChecked = d.isChecked,
                    notificationCategory = d.notificationCategory,
                    scheduleCategory = d.scheduleCategory
                )
            }
        } catch (_: Exception) { null }
    }
}
