package com.project.unimate.model


data class HomeResponse(
    val date: String,
    val weeklyCalendar: List<WeeklyCalendarItem>,
    val todaySchedules: TodaySchedules,
    val myTeamSpaces: List<HomeTeamSpace>,
    val notification: HomeNotification
)

data class WeeklyCalendarItem(
    val date: String,
    val scheduleCount: Int,
    val today: Boolean
)

data class TodaySchedules(
    val teamSchedules: List<HomeTeamSchedule>,
    val personalSchedules: List<HomePersonalSchedule>
)

data class HomeTeamSchedule(
    val teamId: Long,
    val teamName: String,
    val teamColor: String,
    val schedules: List<SimpleSchedule>
)

data class SimpleSchedule(
    val scheduleId: Long,
    val title: String,
    val startAt: String,
    val endAt: String
)

data class HomePersonalSchedule(
    val scheduleId: Long,
    val title: String,
    val startAt: String,
    val endAt: String,
    val private: Boolean
)

data class HomeTeamSpace(
    val teamId: Long,
    val teamName: String,
    val teamColor: String?,
    val teamProfileImageUrl: String?
)
data class HomeNotification(
    val hasUnread: Boolean,
    val unreadCount: Int
)