package com.project.unimate.ui.timepick

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
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.SchedulePollCreateRequest
import com.project.unimate.network.service.SchedulePollService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.ZoneId

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

        cancelBtn.setOnClickListener {
            findNavController().popBackStack(R.id.createTimepickFragment, true)
        }
        saveBtn.setOnClickListener {
            val title = scheduleName.text.toString().ifBlank { "${team?.name ?: ""} 회의" }
            if (existingTask != null) {
                val updated = existingTask.copy(
                    title = title,
                    date = editStartCalendar,
                    startTimeMillis = editStartCalendar.timeInMillis,
                    endTimeMillis = editEndCalendar.timeInMillis
                )
                DummyRepository.updateTask(updated)
                DummyRepository.saveSchedulesTo(requireContext())
            } else if (teamId.isNotEmpty()) {
                val task = TaskItem(
                    id = "timepick-${teamId}-${System.currentTimeMillis()}",
                    teamId = teamId,
                    title = title,
                    date = editStartCalendar,
                    startTimeMillis = editStartCalendar.timeInMillis,
                    endTimeMillis = editEndCalendar.timeInMillis,
                    isChecked = false,
                    creatorName = null
                )
                DummyRepository.addTask(task)
                DummyRepository.saveSchedulesTo(requireContext())

                // API 호출 (시간 조율 투표 생성)
                val numericTeamId = teamId.toLongOrNull()
                if (numericTeamId != null && TimepickStateHolder.pollId == null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val dates = TimepickStateHolder.displayDates.map { dateFmt.format(Date(it)) }
                            val timezone = ZoneId.systemDefault().id
                            val service = RetrofitClient.create<SchedulePollService>(requireContext())
                            val response = service.create(SchedulePollCreateRequest(
                                teamId = numericTeamId,
                                dates = dates,
                                startTime = timeFmt.format(Date(editStartCalendar.timeInMillis)),
                                endTime = timeFmt.format(Date(editEndCalendar.timeInMillis)),
                                timezone = timezone,
                                title = title,
                                slotMinutes = 30
                            ))
                            if (response.isSuccessful) {
                                TimepickStateHolder.pollId = response.body()?.pollId
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
            findNavController().popBackStack(R.id.createTimepickFragment, true)
        }

        return root
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
