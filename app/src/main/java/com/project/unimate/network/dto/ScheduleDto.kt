package com.project.unimate.network.dto

// === Team Schedule ===

data class TeamScheduleCreateRequest(
    val title: String,
    val memo: String? = null,
    val startAt: String,
    val endAt: String,
    val category: String,
    val categoryMemo: String? = null,
    val alarmMinutes: Int? = null
)

data class TeamScheduleUpdateRequest(
    val title: String,
    val memo: String? = null,
    val startAt: String,
    val endAt: String,
    val category: String? = null,
    val categoryMemo: String? = null,
    val alarmMinutes: Int? = null
)

data class TeamScheduleResponse(
    val id: Long?,
    val teamId: Long?,
    val createdBy: Long?,
    val title: String?,
    val memo: String?,
    val startAt: String?,
    val endAt: String?,
    val category: String?,
    val categoryMemo: String?,
    val alarmMinutes: Int?,
    val createdAt: String?,
    val updatedAt: String?
)

// === My (Personal) Schedule ===

data class MyScheduleCreateRequest(
    val title: String,
    val memo: String? = null,
    val startAt: String,
    val endAt: String,
    val isPrivate: Boolean,
    val category: String,
    val categoryMemo: String? = null,
    val alarmMinutes: Int? = null
)

data class MyScheduleUpdateRequest(
    val title: String,
    val memo: String? = null,
    val startAt: String,
    val endAt: String,
    val isPrivate: Boolean,
    val category: String? = null,
    val categoryMemo: String? = null,
    val alarmMinutes: Int? = null
)

data class MyScheduleResponse(
    val id: Long?,
    val teamId: Long?,
    val userId: Long?,
    val title: String?,
    val memo: String?,
    val startAt: String?,
    val endAt: String?,
    val category: String?,
    val categoryMemo: String?,
    val alarmMinutes: Int?,
    val createdAt: String?,
    val updatedAt: String?,
    @com.google.gson.annotations.SerializedName("private") val isPrivate: Boolean?
)

data class MyScheduleMarkingResponse(
    val markedDates: List<String>?
)

data class MyScheduleNowResponse(
    val busy: Boolean?,
    val schedules: List<ScheduleTimeRange>?
)

data class ScheduleTimeRange(
    val scheduleId: Long?,
    val title: String?,
    val startAt: String?,
    val endAt: String?
)
