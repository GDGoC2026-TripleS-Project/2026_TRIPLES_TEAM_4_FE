package com.project.unimate.data.repository

import com.project.unimate.data.entity.CalendarDayEvent
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.entity.Team
import com.project.unimate.data.entity.TeamMember
import java.util.Calendar

/**
 * 화면 연동용 더미 데이터. 홈/캘린더/마이페이지에서 공유.
 * 실제 서버 연동 시 이 데이터 소스를 API로 교체하면 됨.
 */
object DummyRepository {

    private val calendar = Calendar.getInstance()

    // 팀플 소개문구 (팀 스페이스 상단/수정 초기값용). 팀 수정 시 Team.intro로 덮어씀.
    private val teamIntroMap: Map<String, String> = mapOf(
        "capstone" to "캡스톤 프로젝트를 함께할 팀원들을 위한 공간입니다. 일정 공유와 과제 관리를 꼼꼼히 해요.",
        "cherish" to "체리시 팀의 소개와 규정이에요. 기획·디자인·개발 역할을 나누어 진행합니다.",
        "mamosari" to "마모사리 팀 소개/규정. 매주 회의와 주간 보고를 필수로 합니다.",
        "megacoffe" to "A+을 위해 혼을 갈아넣을 자들 하루 일정 체크 꼼꼼히 하기 콕을 받으면 확인 콕 남기기 (3회이상 어길 시 엑셀파일정리 과제 담당) + 이번 주 할 일은 매주 일요일 밤에 올리기.",
        "momami" to "모마미 팀 소개/규정. 발표와 자료 제출 일정을 반드시 지켜주세요.",
        "psychology" to "행복의 심리학 팀 규정. 과제 제출일과 발표 순서를 확인하세요.",
        "ai_intro" to "인공지능 입문 팀 스페이스. 실습 과제와 팀 프로젝트 일정을 공유합니다."
    )

