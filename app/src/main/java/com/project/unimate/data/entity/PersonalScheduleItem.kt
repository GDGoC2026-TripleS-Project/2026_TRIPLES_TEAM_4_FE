package com.project.unimate.data.entity

import java.util.Calendar

/**
 * 개인일정 엔티티.
 * @param id 일정 ID
 * @param title 일정 제목 (lock 시 팀원에게는 "개인일정"만 표시)
 * @param date 연동 날짜 (표시/필터용)
 * @param startTimeMillis 일정 시작 시각 (ms)
 * @param endTimeMillis 일정 종료 시각 (ms)
 * @param isLocked true: 팀원에게 "개인일정"만 표시, false: 제목 공개
 * @param isChecked 완료 여부
 * @param notificationCategory 알림 (예: "없음", "있음")
 * @param scheduleCategory 일정 카테고리 (아르바이트, 다른 팀플, 회의 등)
 */
data class PersonalScheduleItem(
    val id: String,
    val title: String,
    val date: Calendar,
    val startTimeMillis: Long = (date.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis,
    val endTimeMillis: Long = (date.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis,
    val isLocked: Boolean,
    val isChecked: Boolean,
    val notificationCategory: String = "없음",
    val scheduleCategory: String = "없음"
) {
    fun isSameDay(other: Calendar): Boolean {
        return date.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            date.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }
}
