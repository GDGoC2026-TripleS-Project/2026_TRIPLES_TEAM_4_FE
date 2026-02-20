package com.project.unimate.ui.timepick

import java.util.Calendar

/**
 * 타임픽 생성 플로우(Create -> Select -> Status) 상태 보존용.
 * 뒤로가기 시 이전 화면 상태 복원에 사용.
 */
object TimepickStateHolder {
    data class PollMember(
        val memberId: Long,
        val name: String
    )

    var teamId: String = ""
    var pollId: Long? = null
    var pollStatus: String? = null
    var pollMembers: List<PollMember> = emptyList()
    var pollSelectionsByMember: Map<Long, Set<Pair<Int, Int>>> = emptyMap()
    var pollIntersectionCells: Set<Pair<Int, Int>> = emptySet()

    /** 선택된 날짜 (각 날짜의 00:00 기준 millis) */
    val selectedDates: MutableSet<Long> = mutableSetOf()

    /** 날짜(dayStartMillis) -> (시작시각 0~24, 종료시각 0~24). 선택된 날짜별 가능 시간 범위. */
    val dateTimeRanges: MutableMap<Long, Pair<Int, Int>> = mutableMapOf()

    /** SelectTimeFragment에서 사용자가 선택한 셀: (dayIndex, hourIndex) -> true */
    val selectTimeSelected: MutableMap<Pair<Int, Int>, Boolean> = mutableMapOf()

    /** 표시할 날짜 목록 (순서 유지, 7일). CreateTimepick에서 선택한 날짜들로 구성. */
    var displayDates: List<Long> = emptyList()

    /** 표시할 시간 범위: startHour(8), endHour(24) 등 */
    var globalStartHour: Int = 8
    var globalEndHour: Int = 24

    /** 4페이지에서 확정한 교집합 셀 (dayIndex, hourIndex). 5페이지 편집 시 타임픽에 표시. */
    var confirmedIntersection: Set<Pair<Int, Int>> = emptySet()

    fun clear() {
        teamId = ""
        pollId = null
        pollStatus = null
        pollMembers = emptyList()
        pollSelectionsByMember = emptyMap()
        pollIntersectionCells = emptySet()
        selectedDates.clear()
        dateTimeRanges.clear()
        selectTimeSelected.clear()
        displayDates = emptyList()
        confirmedIntersection = emptySet()
    }

    fun dayStartMillis(cal: Calendar): Long {
        val c = cal.clone() as Calendar
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
