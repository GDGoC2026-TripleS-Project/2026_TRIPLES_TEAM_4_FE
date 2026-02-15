package com.project.unimate.model

data class CalendarDayResponse(
    val date: String,
    val teamSchedules: List<TeamScheduleGroup>,
    val personalSchedules: List<PersonalSchedule>
)

data class TeamScheduleGroup(
    val teamId: Long,
    val teamName: String,
    val teamColor: String?,
    val masked: Boolean,
    val schedules: List<TeamSchedule>
)

data class TeamSchedule(
    val scheduleId: Long,
    val title: String,
    val description: String?,
    val startAt: String,
    val endAt: String,
    val masked: Boolean,
    val isCompleted: Boolean
)

data class PersonalSchedule(
    val scheduleId: Long,
    val title: String,
    val startAt: String,
    val endAt: String,
    val isCompleted: Boolean,
    val masked: Boolean,
    val private: Boolean
)

data class ScheduleItem(
    val scheduleId: Long,
    val title: String?,
    val description: String?,
    val startAt: String,
    val endAt: String,
    val isCompleted: Boolean,
    val masked: Boolean,
    val type: String?,
    val teamId: Long?,
    val teamName: String?
)

data class CalendarMonthResponse(
    val month: String,
    val dayCounts: List<DayCountResponse>
)

data class DayCountResponse(
    val date: String,
    val count: Int
)

data class CreateScheduleRequest(
    val title: String,
    val memo: String,
    val startAt: String,
    val endAt: String,
    val category: String?,
    val categoryMemo: String?,
    val alarmMinutes: Int = 0
)

data class ScheduleDetail(
    val id: Long,
    val teamId: Long,
    val createdBy: Long,
    val title: String,
    val memo: String?,
    val startAt: String,
    val endAt: String,
    val category: String?,
    val categoryMemo: String?,
    val alarmMinutes: Int?,
    val createdAt: String,
    val updatedAt: String
)