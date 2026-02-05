package com.project.unimate.data.entity

import java.util.Calendar

/**
 * 개인일정 엔티티.
 * @param id 일정 ID
 * @param title 일정 제목 (lock 시 팀원에게는 "개인일정"만 표시)
 * @param date 연동 날짜
 * @param isLocked true: 팀원에게 "개인일정"만 표시, false: 제목 공개
 * @param isChecked 완료 여부
 */
data class PersonalScheduleItem(
    val id: String,
    val title: String,
    val date: Calendar,
    val isLocked: Boolean,
    val isChecked: Boolean
) {
    fun isSameDay(other: Calendar): Boolean {
        return date.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            date.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }
}
