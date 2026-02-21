package com.project.unimate.ui.timepick

// 역할: 모이기 확정·일정 생성. poll detail 기반 최적 시간·TeamScheduleService 생성

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.repository.DummyRepository
import android.util.Log
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.MemberVoteDto
import com.project.unimate.network.dto.SlotDefinitionResponse
import com.project.unimate.network.dto.TeamScheduleCreateRequest
import com.project.unimate.network.service.SchedulePollService
import com.project.unimate.network.service.TeamScheduleService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditTimepickFragment : Fragment() {

    private var editStartCalendar = Calendar.getInstance()
    private var editEndCalendar = Calendar.getInstance()
    private var notificationMinutesBefore: Int? = null

    private fun hourToLabel(hour: Int): String = when (hour) {
        0 -> "오전 12시"
        in 1..11 -> "오전 ${hour}시"
        12 -> "오후 12시"
        else -> "오후 ${hour - 12}시"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_edit_timepick, container, false)
        val cancelBtn = root.findViewById<TextView>(R.id.editTimepickCancel)
        val saveBtn = root.findViewById<TextView>(R.id.editTimepickSave)
        val grid = root.findViewById<TimepickGridView>(R.id.editTimepickGrid)
        val strip = root.findViewById<TimepickTimeLabelStrip>(R.id.editTimepickLabelStrip)
        val scrollWrapper = root.findViewById<LinearLayout>(R.id.editTimepickScrollWrapper)
        val contentRow = root.findViewById<LinearLayout>(R.id.editTimepickContentRow)
        val timepickScroll = root.findViewById<HorizontalScrollView>(R.id.editTimepickScroll)
        val gridWrapper = root.findViewById<View>(R.id.editTimepickGridWrapper)
        val scheduleName = root.findViewById<EditText>(R.id.editTimepickScheduleName)
        val startDateTv = root.findViewById<TextView>(R.id.editTimepickStartDate)
        val startTimeTv = root.findViewById<TextView>(R.id.editTimepickStartTime)
        val endDateTv = root.findViewById<TextView>(R.id.editTimepickEndDate)
        val endTimeTv = root.findViewById<TextView>(R.id.editTimepickEndTime)
        val notificationBtn = root.findViewById<TextView>(R.id.editTimepickNotificationBtn)

        val taskId = arguments?.getString("taskId")
        val teamId = TimepickStateHolder.teamId.ifEmpty { DummyRepository.getTaskById(taskId ?: "")?.teamId ?: "" }
        val team = DummyRepository.getTeamById(teamId)
        val existingTask = taskId?.let { DummyRepository.getTaskById(it) }

        val dates = TimepickStateHolder.displayDates.ifEmpty {
            existingTask?.let { listOf(TimepickStateHolder.dayStartMillis(it.date)) } ?: listOf(Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis)
        }
        val startHour = TimepickStateHolder.globalStartHour
        val endHour = TimepickStateHolder.globalEndHour
        val confirmedIntersection = TimepickStateHolder.confirmedIntersection

        if (existingTask != null) {
            scheduleName.setText(existingTask.title)
            editStartCalendar.timeInMillis = existingTask.startTimeMillis
            editEndCalendar.timeInMillis = existingTask.endTimeMillis
        } else {
            val firstDate = dates.firstOrNull() ?: System.currentTimeMillis()
            editStartCalendar = Calendar.getInstance().apply { timeInMillis = firstDate; set(Calendar.HOUR_OF_DAY, 13); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            editEndCalendar = Calendar.getInstance().apply { timeInMillis = editStartCalendar.timeInMillis; add(Calendar.HOUR_OF_DAY, 1) }
        }

        grid.dayCount = dates.size
        grid.startHour = startHour
        grid.endHour = endHour
        grid.dateHeaders = dates.map { dayMillis ->
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            "${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.DAY_OF_MONTH)}" to
                listOf("일", "월", "화", "수", "목", "금", "토")[cal.get(Calendar.DAY_OF_WEEK) - 1]
        }
        grid.drawTimeLabels = false
        grid.timeLabels = (startHour..endHour).map { hourToLabel(it) }
        grid.isReadOnly = true
        strip.timeLabels = (startHour..endHour).map { hourToLabel(it) }
        strip.startHour = startHour
        strip.endHour = endHour
        grid.viewTreeObserver.addOnGlobalLayoutListener {
            if (grid.height > 0 && strip.layoutParams.height != grid.height) {
                strip.layoutParams = strip.layoutParams.apply { height = grid.height }
                strip.requestLayout()
            }
        }
        if (confirmedIntersection.isNotEmpty()) {
            grid.cellCounts = confirmedIntersection.associateWith { 5 }
        }

        val dayCount = dates.size
        val density = resources.displayMetrics.density
        val pad16 = (16 * density).toInt()
        val pad28 = (28 * density).toInt()
        scrollWrapper.setPadding(
            pad16, scrollWrapper.paddingTop,
            if (dayCount < 7) pad28 else pad16,
            scrollWrapper.paddingBottom
        )
        scrollWrapper.gravity = Gravity.CENTER_HORIZONTAL
        (contentRow.layoutParams as LinearLayout.LayoutParams).width = if (dayCount < 7) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT
        (timepickScroll.layoutParams as LinearLayout.LayoutParams).apply {
            width = if (dayCount < 7) LinearLayout.LayoutParams.WRAP_CONTENT else 0
            weight = if (dayCount < 7) 0f else 1f
            marginStart = 0
            marginEnd = 0
        }
        scrollWrapper.requestLayout()
        (gridWrapper.layoutParams as FrameLayout.LayoutParams).width = if (dayCount < 7) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT

        val confirmedBlocks = buildConfirmedBlocks(confirmedIntersection)
        if (existingTask == null && confirmedBlocks.isNotEmpty()) {
            val (dIdx, slotRange) = confirmedBlocks.first()
            val dayMillis = dates.getOrNull(dIdx) ?: dates.first()
            val startSlot = slotRange.first
            val endSlotInclusive = slotRange.last
            val startHourActual = (startHour + startSlot / 2).coerceIn(0, 23)
            val startMinute = (startSlot % 2) * 30
            val endHourActual = (startHour + (endSlotInclusive + 1) / 2).coerceIn(0, 24)
            val endMinute = ((endSlotInclusive + 1) % 2) * 30
            editStartCalendar = Calendar.getInstance().apply {
                timeInMillis = dayMillis
                set(Calendar.HOUR_OF_DAY, startHourActual)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            editEndCalendar = Calendar.getInstance().apply {
                timeInMillis = dayMillis
                set(Calendar.HOUR_OF_DAY, if (endHourActual >= 24) 23 else endHourActual)
                set(Calendar.MINUTE, if (endHourActual >= 24) 59 else endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        fun refreshStartEndDisplay() {
            startDateTv.text = "${editStartCalendar.get(Calendar.YEAR)}. ${editStartCalendar.get(Calendar.MONTH) + 1}. ${editStartCalendar.get(Calendar.DAY_OF_MONTH)}"
            startTimeTv.text = formatTime(editStartCalendar)
            endDateTv.text = "${editEndCalendar.get(Calendar.YEAR)}. ${editEndCalendar.get(Calendar.MONTH) + 1}. ${editEndCalendar.get(Calendar.DAY_OF_MONTH)}"
            endTimeTv.text = formatTime(editEndCalendar)
            updateGridFromDates(grid)
        }

        updateGridFromDates(grid)
        grid.onCellClick = { dayIndex, slotIndex ->
            val block = confirmedBlocks.find { (d, range) -> d == dayIndex && slotIndex in range }
            block?.let { (dIdx, slotRange) ->
                val dayMillis = dates.getOrNull(dIdx) ?: return@let
                val startSlot = slotRange.first
                val endSlotInclusive = slotRange.last
                val startHourActual = (startHour + startSlot / 2).coerceIn(0, 23)
                val startMinute = (startSlot % 2) * 30
                val endHourActual = (startHour + (endSlotInclusive + 1) / 2).coerceIn(0, 24)
                val endMinute = ((endSlotInclusive + 1) % 2) * 30
                editStartCalendar = Calendar.getInstance().apply {
                    timeInMillis = dayMillis
                    set(Calendar.HOUR_OF_DAY, startHourActual)
                    set(Calendar.MINUTE, startMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                editEndCalendar = Calendar.getInstance().apply {
                    timeInMillis = dayMillis
                    set(Calendar.HOUR_OF_DAY, if (endHourActual >= 24) 23 else endHourActual)
                    set(Calendar.MINUTE, if (endHourActual >= 24) 59 else endMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                refreshStartEndDisplay()
            }
        }

        refreshStartEndDisplay()

        // 신규 확정 시 참여자 투표 기반 최적 시간 자동 계산
        if (existingTask == null) {
            loadAndPickConfirmedTime { refreshStartEndDisplay() }
        }

        val notificationOptions = arrayOf(getString(R.string.none), "5분 전", "15분 전", "30분 전", "1시간 전")
        val notificationValues = arrayOf<Int?>(null, 5, 15, 30, 60)
        val notificationDropdown = root.findViewById<View>(R.id.editTimepickNotificationDropdown)
        val notificationItemIds = intArrayOf(
            R.id.editTimepickNotificationItem0,
            R.id.editTimepickNotificationItem1,
            R.id.editTimepickNotificationItem2,
            R.id.editTimepickNotificationItem3,
            R.id.editTimepickNotificationItem4
        )
        val selectedBg = ContextCompat.getColor(requireContext(), R.color.option_selected_bg)
        fun refreshNotificationDropdownHighlight() {
            val selectedIndex = notificationValues.indexOf(notificationMinutesBefore)
            notificationItemIds.forEachIndexed { index, id ->
                (root.findViewById<View>(id) as? TextView)?.setBackgroundColor(
                    if (index == selectedIndex) selectedBg else android.graphics.Color.TRANSPARENT
                )
            }
        }
        notificationItemIds.forEachIndexed { index, id ->
            (root.findViewById<View>(id) as? TextView)?.setOnClickListener {
                notificationMinutesBefore = notificationValues[index]
                notificationBtn.text = notificationOptions[index]
                notificationDropdown.visibility = View.GONE
            }
        }
        notificationBtn.setOnClickListener {
            if (notificationDropdown.visibility == View.VISIBLE) {
                notificationDropdown.visibility = View.GONE
            } else {
                refreshNotificationDropdownHighlight()
                notificationDropdown.visibility = View.VISIBLE
            }
        }

        cancelBtn.setOnClickListener { closeAfterEdit() }
        saveBtn.setOnClickListener {
            val title = scheduleName.text.toString().ifBlank { "${team?.name ?: ""} 회의" }
            val notificationLabel = notificationBtn.text?.toString().orEmpty()
            if (existingTask != null) {
                val updated = existingTask.copy(
                    title = title,
                    date = editStartCalendar,
                    startTimeMillis = editStartCalendar.timeInMillis,
                    endTimeMillis = editEndCalendar.timeInMillis
                )
                DummyRepository.updateTask(updated)
                DummyRepository.saveSchedulesTo(requireContext())
                navigateToHome()
            } else if (teamId.isNotEmpty()) {
                val numericTeamId = teamId.toLongOrNull()
                if (numericTeamId == null) {
                    val task = TaskItem(
                        id = "timepick-${teamId}-${System.currentTimeMillis()}",
                        teamId = teamId,
                        title = title,
                        date = editStartCalendar,
                        startTimeMillis = editStartCalendar.timeInMillis,
                        endTimeMillis = editEndCalendar.timeInMillis,
                        isChecked = false,
                        creatorName = null,
                        notificationCategory = notificationLabel.ifBlank { "없음" }
                    )
                    DummyRepository.addTask(task)
                    DummyRepository.saveSchedulesTo(requireContext())
                    navigateToHome()
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val serverId = createTeamScheduleOnServer(
                        teamId = numericTeamId,
                        title = title,
                        alarmMinutes = alarmMinutesFromLabel(notificationLabel)
                    )
                    if (serverId == null) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "일정 저장에 실패했어요. 다시 시도해주세요.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val task = TaskItem(
                        id = "t-${teamId}-${serverId}",
                        teamId = teamId,
                        title = title,
                        date = editStartCalendar,
                        startTimeMillis = editStartCalendar.timeInMillis,
                        endTimeMillis = editEndCalendar.timeInMillis,
                        isChecked = false,
                        creatorName = null,
                        notificationCategory = notificationLabel.ifBlank { "없음" }
                    )
                    DummyRepository.addTask(task)
                    DummyRepository.saveSchedulesTo(requireContext())
                    navigateToHome()
                }
            } else {
                navigateToHome()
            }
        }

        return root
    }

    private suspend fun createTeamScheduleOnServer(
        teamId: Long,
        title: String,
        alarmMinutes: Int?
    ): Long? {
        return try {
            val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val service = RetrofitClient.create<TeamScheduleService>(requireContext())
            val response = service.create(
                teamId,
                TeamScheduleCreateRequest(
                    title = title,
                    startAt = isoFmt.format(editStartCalendar.time),
                    endAt = isoFmt.format(editEndCalendar.time),
                    category = "MEETING",
                    alarmMinutes = alarmMinutes
                )
            )
            if (response.isSuccessful) response.body()?.id else null
        } catch (_: Exception) {
            null
        }
    }

    private fun alarmMinutesFromLabel(label: String): Int? {
        return when (label.trim()) {
            "5분 전" -> 5
            "15분 전" -> 15
            "30분 전" -> 30
            "1시간 전" -> 60
            else -> null
        }
    }

    /** 저장 완료 후 홈으로 이동. 모이기 플로우 백스택 전부 제거 */
    private fun navigateToHome() {
        TimepickStateHolder.clear()
        findNavController().navigate(
            R.id.homeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.homeFragment, true)
                .build()
        )
    }

    private fun closeAfterEdit() {
        val nav = findNavController()
        val poppedToCreate = nav.popBackStack(R.id.createTimepickFragment, true)
        if (poppedToCreate) return
        val poppedOne = nav.popBackStack()
        if (!poppedOne) {
            nav.navigate(R.id.calendarFragment)
        }
    }

    private fun formatTime(cal: Calendar): String {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val ampm = if (h < 12) "오전" else "오후"
        val hour = if (h == 0) 12 else if (h <= 12) h else h - 12
        return "$ampm $hour:${m.toString().padStart(2, '0')}"
    }

    private fun updateGridFromDates(grid: TimepickGridView) {
        val slotCount = (grid.endHour - grid.startHour) * 2
        val startH = editStartCalendar.get(Calendar.HOUR_OF_DAY) - grid.startHour
        val startM = editStartCalendar.get(Calendar.MINUTE)
        val endH = editEndCalendar.get(Calendar.HOUR_OF_DAY) - grid.startHour
        val endM = editEndCalendar.get(Calendar.MINUTE)
        val startSlot = (startH * 2 + if (startM >= 30) 1 else 0).coerceIn(0, slotCount - 1)
        val endSlotExclusive = (endH * 2 + if (endM > 30) 2 else if (endM > 0) 1 else 0).coerceIn(0, slotCount)
        val dayStartMillis = TimepickStateHolder.dayStartMillis(editStartCalendar)
        val dates = TimepickStateHolder.displayDates
        val dayIndex = dates.indexOfFirst { it == dayStartMillis }.takeIf { it >= 0 } ?: 0
        grid.selectedCells.clear()
        for (s in startSlot until endSlotExclusive) {
            grid.selectedCells.add(dayIndex to s)
        }
        if (TimepickStateHolder.confirmedIntersection.isEmpty()) {
            grid.cellCounts = (startSlot until endSlotExclusive).associate { s -> (dayIndex to s) to 1 }
        }
        grid.invalidate()
    }

    /** 서버 poll detail 기반 votesByMember·slotDefinitions로 최적 구간 선택 후 editStart/EndCalendar 갱신. [onUpdated]로 UI 재그리기 */
    private fun loadAndPickConfirmedTime(onUpdated: () -> Unit) {
        val pollId = TimepickStateHolder.pollId ?: run {
            Log.w("ConfirmDebug", "pollId=null → API 조회 불가, fallback 기본값(13:00) 유지")
            return
        }
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("ConfirmDebug", "==============================")
                Log.d("ConfirmDebug", "loadAndPickConfirmedTime 시작: pollId=$pollId")
                val service  = RetrofitClient.create<SchedulePollService>(ctx)
                val response = service.getDetail(pollId)

                if (!response.isSuccessful) {
                    Log.w("ConfirmDebug", "getDetail 실패 code=${response.code()} → fallback")
                    return@launch
                }
                val detail = response.body() ?: run {
                    Log.w("ConfirmDebug", "getDetail body=null → fallback")
                    return@launch
                }

                val slotMinutes   = detail.slotMinutes ?: 30
                val requiredSlots = (60 / slotMinutes).coerceAtLeast(1)
                val slotDefs      = detail.slotDefinitions.orEmpty()
                val votesByMember = detail.votesByMember.orEmpty()

                Log.d("ConfirmDebug", "slotMinutes=$slotMinutes, requiredSlots=$requiredSlots")
                Log.d("ConfirmDebug", "slotDefinitions=${slotDefs.size}개, votesByMember=${votesByMember.size}개")

                val voters = extractVoters(votesByMember)
                Log.d("ConfirmDebug", "voter(투표한 사람) 수=${voters.size}")
                voters.forEach { v ->
                    Log.d("ConfirmDebug", "  voter memberId=${v.memberId}, slots=${v.slots?.size ?: 0}개")
                }

                if (voters.isEmpty()) {
                    Log.d("ConfirmDebug", "voter=0 → 기본값(13:00~14:00) 유지")
                    return@launch
                }

                val slotDefMap: Map<Int, SlotDefinitionResponse> =
                    slotDefs.filter { it.slotId != null }.associateBy { it.slotId!! }

                val intervals         = buildContinuousIntervals(slotDefs, requiredSlots)
                val intersectionSet   = computeIntersectionSlotIds(voters)
                val intersectionIntvs = filterIntervalsByAllowedSet(intervals, intersectionSet)

                Log.d("ConfirmDebug", "전체 연속구간 ${intervals.size}개, intersectionSlots=${intersectionSet.size}개")
                Log.d("ConfirmDebug", "교집합 기반 후보 ${intersectionIntvs.size}개")

                val chosen: List<Int>? = if (intersectionIntvs.isNotEmpty()) {
                    Log.d("ConfirmDebug", "분기: 교집합 기반 랜덤 선택")
                    pickRandomInterval(intersectionIntvs)
                } else {
                    Log.d("ConfirmDebug", "분기: 교집합 없음 → 최다선택 기반")
                    val countMap   = buildCountMap(voters)
                    val bestIntvs  = pickBestIntervalsByCount(intervals, countMap)
                    Log.d("ConfirmDebug", "최다선택 기반 후보 ${bestIntvs?.size ?: 0}개")
                    bestIntvs?.let { pickRandomInterval(it) } ?: pickRandomInterval(intervals)
                }

                if (chosen == null) {
                    Log.w("ConfirmDebug", "구간 선택 실패 → fallback")
                    return@launch
                }

                val (startDef, endDef) = intervalToStartEnd(chosen, slotDefMap) ?: run {
                    Log.w("ConfirmDebug", "intervalToStartEnd 실패 → fallback")
                    return@launch
                }
                Log.d("ConfirmDebug", "최종 선택: date=${startDef.date}, start=${startDef.startTime}, end=${endDef.endTime}")
                Log.d("ConfirmDebug", "==============================")

                if (!isAdded) return@launch
                updateCalendarsFromDefs(startDef, endDef)
                onUpdated()

            } catch (e: Exception) {
                Log.e("ConfirmDebug", "loadAndPickConfirmedTime 예외: ${e.message}", e)
            }
        }
    }

    /** slots가 비어있지 않은 사람만 voter로 간주 */
    private fun extractVoters(votesByMember: List<MemberVoteDto>): List<MemberVoteDto> =
        votesByMember.filter { !it.slots.isNullOrEmpty() }

    /** 모든 voter가 공통으로 고른 slotId 교집합 */
    private fun computeIntersectionSlotIds(voters: List<MemberVoteDto>): Set<Int> {
        if (voters.isEmpty()) return emptySet()
        val first = voters[0].slots.orEmpty().toSet()
        return voters.drop(1).fold(first) { acc, voter ->
            acc.intersect(voter.slots.orEmpty().toSet())
        }
    }

    /** slotDefinitions에서 같은 날짜의 연속 [requiredSlots]개 슬롯 구간 후보 생성. 반환: List<List<slotId>> */
    private fun buildContinuousIntervals(
        slotDefs: List<SlotDefinitionResponse>,
        requiredSlots: Int
    ): List<List<Int>> {
        val sorted = slotDefs
            .filter { it.slotId != null && it.date != null && it.startTime != null && it.endTime != null }
            .sortedWith(compareBy({ it.date!! }, { normalizeTimeStr(it.startTime!!) }))
        val intervals = mutableListOf<List<Int>>()
        val n = sorted.size
        for (i in 0..(n - requiredSlots)) {
            val candidate = mutableListOf(sorted[i].slotId!!)
            var valid = true
            for (j in 1 until requiredSlots) {
                val prev = sorted[i + j - 1]
                val curr = sorted[i + j]
                if (prev.date != curr.date ||
                    normalizeTimeStr(prev.endTime!!) != normalizeTimeStr(curr.startTime!!)
                ) {
                    valid = false
                    break
                }
                candidate.add(curr.slotId!!)
            }
            if (valid) intervals.add(candidate)
        }
        return intervals
    }

    /** "HH:mm:ss" → "HH:mm" 정규화 (시간 문자열 비교용) */
    private fun normalizeTimeStr(time: String): String =
        time.trim().split(":").take(2).joinToString(":")

    /** allowedSet 에 속하는 slotId 만으로 구성된 구간만 필터링 */
    private fun filterIntervalsByAllowedSet(
        intervals: List<List<Int>>,
        allowedSet: Set<Int>
    ): List<List<Int>> = intervals.filter { interval -> interval.all { it in allowedSet } }

    /** 구간 목록에서 랜덤 1개 선택 */
    private fun pickRandomInterval(intervals: List<List<Int>>): List<Int>? =
        if (intervals.isEmpty()) null else intervals.random()

    /** voter 전체를 대상으로 slotId 별 선택 횟수 집계 */
    private fun buildCountMap(voters: List<MemberVoteDto>): Map<Int, Int> {
        val countMap = mutableMapOf<Int, Int>()
        voters.forEach { voter ->
            voter.slots.orEmpty().forEach { slotId ->
                countMap[slotId] = (countMap[slotId] ?: 0) + 1
            }
        }
        return countMap
    }

    /** 구간 내 슬롯 count 합이 최대인 구간들 반환. 동점 시 여러 개 → 호출부에서 랜덤 선택 */
    private fun pickBestIntervalsByCount(
        intervals: List<List<Int>>,
        countMap: Map<Int, Int>
    ): List<List<Int>>? {
        if (intervals.isEmpty()) return null
        val scored  = intervals.map { it to it.sumOf { id -> countMap[id] ?: 0 } }
        val maxScore = scored.maxOf { it.second }
        if (maxScore == 0) return null
        return scored.filter { it.second == maxScore }.map { it.first }
    }

    /** slotId 리스트 → (첫 슬롯 정의, 마지막 슬롯 정의) 쌍으로 변환 */
    private fun intervalToStartEnd(
        intervalSlotIds: List<Int>,
        slotDefMap: Map<Int, SlotDefinitionResponse>
    ): Pair<SlotDefinitionResponse, SlotDefinitionResponse>? {
        val startDef = slotDefMap[intervalSlotIds.first()] ?: return null
        val endDef   = slotDefMap[intervalSlotIds.last()]  ?: return null
        return startDef to endDef
    }

    /** SlotDefinitionResponse 의 date/time 정보로 editStartCalendar / editEndCalendar 갱신 */
    private fun updateCalendarsFromDefs(
        startDef: SlotDefinitionResponse,
        endDef: SlotDefinitionResponse
    ) {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fun parseHM(time: String): Pair<Int, Int>? {
            val parts = time.trim().split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
            return h to m
        }
        val startDateMs = try { dateFmt.parse(startDef.date ?: return)?.time ?: return } catch (_: Exception) { return }
        val endDateMs   = try { dateFmt.parse(endDef.date   ?: return)?.time ?: return } catch (_: Exception) { return }
        val (sH, sM) = parseHM(startDef.startTime ?: return) ?: return
        val (eH, eM) = parseHM(endDef.endTime     ?: return) ?: return

        editStartCalendar = Calendar.getInstance().apply {
            timeInMillis = startDateMs
            set(Calendar.HOUR_OF_DAY, sH); set(Calendar.MINUTE, sM)
            set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
        }
        editEndCalendar = Calendar.getInstance().apply {
            timeInMillis = endDateMs
            set(Calendar.HOUR_OF_DAY, eH); set(Calendar.MINUTE, eM)
            set(Calendar.SECOND, 0);       set(Calendar.MILLISECOND, 0)
        }
        Log.d("ConfirmDebug", "캘린더 갱신: ${startDef.date} $sH:${sM.toString().padStart(2,'0')} ~ $eH:${eM.toString().padStart(2,'0')}")
    }

    /** 확정 교집합 셀을 같은 날짜의 연속 슬롯대로 블록화. (dayIndex, startSlot..endSlot) */
    private fun buildConfirmedBlocks(confirmed: Set<Pair<Int, Int>>): List<Pair<Int, IntRange>> {
        val byDay = confirmed.groupBy { it.first }
        val blocks = mutableListOf<Pair<Int, IntRange>>()
        byDay.forEach { (dayIdx, cells) ->
            val slots = cells.map { it.second }.sorted()
            var i = 0
            while (i < slots.size) {
                val start = slots[i]
                var end = start + 1
                while (i + 1 < slots.size && slots[i + 1] == end) {
                    i++
                    end++
                }
                blocks.add(dayIdx to (start..end - 1))
                i++
            }
        }
        return blocks.sortedWith(compareBy({ it.first }, { it.second.first }))
    }
}
