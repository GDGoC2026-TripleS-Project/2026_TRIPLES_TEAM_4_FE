package com.project.unimate.ui.timepick

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository
import java.util.Calendar

class TimepickStatusFragment : Fragment() {

    private enum class FilterMode { ALL_AVAILABLE, ALL, MEMBER }
    private var currentFilter: FilterMode = FilterMode.ALL_AVAILABLE
    private var selectedMemberId: String? = null

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
        val root = inflater.inflate(R.layout.fragment_timepick_status, container, false)
        val back = root.findViewById<ImageButton>(R.id.timepickStatusBack)
        val participantCount = root.findViewById<TextView>(R.id.timepickStatusParticipantCount)
        val allAvailableBtn = root.findViewById<TextView>(R.id.timepickStatusAllAvailableBtn)
        val memberFilters = root.findViewById<LinearLayout>(R.id.timepickStatusMemberFilters)
        val grid = root.findViewById<TimepickGridView>(R.id.timepickStatusGrid)
        val strip = root.findViewById<TimepickTimeLabelStrip>(R.id.timepickStatusLabelStrip)
        val scrollWrapper = root.findViewById<LinearLayout>(R.id.timepickStatusScrollWrapper)
        val contentRow = root.findViewById<LinearLayout>(R.id.timepickStatusContentRow)
        val timepickScroll = root.findViewById<HorizontalScrollView>(R.id.timepickStatusScroll)
        val gridWrapper = root.findViewById<View>(R.id.timepickStatusGridWrapper)
        val editTimeBtn = root.findViewById<android.widget.Button>(R.id.timepickStatusEditTime)

        val teamId = TimepickStateHolder.teamId
        val members = DummyRepository.getTeamMembers(teamId)
        val totalMembers = members.size
        participantCount.text = getString(R.string.timepick_participants, totalMembers, totalMembers)

        val dates = TimepickStateHolder.displayDates
        val startHour = TimepickStateHolder.globalStartHour
        val endHour = TimepickStateHolder.globalEndHour
        if (dates.isEmpty()) {
            findNavController().popBackStack()
            return root
        }

