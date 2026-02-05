package com.project.unimate.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository
import java.util.Calendar

class HomeFragment : Fragment() {

    private var weekAnchor: Calendar = Calendar.getInstance()
    private var selectedDay: Calendar = Calendar.getInstance().apply { timeInMillis = weekAnchor.timeInMillis }
    private var isChecklistExpanded = false
    private val maxCollapsedPersonalItems = 3


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

        val homeMonthYear = root.findViewById<TextView>(R.id.homeMonthYear)
        val homePrevWeek = root.findViewById<ImageButton>(R.id.homePrevWeek)
        val homeNextWeek = root.findViewById<ImageButton>(R.id.homeNextWeek)
        val homeWeekDatesContainer = root.findViewById<LinearLayout>(R.id.homeWeekDatesContainer)
        val homeTodayTasksContainer = root.findViewById<LinearLayout>(R.id.homeTodayTasksContainer)
        val homePersonalTasksContainer = root.findViewById<LinearLayout>(R.id.homePersonalTasksContainer)
        val homeExpandArrow = root.findViewById<ImageButton>(R.id.homeExpandArrow)
        val homeTeamSpaceIcons = root.findViewById<LinearLayout>(R.id.homeTeamSpaceIcons)
        val homePersonalLabel = root.findViewById<TextView>(R.id.homePersonalLabel)
        val homeTodayCard = root.findViewById<com.google.android.material.card.MaterialCardView>(R.id.homeTodayCard)
        val homeCardInner = root.findViewById<LinearLayout>(R.id.homeTodayCardInner)
        val homeCardContentWrapper = root.findViewById<android.widget.ScrollView>(R.id.homeTodayCardContentWrapper)

        fun refreshTodayTasks() {
            homeTodayTasksContainer.removeAllViews()
            val today = selectedDay
            val byTeam = DummyRepository.getTodayTasksByTeam(today)
            val personalList = DummyRepository.getPersonalForToday(today)
            val hasAnySchedule = byTeam.isNotEmpty() || personalList.isNotEmpty()
            val teamMap = DummyRepository.allTeams.associateBy { it.id }

            if (!hasAnySchedule) {
                val emptyTv = TextView(requireContext()).apply {
                    text = getString(R.string.no_schedule)
                    setPadding(0, 24.dpToPx(), 0, 24.dpToPx())
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                }
                homeTodayTasksContainer.addView(emptyTv)
                homePersonalLabel.visibility = View.GONE
                homePersonalTasksContainer.removeAllViews()
                homeExpandArrow.visibility = View.GONE
                homeCardContentWrapper.layoutParams = homeCardContentWrapper.layoutParams?.apply { height = ViewGroup.LayoutParams.WRAP_CONTENT }
                return@refreshTodayTasks
            }

            byTeam.forEach { (teamId, tasks) ->
                val team = teamMap[teamId] ?: return@forEach
                val teamHeader = TextView(requireContext()).apply {
                    text = team.name
                    setTextColor(Color.parseColor(team.colorHex))
                    setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                }
                homeTodayTasksContainer.addView(teamHeader)
                tasks.forEach { task ->
                    val row = inflater.inflate(R.layout.item_task_row, homeTodayTasksContainer, false)
                    val checkBtn = row.findViewById<ImageButton>(R.id.taskCheck)
                    val titleTv = row.findViewById<TextView>(R.id.taskTitle)
                    checkBtn.setImageResource(if (task.isChecked) R.drawable.ic_check_on else R.drawable.ic_check_off)
                    titleTv.text = task.title
                    if (task.isChecked) {
                        titleTv.paintFlags = titleTv.paintFlags or 0x10
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                    } else {
                        titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                    row.findViewById<ImageButton>(R.id.taskLock).visibility = View.GONE
                    checkBtn.setOnClickListener {
                        DummyRepository.setTaskChecked(task.id, !task.isChecked)
                        refreshTodayTasks()
                    }
                homeTodayTasksContainer.addView(row)
            }
            }

            homePersonalLabel.visibility = if (personalList.isEmpty()) View.GONE else View.VISIBLE
            homePersonalTasksContainer.removeAllViews()
            homeExpandArrow.visibility = View.VISIBLE
            homeExpandArrow.setImageResource(if (isChecklistExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)
            if (personalList.isNotEmpty()) {
                val toShow = if (isChecklistExpanded) personalList else personalList.take(maxCollapsedPersonalItems)
                toShow.forEach { item ->
                    val row = inflater.inflate(R.layout.item_task_row, homePersonalTasksContainer, false)
                    val checkBtn = row.findViewById<ImageButton>(R.id.taskCheck)
                    val titleTv = row.findViewById<TextView>(R.id.taskTitle)
                    checkBtn.setImageResource(if (item.isChecked) R.drawable.ic_check_on else R.drawable.ic_check_off)
                    titleTv.text = item.title
                    if (item.isChecked) {
                        titleTv.paintFlags = titleTv.paintFlags or 0x10
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                    } else {
                        titleTv.paintFlags = titleTv.paintFlags and 0x10.inv()
                        titleTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    }
                    val lockBtn = row.findViewById<ImageButton>(R.id.taskLock)
                    lockBtn.visibility = View.VISIBLE
                    lockBtn.setImageResource(if (item.isLocked) R.drawable.ic_personal_lock else R.drawable.ic_personal_unlock)
                    lockBtn.setOnClickListener {
                        DummyRepository.setPersonalLocked(item.id, !item.isLocked)
                        refreshTodayTasks()
                    }
                    checkBtn.setOnClickListener {
                        DummyRepository.setPersonalChecked(item.id, !item.isChecked)
                        refreshTodayTasks()
                    }
                    homePersonalTasksContainer.addView(row)
                }
            }
            val arrowHeightPx = (40 * resources.displayMetrics.density).toInt()
            val collapsedContentH = (380 * resources.displayMetrics.density).toInt() - arrowHeightPx - 16
            val lp = homeCardContentWrapper.layoutParams
                ?: LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            if (isChecklistExpanded) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                homeCardContentWrapper.setMinimumHeight(collapsedContentH.coerceAtLeast(0))
            } else {
                homeCardContentWrapper.setMinimumHeight(0)
                lp.height = collapsedContentH.coerceAtLeast(0)
            }
            homeCardContentWrapper.layoutParams = lp
        }

        fun refreshWeek() {
            homeMonthYear.text = getString(R.string.date_format_year_month, weekAnchor.get(Calendar.YEAR), weekAnchor.get(Calendar.MONTH) + 1)
            val weekDates = DummyRepository.getWeekDates(weekAnchor)
            homeWeekDatesContainer.removeAllViews()
            val allTeamIds = DummyRepository.allTeams.map { it.id }

            weekDates.forEach { day ->
                val column = inflater.inflate(R.layout.item_home_week_column, homeWeekDatesContainer, false) as LinearLayout
                val dateTv = column.findViewById<TextView>(R.id.weekDateNumber)
                val countBadge = column.findViewById<View>(R.id.weekEventCountBadge)
                val countTv = column.findViewById<TextView>(R.id.weekEventCount)
                dateTv.text = day.get(Calendar.DAY_OF_MONTH).toString()
                val isSelected = day.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR) &&
                    day.get(Calendar.DAY_OF_YEAR) == selectedDay.get(Calendar.DAY_OF_YEAR)
                column.setBackgroundResource(if (isSelected) R.drawable.bg_home_day_selected else 0)
                column.setOnClickListener {
                    selectedDay.timeInMillis = day.timeInMillis
                    refreshWeek()
                    refreshTodayTasks()
                }
                val count = DummyRepository.getDayEventCount(day, allTeamIds)
                if (count > 0) {
                    countBadge.visibility = View.VISIBLE
                    countTv.text = count.toString()
                } else {
                    countBadge.visibility = View.GONE
                }
                homeWeekDatesContainer.addView(column)
            }
        }

