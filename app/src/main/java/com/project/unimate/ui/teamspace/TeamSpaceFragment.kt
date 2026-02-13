package com.project.unimate.ui.teamspace

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.project.unimate.R
import com.project.unimate.data.entity.TeamMember
import com.project.unimate.data.repository.DummyRepository
import java.util.Calendar

class TeamSpaceFragment : Fragment() {

    private val teamId: String
        get() = arguments?.getString(ARG_TEAM_ID) ?: ""

    companion object {
        const val ARG_TEAM_ID = "teamId"
    }

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay: Calendar = Calendar.getInstance()
    private var isIntroExpanded = false
    private var isCalendarTeamMode = true // true = 팀, false = 개인

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_team_space, container, false)
        val team = DummyRepository.getTeamById(teamId) ?: run {
            findNavController().navigateUp()
            return root
        }

        fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

        val teamSpaceBack = root.findViewById<ImageButton>(R.id.teamSpaceBack)
        val teamSpaceEdit = root.findViewById<TextView>(R.id.teamSpaceEdit)
        val teamSpaceColorCircle = root.findViewById<View>(R.id.teamSpaceColorCircle)
        val teamSpaceTeamName = root.findViewById<TextView>(R.id.teamSpaceTeamName)
        val teamSpaceShare = root.findViewById<ImageButton>(R.id.teamSpaceShare)
        val teamSpaceIntroTitle = root.findViewById<TextView>(R.id.teamSpaceIntroTitle)
        val teamSpaceIntroText = root.findViewById<TextView>(R.id.teamSpaceIntroText)
        val teamSpaceIntroContentWrapper = root.findViewById<View>(R.id.teamSpaceIntroContentWrapper)
        val teamSpaceIntroExpandArrow = root.findViewById<ImageButton>(R.id.teamSpaceIntroExpandArrow)
        val teamSpaceMembersTitle = root.findViewById<TextView>(R.id.teamSpaceMembersTitle)
        val teamSpaceMembersCount = root.findViewById<TextView>(R.id.teamSpaceMembersCount)
        val teamSpaceMembersIcons = root.findViewById<LinearLayout>(R.id.teamSpaceMembersIcons)
        val teamSpaceScheduleLabel = root.findViewById<TextView>(R.id.teamSpaceScheduleLabel)
        val teamSpaceScheduleCount = root.findViewById<TextView>(R.id.teamSpaceScheduleCount)
        val teamSpaceMonthYear = root.findViewById<TextView>(R.id.teamSpaceMonthYear)
        val teamSpacePrevMonth = root.findViewById<ImageButton>(R.id.teamSpacePrevMonth)
        val teamSpaceNextMonth = root.findViewById<ImageButton>(R.id.teamSpaceNextMonth)
        val teamSpaceCalendarTeamPersonalToggle = root.findViewById<View>(R.id.teamSpaceCalendarTeamPersonalToggle)
        val teamSpaceCalendarToggleLabel = root.findViewById<TextView>(R.id.teamSpaceCalendarToggleLabel)
        val teamSpaceMonthGrid = root.findViewById<GridLayout>(R.id.teamSpaceMonthGrid)
        val teamSpaceSelectMeetingDate = root.findViewById<View>(R.id.teamSpaceSelectMeetingDate)
        val teamSpaceSelectedDateText = root.findViewById<TextView>(R.id.teamSpaceSelectedDateText)
        val teamSpaceDayTasksContainer = root.findViewById<LinearLayout>(R.id.teamSpaceDayTasksContainer)
        val teamSpaceNoScheduleCard = root.findViewById<MaterialCardView>(R.id.teamSpaceNoScheduleCard)
        val teamSpaceNoScheduleMembersInner = root.findViewById<LinearLayout>(R.id.teamSpaceNoScheduleMembersInner)
        val teamSpaceFab = root.findViewById<ImageButton>(R.id.teamSpaceFab)

        teamSpaceBack.setOnClickListener { findNavController().navigateUp() }
        teamSpaceEdit.setOnClickListener {
            findNavController().navigate(R.id.editTeamSpaceFragment, Bundle().apply { putString("teamId", teamId) })
        }
        teamSpaceShare.setOnClickListener { /* 추후 연결 */ }
        teamSpaceSelectMeetingDate.setOnClickListener { /* 추후 연결 */ }
        teamSpaceFab.setOnClickListener {
            findNavController().navigate(R.id.addTeamTaskFragment, Bundle().apply { putString("teamId", teamId) })
        }

        val teamColor = android.graphics.Color.parseColor(team.colorHex)
        teamSpaceColorCircle.background = GradientDrawable().apply {
            setColor(teamColor)
            shape = GradientDrawable.OVAL
        }
        teamSpaceTeamName.text = team.name
        teamSpaceIntroTitle.text = getString(R.string.team_intro_suffix).let { "${team.name} $it" }
        teamSpaceIntroTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray08))
        val introText = DummyRepository.getTeamIntro(teamId)
        teamSpaceIntroText.text = introText
        teamSpaceIntroText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray07))

        val collapsedIntroHeightPx = (85 * resources.displayMetrics.density).toInt()
        fun refreshIntroHeight() {
            teamSpaceIntroExpandArrow.setImageResource(if (isIntroExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
            val lp = teamSpaceIntroContentWrapper.layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.height = if (isIntroExpanded) ViewGroup.LayoutParams.WRAP_CONTENT else collapsedIntroHeightPx.coerceAtLeast(0)
            teamSpaceIntroContentWrapper.layoutParams = lp
        }
        teamSpaceIntroExpandArrow.setOnClickListener {
            isIntroExpanded = !isIntroExpanded
            refreshIntroHeight()
        }
        refreshIntroHeight()

        val members = DummyRepository.getTeamMembers(teamId)
        teamSpaceMembersTitle.text = "함께하는 팀원"
        teamSpaceMembersCount.text = members.size.toString()
        teamSpaceMembersIcons.removeAllViews()
        members.forEach { member ->
            val item = inflater.inflate(R.layout.item_team_space_member, teamSpaceMembersIcons, false)
            val card = item.findViewById<MaterialCardView>(R.id.teamMemberCard)
            card.strokeColor = teamColor
            val resId = resources.getIdentifier(member.iconResName, "drawable", requireContext().packageName)
            if (resId != 0) item.findViewById<ImageView>(R.id.teamMemberIcon).setImageResource(resId)
            item.findViewById<TextView>(R.id.teamMemberName).text = member.name
            item.findViewById<TextView>(R.id.teamMemberName).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray07))
            teamSpaceMembersIcons.addView(item)
        }

        val scheduleCount = DummyRepository.getTeamScheduleCount(teamId)
        teamSpaceScheduleCount.text = scheduleCount.toString()

        fun refreshMonthLabel() {
            teamSpaceMonthYear.text = getString(R.string.date_format_year_month, currentYear, currentMonth + 1)
        }

        fun addTaskRow(
            title: String,
            isChecked: Boolean,
            id: String,
            container: LinearLayout,
            inflater: LayoutInflater,
            onRefresh: () -> Unit,
            isPersonal: Boolean = false,
            isLocked: Boolean = false
        ) {
            val taskRow = inflater.inflate(R.layout.item_task_row, container, false)
            val checkBtn = taskRow.findViewById<ImageButton>(R.id.taskCheck)
            checkBtn.layoutParams = checkBtn.layoutParams?.apply {
                width = 46.dpToPx()
                height = 46.dpToPx()
            } ?: LinearLayout.LayoutParams(46.dpToPx(), 46.dpToPx())
            val titleTv = taskRow.findViewById<TextView>(R.id.taskTitle)
            titleTv.isClickable = true
            titleTv.isFocusable = true
            titleTv.setOnClickListener {
                if (isPersonal) findNavController().navigate(R.id.editPersonalTaskFragment, Bundle().apply { putString("personalId", id) })
                else findNavController().navigate(R.id.editTeamTaskFragment, Bundle().apply { putString("taskId", id) })
            }
            checkBtn.setImageResource(if (isChecked) R.drawable.ic_check_on else R.drawable.ic_check_off)
            titleTv.text = title
            if (isChecked) {
                titleTv.paintFlags = titleTv.paintFlags or 0x10
                titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
            } else {
                titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            checkBtn.setOnClickListener {
                if (isPersonal) DummyRepository.setPersonalChecked(id, !isChecked)
                else DummyRepository.setTaskChecked(id, !isChecked)
                onRefresh()
            }
            val lockBtn = taskRow.findViewById<ImageButton>(R.id.taskLock)
            if (isPersonal) {
                lockBtn.visibility = View.VISIBLE
                lockBtn.setImageResource(if (isLocked) R.drawable.ic_personal_lock else R.drawable.ic_personal_unlock)
                lockBtn.setOnClickListener {
                    DummyRepository.setPersonalLocked(id, !isLocked)
                    onRefresh()
                }
            } else {
                lockBtn.visibility = View.GONE
            }
            container.addView(taskRow)
        }

        fun refreshDayTasks() {
            teamSpaceSelectedDateText.text = getString(R.string.date_format_month_day,
                selectedDay.get(Calendar.MONTH) + 1, selectedDay.get(Calendar.DAY_OF_MONTH))
            teamSpaceDayTasksContainer.removeAllViews()
            if (isCalendarTeamMode) {
                val tasks = DummyRepository.getTasksForDate(selectedDay, listOf(teamId))
                val byCreator = tasks.groupBy { it.creatorName ?: "" }
                byCreator.forEach { (creatorName, taskList) ->
                    if (creatorName.isNotEmpty()) {
                        val header = TextView(requireContext()).apply {
                            text = creatorName
                            setTextColor(android.graphics.Color.BLACK)
                            setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
                            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
                        }
                        teamSpaceDayTasksContainer.addView(header)
                    }
                    taskList.forEach { task ->
                        addTaskRow(task.title, task.isChecked, task.id, teamSpaceDayTasksContainer, inflater, ::refreshDayTasks)
                    }
                }
            } else {
                val personalList = DummyRepository.getPersonalForDate(selectedDay)
                if (personalList.isNotEmpty()) {
                    val header = TextView(requireContext()).apply {
                        text = getString(R.string.personal_schedule)
                        setTextColor(android.graphics.Color.BLACK)
                        setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
                        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
                    }
                    teamSpaceDayTasksContainer.addView(header)
                }
                personalList.forEach { item ->
                    addTaskRow(item.title, item.isChecked, item.id, teamSpaceDayTasksContainer, inflater, ::refreshDayTasks, isPersonal = true, isLocked = item.isLocked)
                }
            }
        }

        fun refreshNoScheduleMembers() {
            val noSchedule = DummyRepository.getNoScheduleMembers(teamId, selectedDay)
            teamSpaceNoScheduleMembersInner.removeAllViews()
            if (noSchedule.isEmpty()) {
                teamSpaceNoScheduleCard.visibility = View.GONE
                return
            }
            teamSpaceNoScheduleCard.visibility = View.VISIBLE
            noSchedule.forEach { member ->
                val item = inflater.inflate(R.layout.item_team_space_member, teamSpaceNoScheduleMembersInner, false)
                val memberCard = item.findViewById<MaterialCardView>(R.id.teamMemberCard)
                memberCard.strokeColor = teamColor
                val resId = resources.getIdentifier(member.iconResName, "drawable", requireContext().packageName)
                if (resId != 0) item.findViewById<ImageView>(R.id.teamMemberIcon).setImageResource(resId)
                item.findViewById<TextView>(R.id.teamMemberName).text = member.name
                item.findViewById<TextView>(R.id.teamMemberName).setTextColor(ContextCompat.getColor(requireContext(), R.color.gray07))
                (item.layoutParams as? LinearLayout.LayoutParams)?.marginEnd = 12.dpToPx()
                teamSpaceNoScheduleMembersInner.addView(item)
            }
        }

        fun refreshGrid() {
            teamSpaceMonthGrid.removeAllViews()
            val days = DummyRepository.getMonthCalendarDaysCurrentMonthOnly(currentYear, currentMonth + 1)
            val badgeSize = 12.dpToPx()
            days.forEachIndexed { index, dayOrNull ->
                val cell = inflater.inflate(R.layout.item_calendar_day, teamSpaceMonthGrid, false)
                val dayNumber = cell.findViewById<TextView>(R.id.calendarDayNumber)
                val countBadge = cell.findViewById<FrameLayout>(R.id.calendarDayEventCountBadge)
                val cellRoot = cell.findViewById<View>(R.id.calendarDayRoot)
                if (dayOrNull == null) {
                    dayNumber.text = ""
                    dayNumber.visibility = View.INVISIBLE
                    countBadge.visibility = View.GONE
                    cellRoot.setBackgroundResource(0)
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
                    cellRoot.setBackgroundResource(
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
                        refreshNoScheduleMembers()
                    }
                    val count = if (isCalendarTeamMode) {
                        DummyRepository.getDayEventCountForTeam(teamId, day)
                    } else {
                        DummyRepository.getPersonalForDate(day).size
                    }
                    if (count > 0) {
                        countBadge.visibility = View.VISIBLE
                        countBadge.background = null
                        countBadge.removeAllViews()
                        val badgeRow = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER
                        }
                        val circle = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize)
                            background = GradientDrawable().apply {
                                setColor(teamColor)
                                shape = GradientDrawable.OVAL
                            }
                        }
                        val countText = TextView(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                marginStart = 4.dpToPx()
                            }
                            text = count.toString()
                            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                            setTypeface(ResourcesCompat.getFont(requireContext(), R.font.pretendard_medium))
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        }
                        badgeRow.addView(circle)
                        badgeRow.addView(countText)
                        countBadge.addView(badgeRow)
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
                teamSpaceMonthGrid.addView(cell, params)
            }
        }

        teamSpaceCalendarTeamPersonalToggle.isClickable = true
        teamSpaceCalendarTeamPersonalToggle.setOnClickListener {
            isCalendarTeamMode = !isCalendarTeamMode
            teamSpaceCalendarToggleLabel.text = if (isCalendarTeamMode) getString(R.string.team_calendar_team_label) else getString(R.string.personal)
            refreshGrid()
            refreshDayTasks()
        }

        refreshMonthLabel()
        refreshGrid()
        refreshDayTasks()
        refreshNoScheduleMembers()

        teamSpacePrevMonth.setOnClickListener {
            if (currentMonth == 0) { currentYear--; currentMonth = 11 } else currentMonth--
            refreshMonthLabel()
            refreshGrid()
            refreshDayTasks()
            refreshNoScheduleMembers()
        }
        teamSpaceNextMonth.setOnClickListener {
            if (currentMonth == 11) { currentYear++; currentMonth = 0 } else currentMonth++
            refreshMonthLabel()
            refreshGrid()
            refreshDayTasks()
            refreshNoScheduleMembers()
        }

        return root
    }
}
