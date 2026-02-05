package com.project.unimate.data.repository

import com.project.unimate.data.entity.CalendarDayEvent
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.entity.Team
import java.util.Calendar

/**
 * 화면 연동용 더미 데이터. 홈/캘린더/마이페이지에서 공유.
 * 실제 서버 연동 시 이 데이터 소스를 API로 교체하면 됨.
 */
object DummyRepository {

    private val calendar = Calendar.getInstance()

    /** 팀 7개: 캡스톤, 체리시, 마모사리, 메가커피릿, 모마미, 행복의 심리학, 인공지능 입문. 마감일: 메가커피릿/체리시/모마미 2/5 이전, 나머지 이후 */
    val allTeams: List<Team> = listOf(
        Team("capstone", "캡스톤", "#E8E0A0", "megacoffe_image", false, 4, 10),
        Team("cherish", "체리시", "#F495E0", "cherish_image", false, 4, 2),
        Team("mamosari", "마모사리", "#C8E6C8", "cherish_image", false, 6, 8),
        Team("megacoffe", "메가커피릿", "#FFACAC", "megacoffe_image", false, 4, 1),
        Team("momami", "모마미", "#98ADFF", "momami_image", false, 4, 3),
        Team("psychology", "행복의 심리학", "#EDF3D7", "megacoffe_image", false, 4, 3),
        Team("ai_intro", "인공지능 입문", "#EDF3D7", "cherish_image", false, 6, 7)
    )

    /** 나의 팀 스페이스 (홈 하단): 메가커피릿, 체리시, 모마미 */
    fun getMyTeamSpaceTeams(): List<Team> = allTeams.filter { it.id in listOf("megacoffe", "cherish", "momami") }

    /** 참여중인 팀플 (마이페이지): 행복의 심리학, 인공지능 입문 */
    fun getParticipatingTeamProjects(): List<Team> = allTeams.filter { it.id in listOf("psychology", "ai_intro") }

    /** 완료된 팀플 (마이페이지): 메가커피릿, 체리시, 모마미 */
    fun getCompletedTeamProjects(): List<Team> = allTeams.filter { it.id in listOf("megacoffe", "cherish", "momami") }.map { it.copy(isCompleted = true) }

    /** 캘린더 필터용 팀 목록 (전체 7개) */
    fun getCalendarFilterTeams(): List<Team> = allTeams

    // ---- 할일 더미 (팀플별) ----
    private fun task(teamId: String, title: String, year: Int, month: Int, day: Int, checked: Boolean): TaskItem {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return TaskItem("t-$teamId-$year-$month-$day-${title.hashCode()}", teamId, title, c, checked)
    }

    private val allTaskItems: List<TaskItem> = run {
        val list = mutableListOf<TaskItem>()
        // 기존: 체리시·캡스톤 2/4, 2/17
        list.add(task("cherish", "체리시 기획안 업로드", 2026, 2, 4, false))
        list.add(task("cherish", "캐릭터 제작 모델링", 2026, 2, 4, true))
        list.add(task("cherish", "학성지 VOD 3주차 시청", 2026, 2, 4, true))
        list.add(task("cherish", "캐릭터 제작 모델링", 2026, 2, 17, true))
        list.add(task("cherish", "학성지 VOD 3주차 시청", 2026, 2, 17, true))
        list.add(task("capstone", "유니 마감", 2026, 2, 4, false))
        list.add(task("capstone", "MT", 2026, 2, 4, false))
        list.add(task("capstone", "캡스톤 초안 형성", 2026, 2, 17, true))
        list.add(task("capstone", "캐릭터 제작 모델링", 2026, 2, 17, true))
        list.add(task("capstone", "학성지 VOD 3주차 시청", 2026, 2, 17, true))
        // 2026-01-01 ~ 2026-02-14: 매주 3일씩 일정 (팀플 할일)
        val teamIds = listOf("capstone", "cherish", "mamosari", "megacoffe", "momami", "psychology", "ai_intro")
        val weekDays = listOf(
            listOf(1, 2, 3), listOf(4, 5, 6), listOf(11, 12, 13), listOf(18, 19, 20),
            listOf(25, 26, 27), listOf(1, 2, 3), listOf(8, 9, 10)
        )
        val monthForWeek = listOf(1, 1, 1, 1, 1, 2, 2)
        val titleByTeam = mapOf(
            "capstone" to "캡스톤 일정", "cherish" to "체리시 일정", "mamosari" to "마모사리 일정",
            "megacoffe" to "메가커피릿 일정", "momami" to "모마미 일정",
            "psychology" to "행복의 심리학 일정", "ai_intro" to "인공지능 입문 일정"
        )
        weekDays.forEachIndexed { wi, days ->
            val month = monthForWeek[wi]
            val year = 2026
            days.forEachIndexed { di, day ->
                val tid = teamIds[(wi + di) % teamIds.size]
                list.add(task(tid, titleByTeam[tid] ?: "팀 일정", year, month, day, false))
            }
        }
        list
    }

