package com.project.unimate.data.repository

import android.content.Context
import android.util.Log
import com.project.unimate.data.entity.CalendarDayEvent
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.entity.Team
import com.project.unimate.data.entity.TeamMember
import java.util.Calendar
import java.util.Random

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

    /** 종료 팀플 기본 시작일 */
    private val defaultCompletedStartMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 10, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }

    /** 진행중 팀플 기본 시작일 (3월 1일) */
    private val defaultOngoingStartMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }

    /** 새로 추가하는 진행중 팀플 기본 종료일 (3월 31일) */
    private val defaultOngoingEndMillis: Long = run {
        val c = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 31, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.timeInMillis
    }

    /** 종료 팀플 마감일: 2026-02-01 ~ 2026-02-17 구간 내 팀별 겹치지 않게 (2/5, 2/10, 2/15) */
    private val completedTeamEndMillisList: List<Long> = listOf(5, 10, 15).map { day ->
        Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, day, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** 진행중 팀플 마감일: 2026-03-01 ~ 2026-03-31 구간 내 팀별 겹치지 않게 (3/8, 3/15, 3/22, 3/29) */
    private val ongoingTeamEndMillisList: List<Long> = listOf(8, 15, 22, 29).map { day ->
        Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, day, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /** 팀 7개. 체리시·메가커피·행복의심리학·인공지능입문 진행중, 캡스톤·마모사리·모마미 종료. */
    private val _allTeams: MutableList<Team> = buildInitialSeedTeams().toMutableList()

    /** 프로필 등록 직후 서버 시드용: 서버에 생성할 초기 7개 팀 정보 (변경되지 않는 복사본). */
    fun getSeedTeams(): List<Team> = buildInitialSeedTeams()

    private fun buildInitialSeedTeams(): List<Team> {
        val completedEnd0 = completedTeamEndMillisList[0]
        val completedEnd1 = completedTeamEndMillisList[1]
        val completedEnd2 = completedTeamEndMillisList[2]
        val ongoingEnd0 = ongoingTeamEndMillisList[0]
        val ongoingEnd1 = ongoingTeamEndMillisList[1]
        val ongoingEnd2 = ongoingTeamEndMillisList[2]
        val ongoingEnd3 = ongoingTeamEndMillisList[3]
        return listOf(
            Team("capstone", "캡스톤", "#FFE970", "", true, 4, 10, teamIntroMap["capstone"] ?: "", defaultCompletedStartMillis, completedEnd0, completedEnd0),
            Team("cherish", "체리시", "#F488D4", "cherish_image", false, 4, 2, teamIntroMap["cherish"] ?: "", defaultOngoingStartMillis, ongoingEnd0, null),
            Team("mamosari", "마모사리", "#D9F592", "", true, 6, 8, teamIntroMap["mamosari"] ?: "", defaultCompletedStartMillis, completedEnd1, completedEnd1),
            Team("megacoffe", "메가커피릿", "#FBB0A9", "megacoffe_image", false, 4, 1, teamIntroMap["megacoffe"] ?: "", defaultOngoingStartMillis, ongoingEnd1, null),
            Team("momami", "모마미", "#90A3ED", "momami_image", true, 4, 3, teamIntroMap["momami"] ?: "", defaultCompletedStartMillis, completedEnd2, completedEnd2),
            Team("psychology", "행복의 심리학", "#FFF8D3", "", false, 4, 3, teamIntroMap["psychology"] ?: "", defaultOngoingStartMillis, ongoingEnd2, null),
            Team("ai_intro", "인공지능 입문", "#FF7A6E", "", false, 6, 7, teamIntroMap["ai_intro"] ?: "", defaultOngoingStartMillis, ongoingEnd3, null)
        )
    }

    /** 서버에서 받은 내 팀 목록으로 로컬 팀만 교체. 서버 팀의 imageResName이 있으면 그대로 사용(서버 우선), 없을 때만 기존 로컬 팀 사진 유지. 더미 일정(_allTaskItems)은 유지. */
    fun replaceTeamsWithServerData(teams: List<Team>) {
        val preservedTeamImages = _allTeams.associate { it.id to it.imageResName }.filter { (_, v) -> v.isNotBlank() }
        _allTeams.clear()
        _allTeams.addAll(teams)
        for (i in _allTeams.indices) {
            val t = _allTeams[i]
            if (t.imageResName.isBlank()) {
                preservedTeamImages[t.id]?.let { img ->
                    _allTeams[i] = t.copy(imageResName = img)
                }
            }
        }
        extraTeamMembers.clear()
        serverMembersCache.clear()
    }

    /** 저장된 팀 사진(TeamImageStore)을 _allTeams에 적용. replaceTeamsWithServerData 호출 후 호출. */
    fun applyPersistedTeamImages(context: Context) {
        val stored = TeamImageStore.getAll(context)
        for (i in _allTeams.indices) {
            val t = _allTeams[i]
            stored[t.id]?.takeIf { it.isNotBlank() }?.let { img ->
                _allTeams[i] = t.copy(imageResName = img)
            }
        }
    }

    /** 저장된 팀플명(TeamNameStore)을 _allTeams에 적용. replaceTeamsWithServerData 호출 후 호출. */
    fun applyPersistedTeamNames(context: Context) {
        val stored = TeamNameStore.getAll(context)
        for (i in _allTeams.indices) {
            val t = _allTeams[i]
            stored[t.id]?.let { name ->
                _allTeams[i] = t.copy(name = name)
            }
        }
    }

    /** 서버 팀 목록 + 더미(시드) 팀 병합. 삭제한 시드 팀 이름은 제외. 더미가 먼저, 서버 팀이 나중에 오도록 함. */
    fun mergeServerTeamsWithSeed(serverTeams: List<Team>, deletedSeedTeamNames: Set<String> = emptySet()): List<Team> {
        val serverNames = serverTeams.map { it.name }.toSet()
        val seedOnly = getSeedTeams().filter { it.name !in serverNames && it.name !in deletedSeedTeamNames }
        return seedOnly + serverTeams
    }

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

    /** 서버에서 로드한 팀원 캐시 (teamId → 서버 팀원 목록). getTeamMembers()에서 더미와 merge됨 (feat/#33) */
    private val serverMembersCache = mutableMapOf<String, List<TeamMember>>()

    /** 새 팀 추가 (초대코드 참여/팀 생성/일정에서 팀 선택 시 호출). 현재 사용자 포함 팀원·일정 더미 생성. */
    fun addTeam(team: Team) {
        if (_allTeams.any { it.id == team.id }) return
        val toAdd = if (team.workStartMillis == null || team.workEndMillis == null) {
            team.copy(workStartMillis = team.workStartMillis ?: defaultOngoingStartMillis, workEndMillis = team.workEndMillis ?: defaultOngoingEndMillis)
        } else team
        _allTeams.add(toAdd)
        val count = 4
        val others = koreanNamesPool.filter { it != currentUserName }.drop(toAdd.id.hashCode().and(0x7FFFFFFF) % 10).take(count - 1)
            .let { if (it.size >= count - 1) it.take(count - 1) else it + koreanNamesPool.filter { n -> n != currentUserName }.take(count - 1 - it.size) }
        extraTeamMembers[toAdd.id] = listOf(TeamMember("me", currentUserName, "ic_user")) +
            others.mapIndexed { i, name -> TeamMember("m-${toAdd.id}-$i", name, "ic_user") }
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

    /** 팀 스페이스용: 해당 팀의 팀원 목록. 서버 캐시 + 더미 merge 후, 'me' 멤버는 currentUserName으로 보정·중복 제거. */
    fun getTeamMembers(teamId: String): List<TeamMember> {
        val dummy = _teamMembersMap[teamId] ?: extraTeamMembers[teamId] ?: emptyList()
        val server = serverMembersCache[teamId] ?: emptyList()
        val merged = if (server.isEmpty()) dummy else mergeServerFirstMembers(server, dummy)
        return applyMeAndCurrentUserName(merged)
    }

    /** 서버 팀원을 캐시에 저장. noScheduleMembersCache는 자동 무효화. */
    fun cacheServerMembers(teamId: String, members: List<TeamMember>) {
        serverMembersCache[teamId] = members
        noScheduleMembersCache.clear()
    }

    /** 서버 팀원을 앞에, 더미 팀원 중 이름 미중복인 것을 뒤에 붙여 반환 */
    fun mergeServerFirstMembers(server: List<TeamMember>, dummy: List<TeamMember>): List<TeamMember> {
        val serverNames = server.map { it.name }.toSet()
        return server + dummy.filter { it.name !in serverNames }
    }

    /** 팀원 목록에 'me' 보정 및 currentUserName 중복 제거 적용 */
    private fun applyMeAndCurrentUserName(list: List<TeamMember>): List<TeamMember> {
        val withoutCurrentName = list.filter { it.name != currentUserName }
        val myImage = currentUserProfileImageResName.ifBlank { "ic_user" }
        return if (list.any { it.id == "me" }) {
            list.map { if (it.id == "me") it.copy(name = currentUserName, iconResName = myImage) else it }
                .filter { it.id == "me" || it.name != currentUserName }
        } else {
            listOf(TeamMember("me", currentUserName, myImage)) + withoutCurrentName
        }
    }

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
        // 진행중 팀플(체리시, 메가커피릿, 행복의 심리학, 인공지능 입문)만 2026년 3월 15일까지 더미 일정 추가 (요일 다양: 월·수·금·일·화·목·토)
        val ongoingTeamIds = listOf("cherish", "megacoffe", "psychology", "ai_intro")
        val marchDays = listOf(2, 4, 6, 8, 10, 12, 14, 15) // 3/2(월), 3/4(수), 3/6(금), 3/8(일), 3/10(화), 3/12(목), 3/14(토), 3/15(일)
        marchDays.forEachIndexed { wi, day ->
            ongoingTeamIds.forEachIndexed { ti, tid ->
                val members = getTeamMembers(tid).map { it.name }
                list.add(task(tid, titleByTeam[tid] ?: "팀 일정", 2026, 3, day, false, members.getOrNull((wi + ti) % members.size)))
            }
        }
        list
    }

    val allTaskItems: List<TaskItem> get() = _allTaskItems

    /** 저장된 팀 목록·팀/개인 일정 복원. 앱 시작 시 MainActivity에서 호출. */
    fun loadSchedulesFrom(context: Context) {
        TeamStore.loadTeams(context)?.takeIf { it.isNotEmpty() }?.let { loaded ->
            replaceTeamsWithServerData(loaded)
            applyPersistedTeamImages(context)
            applyPersistedTeamNames(context)
        }
        ScheduleStore.loadTaskItems(context)?.takeIf { it.isNotEmpty() }?.let {
            _allTaskItems.clear()
            _allTaskItems.addAll(it)
        }
        ScheduleStore.loadPersonalItems(context)?.takeIf { it.isNotEmpty() }?.let {
            _allPersonalItems.clear()
            _allPersonalItems.addAll(it)
        }
    }

    /** 팀 목록·팀/개인 일정 전체 저장. sync 후·추가·수정 후 UI에서 호출. */
    fun saveSchedulesTo(context: Context) {
        TeamStore.saveTeams(context, _allTeams)
        ScheduleStore.saveTaskItems(context, _allTaskItems)
        ScheduleStore.savePersonalItems(context, _allPersonalItems)
    }

    fun getTaskById(id: String): TaskItem? = _allTaskItems.find { it.id == id }
    fun addTask(item: TaskItem) { _allTaskItems.add(item) }
    fun updateTask(item: TaskItem) {
        val idx = _allTaskItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) _allTaskItems[idx] = item
    }
    fun removeTaskById(id: String) { _allTaskItems.removeAll { it.id == id } }

    /** 서버에서 팀 일정을 불러온 뒤 해당 팀의 일정만 교체할 때 사용. (재설치 후 복구용) */
    fun replaceTasksForTeam(teamId: String, newTasks: List<TaskItem>) {
        _allTaskItems.removeAll { it.teamId == teamId }
        _allTaskItems.addAll(newTasks)
    }

    /** 서버에서 받은 전체 팀 일정으로 완전 교체 (모든 팀의 일정을 한 번에 서버 데이터로 덮어씀) */
    fun replaceSchedulesWithServerData(serverSchedules: List<TaskItem>) {
        _allTaskItems.clear()
        _allTaskItems.addAll(serverSchedules)
        Log.d("DummyRepository", "일정 데이터 교체: ${serverSchedules.size}개")
    }

    /** 팀 스페이스용: 해당 팀의 총 일정 개수 */
    fun getTeamScheduleCount(teamId: String): Int = allTaskItems.count { it.teamId == teamId }

    /** 팀 스페이스 캘린더용: 해당 팀의 특정 날짜 일정 개수 */
    fun getDayEventCountForTeam(teamId: String, date: Calendar): Int =
        allTaskItems.count { it.teamId == teamId && it.isOnDate(date) }

    // ---- 일정 없는 팀원 (2026-02-01 ~ 2026-03-31, 모든 팀: 주당 4일 이상 표시, 그날 최소 전체 팀원 1/3명) ----
    private val noScheduleMembersCache = mutableMapOf<Long, Map<String, List<TeamMember>>>()

    private fun dateKey(cal: Calendar): Long = cal.get(Calendar.YEAR) * 10000L + (cal.get(Calendar.MONTH) + 1) * 100L + cal.get(Calendar.DAY_OF_MONTH)

    /** 해당 날짜가 2026-02-01 ~ 2026-03-31 구간인지 */
    private fun isInNoScheduleRange(cal: Calendar): Boolean {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        if (y != 2026) return false
        if (m < 2 || m > 3) return false
        if (m == 2 && d < 1) return false
        if (m == 3 && d > 31) return false
        return true
    }

    /** 해당 주에서 일정 없는 팀원 카드를 보여줄 4개의 요일(1~7)을 결정. 팀·주별로 동일한 결과. */
    private fun getNoScheduleDaysOfWeekForWeek(teamId: String, cal: Calendar): Set<Int> {
        val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val seed = teamId.hashCode().toLong() * 31 + year * 53L + weekOfYear
        val rnd = Random(seed)
        return (1..7).toList().shuffled(rnd).take(4).toSet()
    }

    fun getNoScheduleMembers(teamId: String, date: Calendar): List<TeamMember> {
        if (!isInNoScheduleRange(date)) return emptyList()
        val key = dateKey(date)
        val byTeam = noScheduleMembersCache.getOrPut(key) {
            allTeams.associate { team ->
                team.id to run {
                    val members = getTeamMembers(team.id)
                    if (members.isEmpty()) emptyList()
                    else {
                        val noScheduleDays = getNoScheduleDaysOfWeekForWeek(team.id, date)
                        val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)
                        if (dayOfWeek !in noScheduleDays) emptyList()
                        else {
                            val minCount = maxOf(1, (members.size + 2) / 3)
                            val memberSeed = key * 31 + team.id.hashCode()
                            val rnd = Random(memberSeed)
                            val start = rnd.nextInt(members.size)
                            (0 until minCount).map { i -> members[(start + i) % members.size] }
                        }
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

    fun setTaskChecked(taskId: String, checked: Boolean) {
        taskCheckedOverrides[taskId] = checked
        val idx = _allTaskItems.indexOfFirst { it.id == taskId }
        if (idx >= 0) _allTaskItems[idx] = _allTaskItems[idx].copy(isChecked = checked)
    }
    fun setPersonalLocked(personalId: String, locked: Boolean) {
        personalLockedOverrides[personalId] = locked
        val idx = _allPersonalItems.indexOfFirst { it.id == personalId }
        if (idx >= 0) _allPersonalItems[idx] = _allPersonalItems[idx].copy(isLocked = locked)
    }
    fun setPersonalChecked(personalId: String, checked: Boolean) {
        personalCheckedOverrides[personalId] = checked
        val idx = _allPersonalItems.indexOfFirst { it.id == personalId }
        if (idx >= 0) _allPersonalItems[idx] = _allPersonalItems[idx].copy(isChecked = checked)
    }

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
    fun removePersonalById(id: String) { _allPersonalItems.removeAll { it.id == id } }
    fun updatePersonalSchedule(item: PersonalScheduleItem) {
        val idx = _allPersonalItems.indexOfFirst { it.id == item.id }
        if (idx >= 0) _allPersonalItems[idx] = item
    }

    /** 서버에서 개인 일정을 불러온 뒤, 서버 출처 항목만 제거하고 새 목록으로 채움. (재설치 후 복구용) */
    fun replacePersonalSchedulesFromServer(serverItems: List<PersonalScheduleItem>) {
        _allPersonalItems.removeAll { it.id.startsWith("p-server-") }
        _allPersonalItems.addAll(serverItems)
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