        homeExpandArrow.setOnClickListener {
            isChecklistExpanded = !isChecklistExpanded
            refreshTodayTasks()
        }

        homePrevWeek.setOnClickListener {
            weekAnchor.add(Calendar.WEEK_OF_YEAR, -1)
            refreshWeek()
            refreshTodayTasks()
        }
        homeNextWeek.setOnClickListener {
            weekAnchor.add(Calendar.WEEK_OF_YEAR, 1)
            refreshWeek()
            refreshTodayTasks()
        }

        refreshWeek()
        refreshTodayTasks()

        DummyRepository.getMyTeamSpaceTeams().forEach { team ->
            val item = inflater.inflate(R.layout.item_home_team_icon, homeTeamSpaceIcons, false)
            item.isClickable = true
            item.isFocusable = true
            item.setOnClickListener { /* 버튼으로만, 기능 없음 */ }
            val card = item.findViewById<com.google.android.material.card.MaterialCardView>(R.id.teamIconCard)
            card.strokeColor = Color.parseColor(team.colorHex)
            val resId = resources.getIdentifier(team.imageResName, "drawable", requireContext().packageName)
            if (resId != 0) item.findViewById<ImageView>(R.id.teamIconImage).setImageResource(resId)
            item.findViewById<TextView>(R.id.teamIconName).text = team.name
            homeTeamSpaceIcons.addView(item)
        }
        val plusBtn = inflater.inflate(R.layout.item_home_team_plus, homeTeamSpaceIcons, false)
        homeTeamSpaceIcons.addView(plusBtn)
        (plusBtn.layoutParams as? LinearLayout.LayoutParams)?.gravity = android.view.Gravity.CENTER_VERTICAL

        return root
    }

}