    /** 체크 상태 오버라이드 (홈/캘린더 체크 시 반영) */
    private val taskCheckedOverrides = mutableMapOf<String, Boolean>()
    /** 개인일정 잠금/체크 상태 오버라이드 (락·체크 버튼 연동) */
    private val personalLockedOverrides = mutableMapOf<String, Boolean>()
    private val personalCheckedOverrides = mutableMapOf<String, Boolean>()

    fun setTaskChecked(taskId: String, checked: Boolean) { taskCheckedOverrides[taskId] = checked }
    fun setPersonalLocked(personalId: String, locked: Boolean) { personalLockedOverrides[personalId] = locked }
    fun setPersonalChecked(personalId: String, checked: Boolean) { personalCheckedOverrides[personalId] = checked }

    private fun effectiveTaskChecked(task: TaskItem): Boolean = taskCheckedOverrides[task.id] ?: task.isChecked
    private fun effectivePersonalLocked(item: PersonalScheduleItem): Boolean = personalLockedOverrides[item.id] ?: item.isLocked
    private fun effectivePersonalChecked(item: PersonalScheduleItem): Boolean = personalCheckedOverrides[item.id] ?: item.isChecked

    /** 특정 날짜의 팀 할일 (팀 ID 필터 적용). 체크 상태는 오버라이드 반영 */
    fun getTasksForDate(date: Calendar, teamIds: List<String>): List<TaskItem> =
        allTaskItems.filter { it.isSameDay(date) && it.teamId in teamIds }.map { t ->
            t.copy(isChecked = effectiveTaskChecked(t))
        }

    /** 오늘 날짜의 팀 할일 (팀별 그룹용). 체크 상태 오버라이드 반영 */
    fun getTodayTasksByTeam(today: Calendar): Map<String, List<TaskItem>> {
        val list = allTaskItems.filter { it.isSameDay(today) }.map { t ->
            t.copy(isChecked = effectiveTaskChecked(t))
        }
        return list.groupBy { it.teamId }
    }

    // ---- 개인일정 더미 ----
    private fun personal(id: String, title: String, year: Int, month: Int, day: Int, locked: Boolean, checked: Boolean): PersonalScheduleItem {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return PersonalScheduleItem(id, title, c, locked, checked)
    }

    private val allPersonalItems: List<PersonalScheduleItem> = run {
        val list = mutableListOf<PersonalScheduleItem>()
        list.add(personal("p1", "메가커피 아르바이트", 2026, 2, 4, false, false))
        list.add(personal("p2", "메가커피 아르바이트", 2026, 2, 17, false, true))
        val weekDaysWithSchedule = listOf(
            listOf(1, 2, 3), listOf(4, 5, 6), listOf(11, 12, 13), listOf(18, 19, 20),
            listOf(25, 26, 27), listOf(1, 2, 3), listOf(8, 9, 10)
        )
        val monthForWeek = listOf(1, 1, 1, 1, 1, 2, 2)
        weekDaysWithSchedule.forEachIndexed { wi, days ->
            val month = monthForWeek[wi]
            val year = 2026
            days.forEachIndexed { di, day ->
                list.add(personal("p-$year-$month-$day", "개인일정", year, month, day, false, false))
            }
        }
        list
    }

