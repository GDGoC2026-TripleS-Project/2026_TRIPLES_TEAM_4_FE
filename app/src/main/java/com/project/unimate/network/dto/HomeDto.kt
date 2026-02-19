package com.project.unimate.network.dto

// === Home ===

data class HomeSummaryResponse(
    val date: String?,
    val weekStart: String?,
    val weekEnd: String?,
    val weeklyCalendar: List<WeeklyCalendarDayDto>?,
    val todaySchedules: TodaySchedulesDto?,
    val myTeamSpaces: List<MyTeamSpaceDto>?,
    val notification: NotificationBadgeDto?
)

data class WeeklyCalendarDayDto(
    val date: String?,
    val scheduleCount: Int?,
    val today: Boolean?
)

data class TodaySchedulesDto(
    val teamSchedules: List<TeamTodaySchedulesDto>?,
    val personalSchedules: List<PersonalScheduleItemDto>?
)

data class TeamTodaySchedulesDto(
    val teamId: Long?,
    val teamName: String?,
    val teamColor: String?,
    val schedules: List<ScheduleItemDto>?
)

data class ScheduleItemDto(
    val scheduleId: Long?,
    val title: String?,
    val startAt: String?,
    val endAt: String?
)

data class PersonalScheduleItemDto(
    val teamId: Long?,
    val scheduleId: Long?,
    val title: String?,
    val startAt: String?,
    val endAt: String?,
    val private: Boolean?
)

data class MyTeamSpaceDto(
    val teamId: Long?,
    val teamName: String?,
    val teamColor: String?,
    val teamProfileImageUrl: String?
)

data class NotificationBadgeDto(
    val hasUnread: Boolean?,
    val unreadCount: Int?
)

// === Calendar ===

data class CalendarMonthResponse(
    val month: String?,
    val dayCounts: List<CalendarDayCountResponse>?
)

data class CalendarDayCountResponse(
    val date: String?,
    val count: Int?
)

data class CalendarDayResponse(
    val date: String?,
    val teamSchedules: List<TeamScheduleGroupResponse>?,
    val personalSchedules: List<CalendarItemResponse>?
)

data class TeamScheduleGroupResponse(
    val teamId: Long?,
    val teamName: String?,
    val schedules: List<CalendarItemResponse>?
)

data class CalendarItemResponse(
    val scheduleId: Long?,
    val type: String?,
    val teamId: Long?,
    val teamName: String?,
    val title: String?,
    val startAt: String?,
    val endAt: String?,
    val isCompleted: Boolean?,
    val masked: Boolean?
)

// === MyPage ===

data class MyPageSummaryResponse(
    val profile: MyProfileResponse?,
    val activeTeams: List<MyTeamCardResponse>?,
    val completedTeams: List<MyTeamCardResponse>?
)

data class MyProfileResponse(
    val userId: Long?,
    val nickname: String?,
    val email: String?,
    val profileImageUrl: String?
)

data class MyTeamCardResponse(
    val teamId: Long?,
    val name: String?,
    val description: String?,
    val memberCount: Long?,
    val startAt: String?,
    val endAt: String?,
    val isCompleted: Boolean?,
    val color: String?,
    val colorHex: String?,
    val dday: String?
)

// === Member Status ===

data class TeamMemberBusyNowResponse(
    val members: List<MemberBusy>?
)

data class MemberBusy(
    val userId: Long?,
    val busy: Boolean?
)

data class TeamIdleMemberResponse(
    val userId: Long?,
    val nickname: String?,
    val profileImageUrl: String?,
    val displayColorHex: String?
)

// === Notification ===

data class NotificationItemResponse(
    val id: Long?,
    val type: String?,
    val alarmType: String?,
    val teamId: Long?,
    val teamName: String?,
    val teamColorHex: String?,
    val messageTitle: String?,
    val messageBody: String?,
    val createdAt: String?,
    val senderId: Long?,
    val receiverId: Long?,
    val teamScheduleId: Long?,
    val pokeId: Long?,
    val action: Boolean?,
    val actionDone: Boolean?,
    val processedAt: String?,
    val read: Boolean?
)

data class CompletionResponse(
    val notificationId: Long?,
    val userId: Long?,
    val completedAt: String?,
    val completed: Boolean?
)

// === FCM ===

data class FcmTokenRegisterRequest(
    val token: String,
    val deviceId: String? = null,
    val platform: String? = null
)

data class FcmSendRequest(
    val title: String? = null,
    val body: String? = null
)

data class FcmTestResponse(
    val success: Boolean?,
    val message: String?,
    val detail: String?
)
