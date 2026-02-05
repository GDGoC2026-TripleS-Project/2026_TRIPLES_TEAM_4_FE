package com.project.unimate.data.entity

import java.util.Calendar

/**
 * 팀플 할일(체크리스트) 엔티티.
 * @param id 일정 ID
 * @param teamId 소속 팀 ID
 * @param title 할일 제목
 * @param date 연동 날짜 (년월일)
 * @param isChecked 완료 여부
 */
data class TaskItem(
    val id: String,
    val teamId: String,
    val title: String,
    val date: Calendar,
    val isChecked: Boolean
) {
    fun isSameDay(other: Calendar): Boolean {
        return date.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            date.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }
}
