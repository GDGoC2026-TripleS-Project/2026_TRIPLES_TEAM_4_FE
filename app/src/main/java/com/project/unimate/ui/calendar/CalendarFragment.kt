package com.project.unimate.ui.calendar

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository
import java.util.Calendar

class CalendarFragment : Fragment() {

    companion object {
        /** 다른 탭 갔다 와도 팀플 필터 유지 */
        private val savedFilterTeamIds = mutableListOf<String>()
        /** 개인일정 토글 기본 ON, 탭 이동 후에도 유지 */
        private var savedPersonalVisible = true
    }

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay: Calendar = Calendar.getInstance()
    private var personalVisible: Boolean
        get() = savedPersonalVisible
        set(value) { savedPersonalVisible = value }
    private val selectedFilterTeamIds = mutableListOf<String>().apply {
        if (savedFilterTeamIds.isEmpty()) addAll(DummyRepository.getCalendarFilterTeams().map { it.id })
        else addAll(savedFilterTeamIds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_calendar, container, false)

        fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

        val headerCalendarFilter = root.findViewById<ImageButton>(R.id.headerCalendarFilter)
        val calendarPersonalToggle = root.findViewById<ImageButton>(R.id.calendarPersonalToggle)
        val calendarFilterChips = root.findViewById<LinearLayout>(R.id.calendarFilterChips)
        val calendarMonthYear = root.findViewById<TextView>(R.id.calendarMonthYear)
        val calendarPrevMonth = root.findViewById<ImageButton>(R.id.calendarPrevMonth)
        val calendarNextMonth = root.findViewById<ImageButton>(R.id.calendarNextMonth)
        val calendarMonthGrid = root.findViewById<GridLayout>(R.id.calendarMonthGrid)
        val calendarSelectedDateText = root.findViewById<TextView>(R.id.calendarSelectedDateText)
        val calendarDayTasksContainer = root.findViewById<LinearLayout>(R.id.calendarDayTasksContainer)
        val calendarFabAdd = root.findViewById<ImageButton>(R.id.calendarFabAdd)

        fun filterTeams(): List<Team> = DummyRepository.getCalendarFilterTeams().filter { it.id in selectedFilterTeamIds }

        fun refreshMonthLabel() {
            calendarMonthYear.text = getString(R.string.date_format_year_month, currentYear, currentMonth + 1)
        }

        fun refreshDayTasks() {
            calendarSelectedDateText.text = getString(R.string.date_format_full,
                selectedDay.get(Calendar.YEAR), selectedDay.get(Calendar.MONTH) + 1, selectedDay.get(Calendar.DAY_OF_MONTH))
            calendarDayTasksContainer.removeAllViews()
            val teams = filterTeams()
            val teamMap = teams.associateBy { it.id }
            val tasks = DummyRepository.getTasksForDate(selectedDay, teams.map { it.id })
            tasks.groupBy { it.teamId }.forEach { (teamId, taskList) ->
                val team = teamMap[teamId] ?: return@forEach
                val teamHeader = TextView(requireContext()).apply {
                    text = team.name
                    setTextColor(android.graphics.Color.BLACK)
                    setPadding(0, 8.dpToPx(), 0, 4.dpToPx())
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
                }
                calendarDayTasksContainer.addView(teamHeader)
                taskList.forEach { task ->
                    val taskRow = inflater.inflate(R.layout.item_task_row, calendarDayTasksContainer, false)
                    val checkBtn = taskRow.findViewById<ImageButton>(R.id.taskCheck)
                    val titleTv = taskRow.findViewById<TextView>(R.id.taskTitle)
                    titleTv.isClickable = true
                    titleTv.isFocusable = true
                    titleTv.setOnClickListener {
                        findNavController().navigate(R.id.editTeamTaskFragment, Bundle().apply { putString("taskId", task.id) })
                    }
                    checkBtn.setImageResource(if (task.isChecked) R.drawable.ic_check_on else R.drawable.ic_check_off)
                    titleTv.text = task.title
                    if (task.isChecked) {
                        titleTv.paintFlags = titleTv.paintFlags or 0x10
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                    } else {
                        titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                    checkBtn.setOnClickListener {
                        DummyRepository.setTaskChecked(task.id, !task.isChecked)
                        refreshDayTasks()
                    }
                    taskRow.findViewById<ImageButton>(R.id.taskLock).visibility = View.GONE
                    calendarDayTasksContainer.addView(taskRow)
                }
            }
            if (personalVisible) {
                val personalList = DummyRepository.getPersonalForDate(selectedDay)
                if (personalList.isNotEmpty()) {
                    val personalHeader = TextView(requireContext()).apply {
                        text = getString(R.string.personal_schedule)
                        setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                    }
                    calendarDayTasksContainer.addView(personalHeader)
                    personalList.forEach { item ->
                        val personalRow = inflater.inflate(R.layout.item_task_row, calendarDayTasksContainer, false)
                        val checkBtn = personalRow.findViewById<ImageButton>(R.id.taskCheck)
                        val titleTv = personalRow.findViewById<TextView>(R.id.taskTitle)
                        titleTv.isClickable = true
                        titleTv.isFocusable = true
                        titleTv.setOnClickListener {
                            findNavController().navigate(R.id.editPersonalTaskFragment, Bundle().apply { putString("personalId", item.id) })
                        }
                        checkBtn.setImageResource(if (item.isChecked) R.drawable.ic_check_on else R.drawable.ic_check_off)
                        titleTv.text = item.title
                        if (item.isChecked) {
                            titleTv.paintFlags = titleTv.paintFlags or 0x10
                            titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                        } else {
                            titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                            titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        }
                        val lockBtn = personalRow.findViewById<ImageButton>(R.id.taskLock)
                        lockBtn.visibility = View.VISIBLE
                        lockBtn.setImageResource(if (item.isLocked) R.drawable.ic_personal_lock else R.drawable.ic_personal_unlock)
                        lockBtn.setOnClickListener {
                            DummyRepository.setPersonalLocked(item.id, !item.isLocked)
                            refreshDayTasks()
                        }
                        checkBtn.setOnClickListener {
                            DummyRepository.setPersonalChecked(item.id, !item.isChecked)
                            refreshDayTasks()
                        }
                        calendarDayTasksContainer.addView(personalRow)
                    }
                }
            }
        }

        fun refreshGrid() {
            calendarMonthGrid.removeAllViews()
            val days = DummyRepository.getMonthCalendarDaysCurrentMonthOnly(currentYear, currentMonth + 1)
            val teams = filterTeams()
            val teamIds = teams.map { it.id }
            days.forEachIndexed { index, dayOrNull ->
                val cell = inflater.inflate(R.layout.item_calendar_day, calendarMonthGrid, false)
                val dayNumber = cell.findViewById<TextView>(R.id.calendarDayNumber)
                val countBadge = cell.findViewById<View>(R.id.calendarDayEventCountBadge)
                val countTv = cell.findViewById<TextView>(R.id.calendarDayEventCount)
                val root = cell.findViewById<View>(R.id.calendarDayRoot)
                if (dayOrNull == null) {
                    dayNumber.text = ""
                    dayNumber.visibility = View.INVISIBLE
                    countBadge.visibility = View.GONE
                    root.setBackgroundResource(0)
                    cell.isClickable = false
                    cell.isFocusable = false
                } else {
                    val day = dayOrNull
                    dayNumber.visibility = View.VISIBLE
                    dayNumber.text = day.get(Calendar.DAY_OF_MONTH).toString()
                    dayNumber.alpha = 1f
                    val today = Calendar.getInstance()
                    val isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
                    val isSelected = day.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) &&
                        day.get(Calendar.MONTH) == selectedDay.get(Calendar.MONTH) &&
                        day.get(Calendar.DAY_OF_MONTH) == selectedDay.get(Calendar.DAY_OF_MONTH)
                    root.setBackgroundResource(
                        when {
                            isSelected -> R.drawable.bg_calendar_day_selected
                            isToday -> R.drawable.bg_calendar_today
                            else -> 0
                        }
                    )
                    cell.setOnClickListener {
                        selectedDay.timeInMillis = day.timeInMillis
                        refreshGrid()
                        refreshDayTasks()
                    }
                    val count = DummyRepository.getDayEventCount(day, teamIds)
                    if (count > 0) {
                        countBadge.visibility = View.VISIBLE
                        countTv.text = count.toString()
                    } else {
                        countBadge.visibility = View.GONE
                    }
                }
                val row = index / 7
                val col = index % 7
                val cellHeightPx = 68.dpToPx()
                val params = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(col)).apply {
                    width = 0
                    height = cellHeightPx
                    setGravity(Gravity.FILL)
                    columnSpec = GridLayout.spec(col, 1f)
                    rowSpec = GridLayout.spec(row)
                }
                calendarMonthGrid.addView(cell, params)
            }
        }