    /** 종료 팀플의 종료일: 2026년 2월 20일 이전으로 통일 */
    private val completedTeamEndMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 19, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }

    /** 진행중 팀플 기본 시작일/종료일 (수정 페이지·마감 D-N 표시용) */
    private val defaultOngoingStartMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }
    /** 모든 팀플 종료일 2026년 2월 20일 전 */
    private val defaultOngoingEndMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 19, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }
    /** 종료 팀플 기본 시작일 */
    private val defaultCompletedStartMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }

    /** 팀 7개. 체리시·메가커피 진행중, 캡스톤·마모사리·모마미 종료. 사진은 cherish/megacoffe/momami만. 종료일 전부 2026-02-20 전. */
    private val _allTeams: MutableList<Team> = listOf(
        Team("capstone", "캡스톤", "#FFE970", "", true, 4, 10, teamIntroMap["capstone"] ?: "", defaultCompletedStartMillis, completedTeamEndMillis, completedTeamEndMillis),
        Team("cherish", "체리시", "#F488D4", "cherish_image", false, 4, 2, teamIntroMap["cherish"] ?: "", defaultOngoingStartMillis, defaultOngoingEndMillis, null),
        Team("mamosari", "마모사리", "#D9F592", "", true, 6, 8, teamIntroMap["mamosari"] ?: "", defaultCompletedStartMillis, completedTeamEndMillis, completedTeamEndMillis),
        Team("megacoffe", "메가커피릿", "#FBB0A9", "megacoffe_image", false, 4, 1, teamIntroMap["megacoffe"] ?: "", defaultOngoingStartMillis, defaultOngoingEndMillis, null),
        Team("momami", "모마미", "#90A3ED", "momami_image", true, 4, 3, teamIntroMap["momami"] ?: "", defaultCompletedStartMillis, completedTeamEndMillis, completedTeamEndMillis),
        Team("psychology", "행복의 심리학", "#FFF8D3", "", false, 4, 3, teamIntroMap["psychology"] ?: "", defaultOngoingStartMillis, defaultOngoingEndMillis, null),
        Team("ai_intro", "인공지능 입문", "#FF7A6E", "", false, 6, 7, teamIntroMap["ai_intro"] ?: "", defaultOngoingStartMillis, defaultOngoingEndMillis, null)
    ).toMutableList()

    val allTeams: List<Team> get() = _allTeams

    /** 현재 사용자 이름 (마이페이지·프로필 수정). 앱 실행 중에만 반영. */
    private var currentUserName: String = "이주연"
    fun getCurrentUserName(): String = currentUserName

    /** 현재 사용자 프로필 사진 (마이페이지·프로필 수정). "file:파일명" 또는 drawable 리소스명 또는 "". 앱 실행 중에만 반영. */
    private var currentUserProfileImageResName: String = ""
    fun getCurrentUserProfileImageResName(): String = currentUserProfileImageResName
    fun setCurrentUserProfileImageResName(value: String) { currentUserProfileImageResName = value }

    fun setCurrentUserName(name: String) {
        if (name.isBlank()) return
        val oldName = currentUserName
        currentUserName = name
        updateAllUserNameReferences(oldName, name)
    }
    private fun updateAllUserNameReferences(oldName: String, newName: String) {
        for (i in _allTaskItems.indices) {
            if (_allTaskItems[i].creatorName == oldName) {
                _allTaskItems[i] = _allTaskItems[i].copy(creatorName = newName)
            }
        }
        _teamMembersMap.values.forEach { list ->
            for (i in list.indices) {
                if (list[i].name == oldName) list[i] = list[i].copy(name = newName)
            }
        }
        extraTeamMembers.keys.toList().forEach { teamId ->
            extraTeamMembers[teamId] = (extraTeamMembers[teamId] ?: emptyList()).map { member ->
                if (member.name == oldName) member.copy(name = newName) else member
            }
        }
    }

    /** 팀 삭제 (앱 실행 중에만 반영, 재시작 시 데이터 복구). 팀·해당 팀 일정·추가 팀원 데이터 제거. */
    fun deleteTeam(teamId: String) {
        _allTeams.removeAll { it.id == teamId }
        extraTeamMembers.remove(teamId)
        _allTaskItems.removeAll { it.teamId == teamId }
    }

    /** 팀 정보 수정. 종료 체크 해제 후 저장 시 진행중으로 전환. imageResName이 null이면 기존 유지. */
    fun updateTeam(
        teamId: String,
        name: String,
        intro: String,
        workStartMillis: Long?,
        workEndMillis: Long?,
        setCompleted: Boolean,
        completedAtMillis: Long?,
        imageResName: String? = null
    ) {
        val idx = _allTeams.indexOfFirst { it.id == teamId }
        if (idx < 0) return
        val t = _allTeams[idx]
        val now = System.currentTimeMillis()
        val completedAt = if (setCompleted) (completedAtMillis ?: now) else null
        _allTeams[idx] = t.copy(
            name = name,
            intro = intro,
            imageResName = imageResName ?: t.imageResName,
            workStartMillis = workStartMillis,
            workEndMillis = workEndMillis,
            isCompleted = setCompleted,
            completedAtMillis = completedAt
        )
    }

    /** 새 팀 추가 시 사용하는 팀원 더미 (기존 7개 팀은 teamMembersMap, 이후 추가 팀은 여기) */
    private val extraTeamMembers = mutableMapOf<String, List<TeamMember>>()

    /** 새 팀 추가 (초대코드 참여/팀 생성/일정에서 팀 선택 시 호출). 팀원 4명·일정 더미 함께 생성. 시작/종료일 null이면 기본값 설정. */
    fun addTeam(team: Team) {
        if (_allTeams.any { it.id == team.id }) return
        val toAdd = if (team.workStartMillis == null || team.workEndMillis == null) {
            team.copy(workStartMillis = team.workStartMillis ?: defaultOngoingStartMillis, workEndMillis = team.workEndMillis ?: defaultOngoingEndMillis)
        } else team
        _allTeams.add(toAdd)
        val count = _teamMembersMap[toAdd.id]?.size ?: 4
        val names = koreanNamesPool.drop(toAdd.id.hashCode().and(0x7FFFFFFF) % (koreanNamesPool.size - count).coerceAtLeast(0)).take(count)
            .let { if (it.size >= count) it.take(count) else it + koreanNamesPool.take(count - it.size) }
        extraTeamMembers[toAdd.id] = names.mapIndexed { i, name -> TeamMember("m-${toAdd.id}-$i", name, "ic_user") }
        addDefaultTasksForTeam(toAdd.id, toAdd.name)
    }

    /** 새 팀에 대해 기존 더미와 동일한 규칙으로 일정 다수 생성 (2026-01~02, 주당 3일 등) */
    private fun addDefaultTasksForTeam(teamId: String, teamName: String) {
        val weekDays = listOf(
            listOf(1, 2, 3), listOf(4, 5, 6), listOf(11, 12, 13), listOf(18, 19, 20),
            listOf(25, 26, 27), listOf(1, 2, 3), listOf(8, 9, 10)
        )
        val monthForWeek = listOf(1, 1, 1, 1, 1, 2, 2)
        val members = getTeamMembers(teamId).map { it.name }
        weekDays.forEachIndexed { wi, days ->
            val month = monthForWeek[wi]
            val year = 2026
            days.forEachIndexed { di, day ->
                val creator = members.getOrNull(di % members.size)
                _allTaskItems.add(task(teamId, "$teamName 일정", year, month, day, false, creator))
            }
        }
    }

    private fun isTeamPastDeadline(team: Team): Boolean = team.workEndMillis != null && team.workEndMillis < System.currentTimeMillis()

    /** 나의 팀 스페이스 (홈 하단): 진행중인 팀플 (종료 체크 안 했고 마감일 미지남) */
    fun getMyTeamSpaceTeams(): List<Team> = allTeams.filter { !it.isCompleted && !isTeamPastDeadline(it) }

    /** 참여중인 팀플 (마이페이지). 마감일 지나면 완료로 내려감 */
    fun getParticipatingTeamProjects(): List<Team> = allTeams.filter { !it.isCompleted && !isTeamPastDeadline(it) }

    /** 완료된 팀플 (마이페이지). 종료 체크했거나 마감일 지남 */
    fun getCompletedTeamProjects(): List<Team> = allTeams.filter { it.isCompleted || isTeamPastDeadline(it) }

    /** 캘린더 필터용 팀 목록 (전체 7개) */
    fun getCalendarFilterTeams(): List<Team> = allTeams

    // ---- 팀원 데이터 (팀별 4~5명, 기본 아이콘 ic_user) ----
    private val koreanNamesPool = listOf(
        "이주연", "박성원", "이나영", "최지원", "이태민", "조승민", "김민지", "정하늘", "최유진", "한소희",
        "강현석", "송동현", "김수연", "박민아", "이준호", "최민국", "김태진", "김지민", "이수민", "박서진"
    )

    private val _teamMembersMap: MutableMap<String, MutableList<TeamMember>> = run {
        val teamIds = _allTeams.map { it.id }
        teamIds.mapIndexed { index, teamId ->
            val count = if (index % 3 == 0) 5 else 4
            val names = koreanNamesPool.drop(index * 2).take(5).let { if (it.size >= count) it.take(count) else it + koreanNamesPool.take(count - it.size) }
            teamId to names.mapIndexed { i, name -> TeamMember("m-$teamId-$i", name, "ic_user") }.toMutableList()
        }.toMap().toMutableMap()
    }

    /** 팀 스페이스용: 해당 팀의 팀원 목록 (기존 팀 = _teamMembersMap, 추가 팀 = extraTeamMembers) */
    fun getTeamMembers(teamId: String): List<TeamMember> = _teamMembersMap[teamId] ?: extraTeamMembers[teamId] ?: emptyList()

    fun getTeamIntro(teamId: String): String = getTeamById(teamId)?.intro ?: teamIntroMap[teamId] ?: ""

    // ---- 할일 더미 (팀플별, 팀 스페이스용 creatorName 포함) ----
    private fun task(teamId: String, title: String, year: Int, month: Int, day: Int, checked: Boolean, creatorName: String? = null): TaskItem {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
        }
        return TaskItem(id = "t-$teamId-$year-$month-$day-${title.hashCode()}", teamId = teamId, title = title, date = c, isChecked = checked, creatorName = creatorName)
    }

    private val _allTaskItems: MutableList<TaskItem> = run {
        val list = mutableListOf<TaskItem>()
        val membersMegacoffe = getTeamMembers("megacoffe").map { it.name }
        val membersCapstone = getTeamMembers("capstone").map { it.name }
        val membersCherish = getTeamMembers("cherish").map { it.name }
        // 기존: 체리시·캡스톤 2/4, 2/17
        list.add(task("cherish", "체리시 기획안 업로드", 2026, 2, 4, false, membersCherish.getOrNull(0)))
        list.add(task("cherish", "캐릭터 제작 모델링", 2026, 2, 4, true, membersCherish.getOrNull(1)))
        list.add(task("cherish", "학성지 VOD 3주차 시청", 2026, 2, 4, true, membersCherish.getOrNull(2)))
        list.add(task("cherish", "캐릭터 제작 모델링", 2026, 2, 17, true, membersCherish.getOrNull(0)))
        list.add(task("cherish", "학성지 VOD 3주차 시청", 2026, 2, 17, true, membersCherish.getOrNull(1)))
        list.add(task("capstone", "유니 마감", 2026, 2, 4, false, membersCapstone.getOrNull(0)))
        list.add(task("capstone", "MT", 2026, 2, 4, false, membersCapstone.getOrNull(1)))
        list.add(task("capstone", "캡스톤 초안 형성", 2026, 2, 17, true, membersCapstone.getOrNull(2)))
        list.add(task("capstone", "캐릭터 제작 모델링", 2026, 2, 17, true, membersCapstone.getOrNull(0)))
        list.add(task("capstone", "학성지 VOD 3주차 시청", 2026, 2, 17, true, membersCapstone.getOrNull(1)))
        // 메가커피릿 2/20: 나영 2개, 지원 1개 (캡처본 기준)
        list.add(task("megacoffe", "캡스톤 초안 형성", 2026, 2, 20, false, "나영"))
        list.add(task("megacoffe", "캐릭터 제작 모델링", 2026, 2, 20, true, "나영"))
        list.add(task("megacoffe", "메가커피 아르바이트", 2026, 2, 20, false, "지원"))
        // 메가커피릿 2/11: 일정 2개 (캘린더 동그라미 2)
        list.add(task("megacoffe", "팀 회의", 2026, 2, 11, false, membersMegacoffe.getOrNull(0)))
        list.add(task("megacoffe", "자료 조사", 2026, 2, 11, false, membersMegacoffe.getOrNull(1)))
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
                val members = getTeamMembers(tid).map { it.name }
                list.add(task(tid, titleByTeam[tid] ?: "팀 일정", year, month, day, false, members.getOrNull(di % members.size)))
            }
        }
        list
    }

    val allTaskItems: List<TaskItem> get() = _allTaskItems

    fun getTaskById(id: String): TaskItem? = _allTaskItems.find { it.id == id }
    fun addTask(item: TaskItem) { _allTaskItems.add(item) }
    fun updateTask(item: TaskItem) {
        val idx = _allTaskItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) _allTaskItems[idx] = item
    }

    /** 팀 스페이스용: 해당 팀의 총 일정 개수 */
    fun getTeamScheduleCount(teamId: String): Int = allTaskItems.count { it.teamId == teamId }

    /** 팀 스페이스 캘린더용: 해당 팀의 특정 날짜 일정 개수 */
    fun getDayEventCountForTeam(teamId: String, date: Calendar): Int =
        allTaskItems.count { it.teamId == teamId && it.isOnDate(date) }

    // ---- 일정 없는 팀원 (2026-01 ~ 2026-02, 팀별 날짜당 1~3명 랜덤) ----
    private val noScheduleMembersCache = mutableMapOf<Long, Map<String, List<TeamMember>>>()

    private fun dateKey(cal: Calendar): Long = cal.get(Calendar.YEAR) * 10000L + (cal.get(Calendar.MONTH) + 1) * 100L + cal.get(Calendar.DAY_OF_MONTH)

    fun getNoScheduleMembers(teamId: String, date: Calendar): List<TeamMember> {
        val key = dateKey(date)
        val byTeam = noScheduleMembersCache.getOrPut(key) {
            allTeams.associate { team ->
                team.id to run {
                    val members = getTeamMembers(team.id)
                    if (members.isEmpty()) emptyList()
                    else {
                        val seed = key * 31 + team.id.hashCode()
                        val count = (seed % 3).toInt().coerceIn(1, 3).coerceAtMost(members.size)
                        val start = (seed % members.size).toInt().coerceAtLeast(0)
                        (0 until count).map { i -> members[(start + i) % members.size] }
                    }
                }
            }
        }
        return byTeam[teamId] ?: emptyList()
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

    /** 특정 날짜의 팀 할일 (팀 ID 필터 적용). 체크 상태는 오버라이드 반영. 시작일~종료일 구간에 포함되는 날 모두 표시 */
    fun getTasksForDate(date: Calendar, teamIds: List<String>): List<TaskItem> =
        allTaskItems.filter { it.isOnDate(date) && it.teamId in teamIds }.map { t ->
            t.copy(isChecked = effectiveTaskChecked(t))
        }

    /** 오늘 날짜의 팀 할일 (팀별 그룹용). 체크 상태 오버라이드 반영. 시작일~종료일 구간에 오늘이 포함되면 표시 */
    fun getTodayTasksByTeam(today: Calendar): Map<String, List<TaskItem>> {
        val list = allTaskItems.filter { it.isOnDate(today) }.map { t ->
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
        return PersonalScheduleItem(id = id, title = title, date = c, isLocked = locked, isChecked = checked)
    }

    private val _allPersonalItems: MutableList<PersonalScheduleItem> = run {
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

    val allPersonalItems: List<PersonalScheduleItem> get() = _allPersonalItems

    fun getPersonalById(id: String): PersonalScheduleItem? = _allPersonalItems.find { it.id == id }
    fun addPersonalSchedule(item: PersonalScheduleItem) { _allPersonalItems.add(item) }
    fun updatePersonalSchedule(item: PersonalScheduleItem) {
        val idx = _allPersonalItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) _allPersonalItems[idx] = item
    }

    /** 특정 날짜의 개인일정. 시작일~종료일 구간에 포함되는 날 모두 표시 */
    fun getPersonalForDate(date: Calendar): List<PersonalScheduleItem> =
        allPersonalItems.filter { it.isOnDate(date) }.map { p ->
            p.copy(isLocked = effectivePersonalLocked(p), isChecked = effectivePersonalChecked(p))
        }

    fun getPersonalForToday(today: Calendar): List<PersonalScheduleItem> = getPersonalForDate(today)

    /** 그날 일정 개수 (팀 할일 + 개인일정, 팀 구분 없이). 캘린더 날짜칸 표시용. 시작일~종료일 구간 포함 */
    fun getDayEventCount(date: Calendar, teamIds: List<String>): Int {
        val taskCount = allTaskItems.count { it.isOnDate(date) && it.teamId in teamIds }
        val personalCount = allPersonalItems.count { it.isOnDate(date) }
        return taskCount + personalCount
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
