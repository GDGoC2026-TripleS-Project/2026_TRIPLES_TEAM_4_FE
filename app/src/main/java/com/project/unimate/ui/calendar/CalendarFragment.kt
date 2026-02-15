package com.project.unimate.ui.calendar

import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.model.CalendarDayResponse
import com.project.unimate.model.CalendarMonthResponse
import com.project.unimate.model.TeamSummary
import com.project.unimate.network.CalendarService
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.TeamService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {

    companion object {
        private val savedFilterTeamIds = mutableListOf<String>()
        private var savedPersonalVisible = true
    }

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay: Calendar = Calendar.getInstance()
    private var personalVisible: Boolean
        get() = savedPersonalVisible
        set(value) { savedPersonalVisible = value }

    private val selectedFilterTeamIds = mutableListOf<String>().apply {
        addAll(savedFilterTeamIds)
    }

    private var allTeamsFromApi: List<TeamSummary> = emptyList()
    private val dayCountMap = mutableMapOf<String, Int>()
    private var dayResponse: CalendarDayResponse? = null

    private val calendarService: CalendarService by lazy {
        RetrofitClient.getInstance(requireContext()).create(CalendarService::class.java)
    }

    private val teamService: TeamService by lazy {
        RetrofitClient.getInstance(requireContext()).create(TeamService::class.java)
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

        fun selectedTeamIdsAsIntOrNull(): List<Int>? {
            val ids = selectedFilterTeamIds.mapNotNull { it.toIntOrNull() }
            return if (ids.isEmpty()) null else ids
        }

        fun formatMonthParam(year: Int, month0: Int): String {
            return String.format(Locale.US, "%04d-%02d", year, month0 + 1)
        }

        fun formatDateParam(cal: Calendar): String {
            return String.format(
                Locale.US,
                "%04d-%02d-%02d",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun refreshMonthLabel() {
            calendarMonthYear.text = getString(R.string.date_format_year_month, currentYear, currentMonth + 1)
        }

        fun fetchMonthCounts(onDone: () -> Unit) {
            val monthParam = formatMonthParam(currentYear, currentMonth)
            val teamIds = selectedTeamIdsAsIntOrNull()
            calendarService.getMonthlyDayCounts(
                month = monthParam,
                teamIds = teamIds,
                includeMyPersonal = personalVisible
            ).enqueue(object : Callback<CalendarMonthResponse> {
                override fun onResponse(
                    call: Call<CalendarMonthResponse>,
                    response: Response<CalendarMonthResponse>
                ) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        dayCountMap.clear()
                        response.body()?.dayCounts?.forEach { dc ->
                            dayCountMap[dc.date] = dc.count
                        }
                    }
                    onDone()
                }

                override fun onFailure(call: Call<CalendarMonthResponse>, t: Throwable) {
                    if (!isAdded) return
                    onDone()
                }
            })
        }

        fun fetchDaySchedules(onDone: () -> Unit) {
            val dateParam = formatDateParam(selectedDay)
            val teamIds = selectedTeamIdsAsIntOrNull()
            calendarService.getDaySchedules(
                date = dateParam,
                teamIds = teamIds,
                includeMyPersonal = personalVisible
            ).enqueue(object : Callback<CalendarDayResponse> {
                override fun onResponse(
                    call: Call<CalendarDayResponse>,
                    response: Response<CalendarDayResponse>
                ) {
                    if (!isAdded) return
                    if (response.isSuccessful) {
                        dayResponse = response.body()
                    }
                    onDone()
                }

                override fun onFailure(call: Call<CalendarDayResponse>, t: Throwable) {
                    if (!isAdded) return
                    onDone()
                }
            })
        }

        fun refreshDayTasks() {
            calendarSelectedDateText.text = getString(
                R.string.date_format_full,
                selectedDay.get(Calendar.YEAR),
                selectedDay.get(Calendar.MONTH) + 1,
                selectedDay.get(Calendar.DAY_OF_MONTH)
            )

            calendarDayTasksContainer.removeAllViews()

            val data = dayResponse ?: return

            data.teamSchedules.forEach { group ->
                val teamHeader = TextView(requireContext()).apply {
                    text = group.teamName
                    setTextColor(android.graphics.Color.BLACK)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(0, 8.dpToPx(), 0, 4.dpToPx())
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
                }
                calendarDayTasksContainer.addView(teamHeader)

                group.schedules.forEach { sch ->
                    val taskRow = inflater.inflate(R.layout.item_task_row, calendarDayTasksContainer, false)
                    val checkBtn = taskRow.findViewById<ImageButton>(R.id.taskCheck)
                    val titleTv = taskRow.findViewById<TextView>(R.id.taskTitle)

                    titleTv.isClickable = true
                    titleTv.isFocusable = true
                    titleTv.setOnClickListener {
                        findNavController().navigate(
                            R.id.editTeamTaskFragment,
                            Bundle().apply { putString("taskId", sch.scheduleId.toString()) }
                        )
                    }

                    checkBtn.setImageResource(if (sch.isCompleted) R.drawable.ic_schedule_selected else R.drawable.ic_schedule_unselected)
                    titleTv.text = if (sch.masked) "비공개 일정" else sch.title

                    if (sch.isCompleted) {
                        titleTv.paintFlags = titleTv.paintFlags or 0x10
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                    } else {
                        titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }

                    calendarDayTasksContainer.addView(taskRow)
                }
            }

            data.personalSchedules.takeIf { it.isNotEmpty() }?.let { list ->
                val personalHeader = TextView(requireContext()).apply {
                    text = getString(R.string.personal_schedule)
                    setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                }
                calendarDayTasksContainer.addView(personalHeader)

                list.forEach { item ->
                    val personalRow = inflater.inflate(R.layout.item_task_row, calendarDayTasksContainer, false)
                    val checkBtn = personalRow.findViewById<ImageButton>(R.id.taskCheck)
                    val titleTv = personalRow.findViewById<TextView>(R.id.taskTitle)

                    titleTv.isClickable = true
                    titleTv.isFocusable = true
                    titleTv.setOnClickListener {
                        findNavController().navigate(
                            R.id.editPersonalTaskFragment,
                            Bundle().apply { putString("personalId", item.scheduleId.toString()) }
                        )
                    }

                    checkBtn.setImageResource(if (item.isCompleted) R.drawable.ic_schedule_selected else R.drawable.ic_schedule_unselected)
                    titleTv.text = if (item.masked) "비공개 일정" else item.title

                    if (item.isCompleted) {
                        titleTv.paintFlags = titleTv.paintFlags or 0x10
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                    } else {
                        titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }

                    val lockBtn = personalRow.findViewById<ImageButton>(R.id.taskLock)
                    lockBtn.visibility = View.VISIBLE
                    lockBtn.setImageResource(if (item.private) R.drawable.ic_personal_lock else R.drawable.ic_personal_unlock)

                    calendarDayTasksContainer.addView(personalRow)
                }
            }
        }

        fun refreshGrid() {
            calendarMonthGrid.removeAllViews()
            val days = DummyRepository.getMonthCalendarDaysCurrentMonthOnly(currentYear, currentMonth + 1)
            days.forEachIndexed { index, dayOrNull ->
                val cell = inflater.inflate(R.layout.item_calendar_day, calendarMonthGrid, false)
                val dayNumber = cell.findViewById<TextView>(R.id.calendarDayNumber)
                val countBadge = cell.findViewById<View>(R.id.calendarDayEventCountBadge)
                val countTv = cell.findViewById<TextView>(R.id.calendarDayEventCount)
                val dayRoot = cell.findViewById<View>(R.id.calendarDayRoot)

                if (dayOrNull == null) {
                    dayNumber.text = ""
                    dayNumber.visibility = View.INVISIBLE
                    countBadge.visibility = View.GONE
                    dayRoot.setBackgroundResource(0)
                    cell.isClickable = false
                } else {
                    val day = dayOrNull
                    dayNumber.visibility = View.VISIBLE
                    dayNumber.text = day.get(Calendar.DAY_OF_MONTH).toString()

                    val today = Calendar.getInstance()
                    val isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                            day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)

                    val isSelected = day.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) &&
                            day.get(Calendar.MONTH) == selectedDay.get(Calendar.MONTH) &&
                            day.get(Calendar.DAY_OF_MONTH) == selectedDay.get(Calendar.DAY_OF_MONTH)

                    dayRoot.setBackgroundResource(
                        when {
                            isSelected -> R.drawable.bg_calendar_day_selected
                            isToday -> R.drawable.bg_calendar_today
                            else -> 0
                        }
                    )

                    cell.setOnClickListener {
                        selectedDay.timeInMillis = day.timeInMillis
                        refreshGrid()
                        fetchDaySchedules { refreshDayTasks() }
                    }

                    val dateKey = formatDateParam(day)
                    val count = dayCountMap[dateKey] ?: 0
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
            allTeamsFromApi.filter { it.id.toString() in selectedFilterTeamIds }.forEach { team ->
                val chip = inflater.inflate(R.layout.item_team_chip, calendarFilterChips, false)
                val chipTv = chip.findViewById<TextView>(R.id.chipTeamName)
                chipTv.text = team.name

                val teamColor = android.graphics.Color.parseColor(team.colorHex ?: "#CCCCCC")
                val tr = android.graphics.Color.red(teamColor)
                val tg = android.graphics.Color.green(teamColor)
                val tb = android.graphics.Color.blue(teamColor)
                val pastelBg = android.graphics.Color.rgb(
                    (tr * 0.35f + 255 * 0.65f).toInt().coerceIn(0, 255),
                    (tg * 0.35f + 255 * 0.65f).toInt().coerceIn(0, 255),
                    (tb * 0.35f + 255 * 0.65f).toInt().coerceIn(0, 255)
                )
                val radiusPx = 8 * resources.displayMetrics.density
                chipTv.background = GradientDrawable().apply {
                    setColor(pastelBg)
                    setStroke((2 * resources.displayMetrics.density).toInt(), pastelBg)
                    cornerRadius = radiusPx
                }

                val chipRemoveWrap = chip.findViewById<View>(R.id.chipRemoveWrap)
                val onRemove: (View) -> Unit = {
                    selectedFilterTeamIds.remove(team.id.toString())
                    refreshChips()
                    fetchMonthCounts { refreshGrid() }
                    fetchDaySchedules { refreshDayTasks() }
                }
                chipRemoveWrap.setOnClickListener(onRemove)
                calendarFilterChips.addView(chip)

                chip.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        chip.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val rect = Rect()
                        chipRemoveWrap.getHitRect(rect)
                        rect.left -= 56.dpToPx()
                        rect.top -= 80.dpToPx()
                        rect.right += 56.dpToPx()
                        rect.bottom += 40.dpToPx()
                        chip.touchDelegate = TouchDelegate(rect, chipRemoveWrap)
                    }
                })
            }
        }

        fun fetchMyTeams() {
            teamService.getMyTeams().enqueue(object : Callback<List<TeamSummary>> {
                override fun onResponse(call: Call<List<TeamSummary>>, response: Response<List<TeamSummary>>) {
                    if (response.isSuccessful) {
                        allTeamsFromApi = response.body() ?: emptyList()
                        if (savedFilterTeamIds.isEmpty()) {
                            selectedFilterTeamIds.clear()
                            selectedFilterTeamIds.addAll(allTeamsFromApi.map { it.id.toString() })
                        }
                        refreshChips()
                        fetchMonthCounts { refreshGrid() }
                        fetchDaySchedules { refreshDayTasks() }
                    }
                }
                override fun onFailure(call: Call<List<TeamSummary>>, t: Throwable) {
                    Log.e("API_ERROR", "Fetch teams failed: ${t.message}")
                }
            })
        }

        fun showTeamFilterDialog() {
            val dialogView = layoutInflater.inflate(R.layout.dialog_calendar_filter, null)
            val listContainer = dialogView.findViewById<LinearLayout>(R.id.dialogFilterList)
            val confirmBtn = dialogView.findViewById<android.widget.Button>(R.id.dialogFilterConfirm)
            val checkStates = allTeamsFromApi.map { it.id.toString() in selectedFilterTeamIds }.toMutableList()
            listContainer.removeAllViews()

            allTeamsFromApi.forEachIndexed { index, team ->
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
                checkStates.forEachIndexed { i, c -> if (c) selectedFilterTeamIds.add(allTeamsFromApi[i].id.toString()) }
                refreshChips()
                fetchMonthCounts { refreshGrid() }
                fetchDaySchedules { refreshDayTasks() }
                dialog.dismiss()
            }
            dialog.show()
        }

        headerCalendarFilter.setOnClickListener { showTeamFilterDialog() }

        calendarPersonalToggle.setOnClickListener {
            personalVisible = !personalVisible
            calendarPersonalToggle.setImageResource(if (personalVisible) R.drawable.ic_personal_on else R.drawable.ic_personal_off)
            fetchMonthCounts { refreshGrid() }
            fetchDaySchedules { refreshDayTasks() }
        }

        calendarPrevMonth.setOnClickListener {
            if (currentMonth == 0) { currentYear--; currentMonth = 11 } else currentMonth--
            refreshMonthLabel()
            fetchMonthCounts { refreshGrid() }
            fetchDaySchedules { refreshDayTasks() }
        }

        calendarNextMonth.setOnClickListener {
            if (currentMonth == 11) { currentYear++; currentMonth = 0 } else currentMonth++
            refreshMonthLabel()
            fetchMonthCounts { refreshGrid() }
            fetchDaySchedules { refreshDayTasks() }
        }

        refreshMonthLabel()
        calendarPersonalToggle.setImageResource(if (personalVisible) R.drawable.ic_personal_on else R.drawable.ic_personal_off)
        fetchMyTeams()

        calendarFabAdd.setOnClickListener {
            findNavController().navigate(R.id.addPersonalTaskFragment)
        }

        return root
    }
}