        fun refreshChips() {
            savedFilterTeamIds.clear()
            savedFilterTeamIds.addAll(selectedFilterTeamIds)
            calendarFilterChips.removeAllViews()
            filterTeams().forEach { team ->
                val chip = inflater.inflate(R.layout.item_team_chip, calendarFilterChips, false)
                val chipTv = chip.findViewById<TextView>(R.id.chipTeamName)
                chipTv.text = team.name
                val radiusPx = 18 * resources.displayMetrics.density
                chipTv.background = GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor(team.colorHex))
                    cornerRadius = radiusPx
                }
                chip.findViewById<ImageButton>(R.id.chipRemove).setOnClickListener {
                    selectedFilterTeamIds.remove(team.id)
                    refreshChips()
                    refreshGrid()
                    refreshDayTasks()
                }
                calendarFilterChips.addView(chip)
            }
        }

        fun showTeamFilterDialog() {
            val allTeams = DummyRepository.getCalendarFilterTeams()
            val dialogView = layoutInflater.inflate(R.layout.dialog_calendar_filter, null)
            val listContainer = dialogView.findViewById<LinearLayout>(R.id.dialogFilterList)
            val confirmBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogFilterConfirm)
            val checkStates = allTeams.map { it.id in selectedFilterTeamIds }.toMutableList()
            listContainer.removeAllViews()
            allTeams.forEachIndexed { index, team ->
                val cb = CheckBox(requireContext()).apply {
                    text = team.name
                    isChecked = checkStates[index]
                    setPadding(0, 12.dpToPx(), 0, 12.dpToPx())
                }
                cb.setOnCheckedChangeListener { _, isChecked -> checkStates[index] = isChecked }
                listContainer.addView(cb)
            }
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            confirmBtn.setOnClickListener {
                selectedFilterTeamIds.clear()
                checkStates.forEachIndexed { i, c -> if (c) selectedFilterTeamIds.add(allTeams[i].id) }
                refreshChips()
                refreshGrid()
                refreshDayTasks()
                dialog.dismiss()
            }
            dialog.show()
        }

        headerCalendarFilter.setOnClickListener { showTeamFilterDialog() }

        calendarPersonalToggle.setOnClickListener {
            personalVisible = !personalVisible
            calendarPersonalToggle.setImageResource(if (personalVisible) R.drawable.ic_personal_on else R.drawable.ic_personal_off)
            refreshDayTasks()
        }

        calendarPrevMonth.setOnClickListener {
            if (currentMonth == 0) { currentYear--; currentMonth = 11 } else currentMonth--
            refreshMonthLabel()
            refreshGrid()
            refreshDayTasks()
        }
        calendarNextMonth.setOnClickListener {
            if (currentMonth == 11) { currentYear++; currentMonth = 0 } else currentMonth++
            refreshMonthLabel()
            refreshGrid()
            refreshDayTasks()
        }

        refreshChips()
        refreshMonthLabel()
        refreshGrid()
        refreshDayTasks()
        calendarPersonalToggle.setImageResource(if (personalVisible) R.drawable.ic_personal_on else R.drawable.ic_personal_off)

        calendarFabAdd.setOnClickListener {
            findNavController().navigate(R.id.addPersonalTaskFragment)
        }

        return root
    }
}
