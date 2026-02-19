package com.project.unimate.data.entity

import java.util.Calendar

/**
 * 팀플 할일(체크리스트) 엔티티.
 * @param id 일정 ID
 * @param teamId 소속 팀 ID
 * @param title 할일 제목
 * @param date 연동 날짜 (년월일) — 표시/필터용
 * @param startTimeMillis 일정 시작 시각 (ms)
 * @param endTimeMillis 일정 종료 시각 (ms)
 * @param isChecked 완료 여부
 * @param creatorName 일정을 생성한 팀원 이름 (팀 스페이스에서만 표시)
 * @param notificationCategory 알림 (예: "없음", "5분 전")
 */
data class TaskItem(
    val id: String,
    val teamId: String,
    val title: String,
    val date: Calendar,
    val startTimeMillis: Long = date.timeInMillis,
    val endTimeMillis: Long = date.timeInMillis,
    val isChecked: Boolean,
    val creatorName: String? = null,
    val notificationCategory: String = "없음"
) {
    fun isSameDay(other: Calendar): Boolean {
        return date.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            date.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }

    /** 해당 날짜가 일정 구간(시작일~종료일) 안에 있으면 true. 여러 날에 걸친 일정이 그날에도 표시되도록. */
    fun isOnDate(cal: Calendar): Boolean {
        val dayStart = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val dayEnd = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return endTimeMillis >= dayStart && startTimeMillis <= dayEnd
    }
}
