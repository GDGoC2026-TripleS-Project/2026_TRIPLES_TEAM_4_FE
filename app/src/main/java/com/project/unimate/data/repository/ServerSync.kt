package com.project.unimate.data.repository

import android.content.Context
import android.util.Log
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.entity.Team
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamSummaryResponse
import com.project.unimate.network.dto.TeamsListResponse
import com.project.unimate.network.service.MyScheduleService
import com.project.unimate.network.service.TeamScheduleService
import com.project.unimate.network.service.TeamService
import com.project.unimate.network.service.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 로그인/앱 시작 시 서버에서 유저·팀·팀일정·개인일정 로드 후 DummyRepository·로컬 저장소 반영.
 * Mutex로 동시 호출 직렬화(ConcurrentModificationException 방지).
 */
object ServerSync {

    private const val TAG = "ServerSync"
    private val syncMutex = Mutex()

    private fun alarmLabelFromMinutes(minutes: Int?): String {
        return when (minutes) {
            5 -> "5분 전"
            15 -> "15분 전"
            30 -> "30분 전"
            60 -> "1시간 전"
            else -> "없음"
        }
    }

    suspend fun syncFromServer(context: Context) {
        syncMutex.withLock {
            syncFromServerInternal(context)
        }
    }

    private suspend fun syncFromServerInternal(context: Context) {
        val ctx = context.applicationContext
        try {
            Log.d(TAG, "syncFromServer start")
            // 1) 유저 정보 (이름, 프로필 이미지)
            val userService = RetrofitClient.create<UserService>(ctx)
            val userResp = withContext(Dispatchers.IO) { userService.getMyInfo() }
            if (!userResp.isSuccessful) {
                Log.w(TAG, "getMyInfo failed: ${userResp.code()} ${userResp.message()}")
            }
            if (userResp.isSuccessful) {
                val me = userResp.body()
                me?.nickname?.takeIf { it.isNotBlank() }?.let { nick ->
                    withContext(Dispatchers.Main) {
                        DummyRepository.setCurrentUserName(nick)
                        NicknameStore.save(ctx, nick)
                    }
                }
                me?.profileImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    withContext(Dispatchers.IO) {
                        try {
                            val bytes = URL(url).openStream().readBytes()
                            val file = File(ctx.filesDir, "profile_server.jpg")
                            file.writeBytes(bytes)
                            val resName = "file:${file.name}"
                            withContext(Dispatchers.Main) {
                                ProfileImageStore.save(ctx, resName)
                                DummyRepository.setCurrentUserProfileImageResName(resName)
                            }
                        } catch (_: Exception) { }
                    }
                }
            }

            // 팀 목록(서버 응답: 배열 또는 content/data/teams 래핑 둘 다 파싱)
            val teamService = RetrofitClient.create<TeamService>(ctx)
            val teamResp = withContext(Dispatchers.IO) { teamService.getMyTeams() }
            if (!teamResp.isSuccessful) {
                Log.w(TAG, "getMyTeams failed: ${teamResp.code()} ${teamResp.message()}")
            }
            if (teamResp.isSuccessful) {
                val rawList = teamResp.body()?.listOrEmpty() ?: emptyList()
                val deletedUserIds = DeletedUserTeamStore.getDeletedIds(ctx)
                val serverTeams = rawList
                    .filter { r -> r.id?.toString() !in deletedUserIds }
                    .mapNotNull { teamSummaryToTeam(it) }
                val deletedNames = DeletedSeedTeamStore.getDeletedNames(ctx)
                val merged = DummyRepository.mergeServerTeamsWithSeed(serverTeams, deletedNames)
                val seedIds = DummyRepository.getSeedTeams().map { it.id }.toSet()
                val withOverrides = SeedTeamOverridesStore.applyOverrides(ctx, merged, seedIds)
                withContext(Dispatchers.Main) {
                    DummyRepository.replaceTeamsWithServerData(withOverrides)
                    DummyRepository.applyPersistedTeamImages(ctx)
                    DummyRepository.applyPersistedTeamNames(ctx)
                }
                Log.d(TAG, "getMyTeams applied: ${serverTeams.size} server teams, merged=${merged.size}")
            }
            // 팀 목록 스냅샷으로 순회(동시 수정 예외 방지)
            val teamsSnapshot = withContext(Dispatchers.Main) { DummyRepository.allTeams.toList() }

            // 팀 일정
            val scheduleService = RetrofitClient.create<TeamScheduleService>(ctx)
            for (team in teamsSnapshot) {
                val numericTeamId = team.id.toLongOrNull() ?: continue
                val listResp = withContext(Dispatchers.IO) {
                    scheduleService.getByRange(numericTeamId, "2025-01-01", "2026-12-31")
                }
                if (!listResp.isSuccessful) continue
                val serverList = listResp.body() ?: continue
                val taskItems = serverList.mapNotNull { s ->
                    val sid = s.id ?: return@mapNotNull null
                    val startMs = parseIsoToMillis(s.startAt)
                    val endMs = parseIsoToMillis(s.endAt)
                    if (startMs == null || endMs == null) return@mapNotNull null
                    val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                    TaskItem(
                        id = "t-${team.id}-$sid",
                        teamId = team.id,
                        title = s.title ?: "",
                        date = cal,
                        startTimeMillis = startMs,
                        endTimeMillis = endMs,
                        isChecked = false,
                        creatorName = null,
                        notificationCategory = alarmLabelFromMinutes(s.alarmMinutes)
                    )
                }
                withContext(Dispatchers.Main) {
                    DummyRepository.replaceTasksForTeam(team.id, taskItems)
                }
            }

            // 개인 일정
            val myScheduleService = RetrofitClient.create<MyScheduleService>(ctx)
            val allPersonalFromServer = mutableListOf<PersonalScheduleItem>()
            for (team in teamsSnapshot) {
                val numericTeamId = team.id.toLongOrNull() ?: continue
                val markedResp = withContext(Dispatchers.IO) {
                    myScheduleService.getMarkedDates(numericTeamId, "2025-01-01", "2026-12-31")
                }
                if (!markedResp.isSuccessful) continue
                val dates = markedResp.body()?.markedDates?.take(60) ?: continue
                for (dateStr in dates) {
                    val dayResp = withContext(Dispatchers.IO) {
                        myScheduleService.getDaySchedules(numericTeamId, dateStr)
                    }
                    val dayList = dayResp.body() ?: continue
                    for (s in dayList) {
                        val sid = s.id ?: continue
                        val startMs = parseIsoToMillis(s.startAt)
                        val endMs = parseIsoToMillis(s.endAt)
                        if (startMs == null || endMs == null) continue
                        val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                        allPersonalFromServer.add(
                            PersonalScheduleItem(
                                id = "p-server-$sid",
                                title = s.title ?: "",
                                date = cal,
                                startTimeMillis = startMs,
                                endTimeMillis = endMs,
                                isLocked = s.isPrivate == true,
                                isChecked = false,
                                notificationCategory = alarmLabelFromMinutes(s.alarmMinutes),
                                scheduleCategory = "없음"
                            )
                        )
                    }
                }
            }
            if (allPersonalFromServer.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    DummyRepository.replacePersonalSchedulesFromServer(allPersonalFromServer)
                }
            }
            Log.d(TAG, "syncFromServer done")
        } catch (e: Exception) {
            Log.e(TAG, "syncFromServer failed", e)
        } finally {
            withContext(Dispatchers.Main) {
                DummyRepository.saveSchedulesTo(ctx)
            }
        }
    }

    private fun parseIsoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val s = iso.replace("Z", "").trim()
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).parse(s)?.time
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(s)?.time
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s)?.time
        } catch (_: Exception) { null }
    }

    private fun teamSummaryToTeam(r: TeamSummaryResponse): Team? {
        val id = r.id ?: return null
        val completed = r.completed == true || r.isCompleted == true
        val endMillis = parseIsoToMillis(r.endAt)
        val startMillis = parseIsoToMillis(r.startAt)
        return Team(
            id = id.toString(),
            name = r.name ?: "",
            colorHex = r.colorHex ?: r.color ?: "#cccccc",
            imageResName = r.imageUrl?.takeIf { it.isNotBlank() } ?: "",
            isCompleted = completed,
            memberCount = (r.memberCount ?: 0).toInt(),
            deadlineDays = null,
            intro = r.description ?: "",
            workStartMillis = startMillis,
            workEndMillis = endMillis,
            completedAtMillis = if (completed) endMillis else null
        )
    }
}
