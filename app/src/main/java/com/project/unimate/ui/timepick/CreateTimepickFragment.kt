package com.project.unimate.ui.timepick

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.TeamMember
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateTimepickFragment : Fragment() {

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDateForTime: Long? = null

    private val timeLabels: List<String> by lazy {
        (0..24).map { hourToLabel(it) }
    }

    private lateinit var inflater: LayoutInflater
    private lateinit var yearMonth: TextView
    private lateinit var monthGrid: GridLayout
    private lateinit var selectedDateChips: LinearLayout
    private lateinit var dateTabs: LinearLayout
    private lateinit var startTimeTv: TextView
    private lateinit var endTimeTv: TextView
    private lateinit var selectedTimeCard: TextView
    private lateinit var selectedTimeCardWrap: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        this.inflater = inflater
        val root = inflater.inflate(R.layout.fragment_create_timepick, container, false)
        val teamId = arguments?.getString(ARG_TEAM_ID) ?: ""
        if (teamId.isEmpty()) {
            findNavController().navigateUp()
            return root
        }

        // 새 모이기 플로우 진입 시 이전 생성 상태(pollId)가 남아 있으면 초기화
        // - 다른 팀으로 진입
        // - 이전 모이기 생성 성공 후 같은 팀에서 다시 시작
        if (TimepickStateHolder.teamId != teamId || TimepickStateHolder.pollId != null) {
            TimepickStateHolder.clear()
        }
        TimepickStateHolder.teamId = teamId

        // 서버 팀원 미리 캐시 → TimepickStatusFragment에서 merge된 팀원 표시
        val numericTeamIdForCache = teamId.toLongOrNull()
        if (numericTeamIdForCache != null) {
            lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val resp = service.getMembers(numericTeamIdForCache)
                    if (resp.isSuccessful) {
                        val serverMembers = (resp.body() ?: emptyList()).map { m ->
                            TeamMember(
                                id = "server-${m.userId ?: m.nickname.hashCode()}",
                                name = m.nickname ?: "팀원",
                                iconResName = "ic_user"
                            )
                        }
                        DummyRepository.cacheServerMembers(teamId, serverMembers)
                    }
                } catch (_: Exception) {}
            }
        }

        val back = root.findViewById<ImageButton>(R.id.createTimepickBack)
        yearMonth = root.findViewById(R.id.createTimepickYearMonth)
        val prevMonth = root.findViewById<ImageButton>(R.id.createTimepickPrevMonth)
        val nextMonth = root.findViewById<ImageButton>(R.id.createTimepickNextMonth)
        monthGrid = root.findViewById(R.id.createTimepickMonthGrid)
        selectedDateChips = root.findViewById(R.id.createTimepickSelectedDateChips)
        dateTabs = root.findViewById(R.id.createTimepickDateTabs)
        val startTimeWrap = root.findViewById<LinearLayout>(R.id.createTimepickStartTimeWrap)
        startTimeTv = root.findViewById(R.id.createTimepickStartTime)
        val endTimeWrap = root.findViewById<LinearLayout>(R.id.createTimepickEndTimeWrap)
        endTimeTv = root.findViewById(R.id.createTimepickEndTime)
        selectedTimeCardWrap = root.findViewById(R.id.createTimepickSelectedTimeCardWrap)
        selectedTimeCard = root.findViewById(R.id.createTimepickSelectedTimeCard)
        val nextBtn = root.findViewById<android.widget.Button>(R.id.createTimepickNext)

        back.setOnClickListener { findNavController().navigateUp() }
        prevMonth.setOnClickListener {
            if (currentMonth == 0) { currentYear--; currentMonth = 11 } else currentMonth--
            refreshYearMonth()
            refreshGrid()
        }
        nextMonth.setOnClickListener {
            if (currentMonth == 11) { currentYear++; currentMonth = 0 } else currentMonth++
            refreshYearMonth()
            refreshGrid()
        }

        startTimeWrap.setOnClickListener { if (selectedDateForTime != null) showStartTimePicker() }
        endTimeWrap.setOnClickListener { if (selectedDateForTime != null) showEndTimePicker() }

        nextBtn.setOnClickListener {
            if (TimepickStateHolder.selectedDates.isEmpty()) return@setOnClickListener
            val minStart = TimepickStateHolder.dateTimeRanges.values.minOfOrNull { it.first } ?: 8
            val maxEnd = TimepickStateHolder.dateTimeRanges.values.maxOfOrNull { it.second } ?: 24
            TimepickStateHolder.globalStartHour = minStart
            TimepickStateHolder.globalEndHour = maxEnd
            TimepickStateHolder.displayDates = TimepickStateHolder.selectedDates.sorted()
            TimepickStateHolder.selectTimeSelected.clear()
            findNavController().navigate(R.id.selectTimeFragment)
        }

        refreshYearMonth()
        if (TimepickStateHolder.selectedDates.isNotEmpty()) {
            selectedDateForTime = TimepickStateHolder.selectedDates.minOrNull()
        }
        refreshGrid()
        refreshSelectedChips()
        refreshDateTabs()
        refreshTimeForSelectedDate()

        return root
    }

    private fun refreshYearMonth() {
        yearMonth.text = getString(R.string.date_format_year_month, currentYear, currentMonth + 1)
    }

    private fun refreshTimeForSelectedDate() {
        val day = selectedDateForTime ?: run {
            startTimeTv.text = getString(R.string.timepick_select_start_time)
            endTimeTv.text = getString(R.string.timepick_select_end_time)
            startTimeTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))
            endTimeTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))
            selectedTimeCardWrap.visibility = View.GONE
            return
        }
        val range = TimepickStateHolder.dateTimeRanges[day]
        if (range != null) {
            startTimeTv.text = timeLabels.getOrNull(range.first) ?: getString(R.string.timepick_select_start_time)
            endTimeTv.text = timeLabels.getOrNull(range.second) ?: getString(R.string.timepick_select_end_time)
            startTimeTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray10))
            endTimeTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray10))
            selectedTimeCard.text = "${timeLabels[range.first]} - ${timeLabels[range.second]}"
            selectedTimeCardWrap.visibility = View.VISIBLE
        } else {
            startTimeTv.text = getString(R.string.timepick_select_start_time)
            endTimeTv.text = getString(R.string.timepick_select_end_time)
            startTimeTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))
            endTimeTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))
            selectedTimeCardWrap.visibility = View.GONE
        }
    }

    private fun refreshDateTabs() {
        dateTabs.removeAllViews()
        TimepickStateHolder.selectedDates.sorted().forEach { dayMillis ->
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val tab = inflater.inflate(R.layout.item_timepick_date_tab, dateTabs, false) as TextView
            tab.text = "${cal.get(Calendar.DAY_OF_MONTH)}"
            tab.setBackgroundResource(
                if (selectedDateForTime == dayMillis) R.drawable.bg_timepick_date_tab_selected
                else R.drawable.bg_timepick_date_tab
            )
            tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            tab.setOnClickListener {
                selectedDateForTime = dayMillis
                refreshDateTabs()
                refreshTimeForSelectedDate()
            }
            dateTabs.addView(tab)
        }
    }

    private fun refreshGrid() {
        monthGrid.removeAllViews()
        val days = DummyRepository.getMonthCalendarDaysCurrentMonthOnly(currentYear, currentMonth + 1)
        val today = Calendar.getInstance()
        days.forEachIndexed { index, dayOrNull ->
            val cell = inflater.inflate(R.layout.item_calendar_day, monthGrid, false)
            val col = index % 7
            val row = index / 7
            val density = resources.displayMetrics.density
            val cellHeightPx = (50 * density).toInt()
            val cellWidthPx = (54 * density).toInt()
            val lp = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(col)).apply {
                width = 0
                height = cellHeightPx
                columnSpec = GridLayout.spec(col, 1f)
                rowSpec = GridLayout.spec(row)
            }
            cell.layoutParams = lp
            cell.minimumWidth = cellWidthPx
            val dayNumber = cell.findViewById<TextView>(R.id.calendarDayNumber)
            val countBadge = cell.findViewById<View>(R.id.calendarDayEventCountBadge)
            val cellRoot = cell.findViewById<View>(R.id.calendarDayRoot)
            if (dayOrNull == null) {
                dayNumber.text = ""
                dayNumber.visibility = View.INVISIBLE
                countBadge.visibility = View.GONE
                cellRoot.setBackgroundResource(0)
                cell.isClickable = false
            } else {
                val day = dayOrNull
                val dayStart = TimepickStateHolder.dayStartMillis(day)
                dayNumber.visibility = View.VISIBLE
                dayNumber.text = day.get(Calendar.DAY_OF_MONTH).toString()
                val isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
                val isSelected = TimepickStateHolder.selectedDates.contains(dayStart)
                cellRoot.setBackgroundResource(
                    when {
                        isSelected -> R.drawable.bg_timepick_day_selected
                        isToday -> R.drawable.bg_calendar_today
                        else -> 0
                    }
                )
                dayNumber.setTextColor(
                    ContextCompat.getColor(requireContext(), if (isSelected) R.color.white else R.color.gray10)
                )
                countBadge.visibility = View.GONE
                cell.setOnClickListener {
                    if (TimepickStateHolder.selectedDates.contains(dayStart)) {
                        TimepickStateHolder.selectedDates.remove(dayStart)
                        TimepickStateHolder.dateTimeRanges.remove(dayStart)
                    } else {
                        TimepickStateHolder.selectedDates.add(dayStart)
                        TimepickStateHolder.dateTimeRanges[dayStart] = 9 to 17
                    }
                    if (selectedDateForTime == null) selectedDateForTime = dayStart
                    refreshGrid()
                    refreshSelectedChips()
                    refreshDateTabs()
                    refreshTimeForSelectedDate()
                }
            }
            monthGrid.addView(cell)
        }
    }

    private fun refreshSelectedChips() {
        selectedDateChips.removeAllViews()
        TimepickStateHolder.selectedDates.sorted().forEach { dayMillis ->
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val chip = inflater.inflate(R.layout.item_timepick_date_chip, selectedDateChips, false) as LinearLayout
            chip.findViewById<TextView>(R.id.timepickChipDateText).text = "${cal.get(Calendar.DAY_OF_MONTH)}"
            chip.findViewById<ImageButton>(R.id.timepickChipClose).setOnClickListener {
                TimepickStateHolder.selectedDates.remove(dayMillis)
                TimepickStateHolder.dateTimeRanges.remove(dayMillis)
                if (selectedDateForTime == dayMillis) selectedDateForTime = TimepickStateHolder.selectedDates.minOrNull()
                refreshSelectedChips()
                refreshDateTabs()
                refreshTimeForSelectedDate()
                refreshGrid()
            }
            selectedDateChips.addView(chip)
        }
    }

    private fun showStartTimePicker() {
        val day = selectedDateForTime ?: return
        var endHour = TimepickStateHolder.dateTimeRanges[day]?.second ?: 17
        val startHour = TimepickStateHolder.dateTimeRanges[day]?.first ?: 9
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val dlg = TimePickerDialog(
            contextWrapper,
            { _, h, _ ->
                var start = h
                if (endHour <= start) endHour = (start + 1).coerceAtMost(24)
                TimepickStateHolder.dateTimeRanges[day] = start to endHour
                refreshTimeForSelectedDate()
                refreshDateTabs()
            },
            startHour,
            0,
            false
        )
        dlg.setButton(TimePickerDialog.BUTTON_POSITIVE, "확인", dlg)
        dlg.setButton(TimePickerDialog.BUTTON_NEGATIVE, "취소", dlg)
        dlg.setOnShowListener {
            val colorBlack = ContextCompat.getColor(requireContext(), android.R.color.black)
            dlg.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(colorBlack)
            dlg.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(colorBlack)
        }
        dlg.show()
    }

    private fun showEndTimePicker() {
        val day = selectedDateForTime ?: return
        val startHour = TimepickStateHolder.dateTimeRanges[day]?.first ?: 9
        var endHour = TimepickStateHolder.dateTimeRanges[day]?.second ?: 17
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val dlg = TimePickerDialog(
            contextWrapper,
            { _, h, _ ->
                var end = h
                if (end <= startHour) end = (startHour + 1).coerceAtMost(24)
                TimepickStateHolder.dateTimeRanges[day] = startHour to end
                refreshTimeForSelectedDate()
                refreshDateTabs()
            },
            endHour,
            0,
            false
        )
        dlg.setButton(TimePickerDialog.BUTTON_POSITIVE, "확인", dlg)
        dlg.setButton(TimePickerDialog.BUTTON_NEGATIVE, "취소", dlg)
        dlg.setOnShowListener {
            val colorBlack = ContextCompat.getColor(requireContext(), android.R.color.black)
            dlg.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(colorBlack)
            dlg.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(colorBlack)
        }
        dlg.show()
    }

    private fun hourToLabel(hour: Int): String {
        return when (hour) {
            0 -> "오전 12시"
            in 1..11 -> "오전 ${hour}시"
            12 -> "오후 12시"
            else -> "오후 ${hour - 12}시"
        }
    }

    companion object {
        const val ARG_TEAM_ID = "teamId"
    }
}