    fun getPersonalForDate(date: Calendar): List<PersonalScheduleItem> =
        allPersonalItems.filter { it.isSameDay(date) }.map { p ->
            p.copy(isLocked = effectivePersonalLocked(p), isChecked = effectivePersonalChecked(p))
        }

    fun getPersonalForToday(today: Calendar): List<PersonalScheduleItem> = getPersonalForDate(today)

    /** 그날 일정 개수 (팀 할일 + 개인일정, 팀 구분 없이). 캘린더 날짜칸 표시용 */
    fun getDayEventCount(date: Calendar, teamIds: List<String>): Int {
        val taskCount = allTaskItems.count { it.isSameDay(date) && it.teamId in teamIds }
        val personalCount = allPersonalItems.count { it.isSameDay(date) }
        return taskCount + personalCount
    }

    // ---- 캘린더 날짜 칸용 일정 요약 (동그라미 + 제목 한 줄) ----
    /** 특정 날짜에 캘린더 칸에 표시할 이벤트. 팀플이 하나면 같은 팀에서 두 줄까지, 두 개 이상이면 팀당 한 일정만 */
    fun getCalendarDayEvents(date: Calendar, teamIds: List<String>): List<CalendarDayEvent> {
        val tasks = getTasksForDate(date, teamIds)
        val teamsMap = allTeams.associateBy { it.id }
        val byTeam = tasks.groupBy { it.teamId }
        return if (byTeam.size <= 1) {
            tasks.take(2).mapNotNull { t ->
                teamsMap[t.teamId]?.let { team -> CalendarDayEvent(t.teamId, team.colorHex, t.title) }
            }
        } else {
            byTeam.values.map { teamTasks -> teamTasks.first() }.take(3).mapNotNull { t ->
                teamsMap[t.teamId]?.let { team -> CalendarDayEvent(t.teamId, team.colorHex, t.title) }
            }
        }
    }

    fun getTeamById(teamId: String): Team? = allTeams.find { it.id == teamId }

    /** 현재 연·월 기준 일주일 날짜 (일~토) */
    fun getWeekDates(anchor: Calendar): List<Calendar> {
        val firstDayOfWeek = anchor.clone() as Calendar
        val dow = firstDayOfWeek.get(Calendar.DAY_OF_WEEK)
        firstDayOfWeek.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY))
        return (0..6).map { day ->
            (firstDayOfWeek.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, day) }
        }
    }

    /** 월 캘린더용: 해당 월의 첫 날 기준 6주 분 날짜 (앞뒤 이전/다음달 포함) */
    fun getMonthCalendarDays(year: Int, month: Int): List<Calendar> {
        val first = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dow = first.get(Calendar.DAY_OF_WEEK)
        first.add(Calendar.DAY_OF_MONTH, -(dow - Calendar.SUNDAY))
        val list = mutableListOf<Calendar>()
        repeat(42) {
            list.add((first.clone() as Calendar))
            first.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    /** 월 캘린더용: 해당 달 날짜만 (빈 칸 + 1~말일, 이전/다음달 미포함). null = 빈 칸 */
    fun getMonthCalendarDaysCurrentMonthOnly(year: Int, month: Int): List<Calendar?> {
        val first = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val dow = first.get(Calendar.DAY_OF_WEEK)
        val offset = dow - Calendar.SUNDAY
        val lastDay = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Calendar?>()
        repeat(offset) { list.add(null) }
        for (day in 1..lastDay) {
            list.add(Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, day)
            })
        }
        return list
    }
}
