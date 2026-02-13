package com.project.unimate.data.entity

/**
 * 캘린더 날짜 칸에 표시할 일정 요약 (한 줄씩).
 * 팀플 지정색 동그라미 + 제목.
 */
data class CalendarDayEvent(
    val teamId: String,
    val teamColorHex: String,
    val title: String
)