        grid.dayCount = dates.size
        grid.startHour = startHour
        grid.endHour = endHour
        grid.dateHeaders = dates.map { dayMillis ->
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
            val weekday = weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
            "$m/$d" to weekday
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

        val slotCount = (endHour - startHour).coerceAtLeast(0) * 2
        val dayTimeRanges = dates.map { dayMillis ->
            val (s, e) = TimepickStateHolder.dateTimeRanges[dayMillis] ?: (startHour to endHour)
            val startSlot = ((s - startHour).coerceAtLeast(0) * 2).coerceIn(0, slotCount)
            val endSlot = ((e - startHour).coerceAtMost(endHour - startHour) * 2).coerceIn(0, slotCount)
            startSlot to endSlot
        }
        val memberSelections: List<Set<Pair<Int, Int>>> = buildMemberSelections(members.size, dates.size, slotCount, dayTimeRanges)
        val allCellsCount = mutableMapOf<Pair<Int, Int>, Int>()
        memberSelections.forEach { selection ->
            selection.forEach { cell ->
                allCellsCount[cell] = (allCellsCount[cell] ?: 0) + 1
            }
        }
        val intersectionCells = allCellsCount.filter { it.value == totalMembers }.keys

        fun refreshFilterUi() {
            allAvailableBtn.setBackgroundResource(
                if (currentFilter == FilterMode.ALL_AVAILABLE) R.drawable.bg_timepick_filter_selected
                else R.drawable.bg_timepick_filter_unselected
            )
            allAvailableBtn.setTextColor(ContextCompat.getColor(requireContext(), if (currentFilter == FilterMode.ALL_AVAILABLE) R.color.white else R.color.gray06))
            for (i in 0 until memberFilters.childCount) {
                val child = memberFilters.getChildAt(i) as? TextView ?: continue
                val isSelected = when (currentFilter) {
                    FilterMode.ALL -> i == 0
                    FilterMode.MEMBER -> i > 0 && members.getOrNull(i - 1)?.id == selectedMemberId
                    else -> false
                }
                child.setBackgroundResource(if (isSelected) R.drawable.bg_timepick_filter_selected else R.drawable.bg_timepick_filter_unselected)
                child.setTextColor(ContextCompat.getColor(requireContext(), if (isSelected) R.color.white else R.color.gray06))
            }
        }

        fun refreshGrid() {
            grid.cellCounts = when (currentFilter) {
                FilterMode.ALL_AVAILABLE -> intersectionCells.associateWith { totalMembers }
                FilterMode.ALL -> allCellsCount
                FilterMode.MEMBER -> {
                    val idx = members.indexOfFirst { it.id == selectedMemberId }
                    if (idx < 0) emptyMap() else memberSelections.getOrNull(idx)?.associateWith { 1 } ?: emptyMap()
                }
            }
        }

        allAvailableBtn.setOnClickListener {
            currentFilter = FilterMode.ALL_AVAILABLE
            selectedMemberId = null
            refreshFilterUi()
            refreshGrid()
        }

        memberFilters.removeAllViews()
        val allTab = layoutInflater.inflate(android.R.layout.simple_list_item_1, memberFilters, false) as TextView
        allTab.text = getString(R.string.timepick_filter_all)
        allTab.gravity = Gravity.CENTER
        allTab.setPadding(24, 0, 24, 0)
        allTab.textSize = 14f
        allTab.layoutParams = LinearLayout.LayoutParams(58.dpToPx(), 26.dpToPx()).apply { marginEnd = 8 }
        allTab.setOnClickListener {
            currentFilter = FilterMode.ALL
            selectedMemberId = null
            refreshFilterUi()
            refreshGrid()
        }
        memberFilters.addView(allTab)
        members.forEach { member ->
            val tab = layoutInflater.inflate(android.R.layout.simple_list_item_1, memberFilters, false) as TextView
            tab.text = member.name
            tab.gravity = Gravity.CENTER
            tab.setPadding(24, 0, 24, 0)
            tab.textSize = 14f
            tab.layoutParams = LinearLayout.LayoutParams(58.dpToPx(), 26.dpToPx()).apply { marginEnd = 8 }
            tab.setOnClickListener {
                currentFilter = FilterMode.MEMBER
                selectedMemberId = member.id
                refreshFilterUi()
                refreshGrid()
            }
            memberFilters.addView(tab)
        }
        refreshFilterUi()
        refreshGrid()

        back.setOnClickListener { findNavController().popBackStack() }
        editTimeBtn.setOnClickListener {
            findNavController().popBackStack(R.id.selectTimeFragment, false)
        }

        root.findViewById<android.widget.Button>(R.id.timepickStatusGoResult)?.setOnClickListener {
            findNavController().navigate(R.id.timepickResultFragment)
        }

        return root
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    /**
     * 더미: 멤버0은 사용자 실제 선택만 사용(모두 가능한 시간 = 교집합 = 사용자 선택).
     * 멤버1 이상은 사용자 선택 + 날짜별 허용 범위 안에서 하루 1~3시간만 랜덤 블록 추가.
     */
    private fun buildMemberSelections(
        memberCount: Int,
        dayCount: Int,
        slotCount: Int,
        dayTimeRanges: List<Pair<Int, Int>>
    ): List<Set<Pair<Int, Int>>> {
        val base = TimepickStateHolder.selectTimeSelected.keys.toSet().toMutableSet()
        val list = mutableListOf<Set<Pair<Int, Int>>>()
        for (m in 0 until memberCount) {
            val set = if (m == 0) {
                base.toMutableSet()
            } else {
                val memberSet = base.toMutableSet()
                for (day in 0 until dayCount) {
                    val (startSlot, endSlot) = dayTimeRanges.getOrNull(day) ?: (0 to slotCount)
                    val rangeSize = (endSlot - startSlot).coerceAtLeast(0)
                    if (rangeSize < 2) continue
                    val durationSlots = when ((m + day) % 3) { 0 -> 2; 1 -> 4; else -> 6 }.coerceAtMost(rangeSize)
                    val maxStart = (endSlot - durationSlots).coerceAtLeast(startSlot)
                    val start = startSlot + (m * 7 + day * 11) % (maxStart - startSlot + 1).coerceAtLeast(1)
                    for (slot in start until (start + durationSlots).coerceAtMost(endSlot)) {
                        memberSet.add(day to slot)
                    }
                }
                memberSet
            }
            list.add(set)
        }
        return list
    }
}
