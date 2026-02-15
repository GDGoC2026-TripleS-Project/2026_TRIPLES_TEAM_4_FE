package com.project.unimate.ui.teamspace

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.project.unimate.R
import com.project.unimate.model.ScheduleDetail
import com.project.unimate.model.TeamDetailResponse
import com.project.unimate.model.TeamMember // TeamMember 모델 필요
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.TeamService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TeamSpaceFragment : Fragment() {

    private val teamIdStr: String
        get() = arguments?.getString(ARG_TEAM_ID) ?: ""

    private val teamIdLong: Long
        get() = teamIdStr.toLongOrNull() ?: 0L

    companion object {
        const val ARG_TEAM_ID = "teamId"
    }

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var selectedDay: Calendar = Calendar.getInstance()
    private var isIntroExpanded = false
    private var isCalendarTeamMode = true

    private val serverDayCounts = mutableMapOf<String, Int>()

    private lateinit var teamSpaceColorCircle: View
    private lateinit var teamSpaceTeamName: TextView
    private lateinit var teamSpaceIntroTitle: TextView
    private lateinit var teamSpaceIntroText: TextView
    private lateinit var teamSpaceMembersCount: TextView
    private lateinit var teamSpaceMembersIcons: LinearLayout
    private lateinit var teamSpaceIntroContentWrapper: View
    private lateinit var teamSpaceIntroExpandArrow: ImageButton
    private lateinit var teamSpaceDayTasksContainer: LinearLayout
    private lateinit var teamSpaceSelectedDateText: TextView
    private lateinit var teamSpaceCalendarToggleLabel: TextView
    private lateinit var teamSpaceCalendarTeamPersonalToggle: View
    private lateinit var teamSpaceMonthGrid: GridLayout
    private lateinit var teamSpaceMonthYear: TextView

    private lateinit var teamService: TeamService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_team_space, container, false)
        teamService = RetrofitClient.getInstance(requireContext()).create(TeamService::class.java)

        val teamSpaceBack = root.findViewById<ImageButton>(R.id.teamSpaceBack)
        val teamSpaceEdit = root.findViewById<TextView>(R.id.teamSpaceEdit)
        teamSpaceColorCircle = root.findViewById<View>(R.id.teamSpaceColorCircle)
        teamSpaceTeamName = root.findViewById<TextView>(R.id.teamSpaceTeamName)
        val teamSpaceShare = root.findViewById<ImageButton>(R.id.teamSpaceShare)
        teamSpaceIntroTitle = root.findViewById<TextView>(R.id.teamSpaceIntroTitle)
        teamSpaceIntroText = root.findViewById<TextView>(R.id.teamSpaceIntroText)
        teamSpaceIntroContentWrapper = root.findViewById<View>(R.id.teamSpaceIntroContentWrapper)
        teamSpaceIntroExpandArrow = root.findViewById<ImageButton>(R.id.teamSpaceIntroExpandArrow)

        val teamSpaceMembersTitle = root.findViewById<TextView>(R.id.teamSpaceMembersTitle)
        teamSpaceMembersCount = root.findViewById<TextView>(R.id.teamSpaceMembersCount)
        teamSpaceMembersIcons = root.findViewById<LinearLayout>(R.id.teamSpaceMembersIcons)

        teamSpaceMonthYear = root.findViewById<TextView>(R.id.teamSpaceMonthYear)
        val teamSpacePrevMonth = root.findViewById<ImageButton>(R.id.teamSpacePrevMonth)
        val teamSpaceNextMonth = root.findViewById<ImageButton>(R.id.teamSpaceNextMonth)
        teamSpaceMonthGrid = root.findViewById<GridLayout>(R.id.teamSpaceMonthGrid)

        val teamSpaceSelectMeetingDate = root.findViewById<View>(R.id.teamSpaceSelectMeetingDate)
        val teamSpaceFab = root.findViewById<ImageButton>(R.id.teamSpaceFab)

        teamSpaceDayTasksContainer = root.findViewById<LinearLayout>(R.id.teamSpaceDayTasksContainer)
        teamSpaceSelectedDateText = root.findViewById<TextView>(R.id.teamSpaceSelectedDateText)
        teamSpaceCalendarToggleLabel = root.findViewById<TextView>(R.id.teamSpaceCalendarToggleLabel)
        teamSpaceCalendarTeamPersonalToggle = root.findViewById<View>(R.id.teamSpaceCalendarTeamPersonalToggle)

        teamSpaceBack.setOnClickListener { findNavController().navigateUp() }

        teamSpaceEdit.setOnClickListener {
            findNavController().navigate(R.id.editTeamSpaceFragment, Bundle().apply { putString("teamId", teamIdStr) })
        }

        teamSpaceShare.setOnClickListener {
            findNavController().navigate(R.id.action_teamSpace_to_teamShare, Bundle().apply {
                putString("teamId", teamIdStr)
            })
        }

        teamSpaceSelectMeetingDate.setOnClickListener {
            findNavController().navigate(R.id.createTimepickFragment, Bundle().apply { putString("teamId", teamIdStr) })
        }

        teamSpaceFab.setOnClickListener {
            if (isCalendarTeamMode) {
                findNavController().navigate(R.id.addTeamTaskFragment, Bundle().apply { putString("teamId", teamIdStr) })
            } else {
                Toast.makeText(context, "개인 일정 추가 기능 준비 중", Toast.LENGTH_SHORT).show()
            }
        }

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

        fun refreshMonthLabel() {
            teamSpaceMonthYear.text = getString(R.string.date_format_year_month, currentYear, currentMonth + 1)
        }

        teamSpacePrevMonth.setOnClickListener {
            if (currentMonth == 0) { currentYear--; currentMonth = 11 } else currentMonth--
            refreshMonthLabel()
            fetchMonthlyData()
        }

        teamSpaceNextMonth.setOnClickListener {
            if (currentMonth == 11) { currentYear++; currentMonth = 0 } else currentMonth++
            refreshMonthLabel()
            fetchMonthlyData()
        }
        refreshMonthLabel()

        teamSpaceCalendarTeamPersonalToggle.setOnClickListener {
            isCalendarTeamMode = !isCalendarTeamMode
            teamSpaceCalendarToggleLabel.text = if (isCalendarTeamMode) getString(R.string.team_calendar_team_label) else getString(R.string.personal)
            fetchMonthlyData()
            fetchDateSchedules()
        }

        fetchTeamDetail()
        fetchDateSchedules()
        fetchMonthlyData()
        fetchTeamMembers() // 🔥 [추가] 팀원 목록 별도 호출

        return root
    }

    private fun fetchTeamDetail() {
        teamService.getTeamDetail(teamIdLong).enqueue(object : Callback<TeamDetailResponse> {
            override fun onResponse(call: Call<TeamDetailResponse>, response: Response<TeamDetailResponse>) {
                if (response.isSuccessful) response.body()?.let { updateTeamInfoUI(it) }
            }
            override fun onFailure(call: Call<TeamDetailResponse>, t: Throwable) {}
        })
    }

    // 🔥 [추가] 팀원 목록 조회 API 호출 함수
    private fun fetchTeamMembers() {
        teamService.getTeamMembers(teamIdLong).enqueue(object : Callback<List<TeamMember>> {
            override fun onResponse(call: Call<List<TeamMember>>, response: Response<List<TeamMember>>) {
                if (response.isSuccessful) {
                    val members = response.body() ?: emptyList()
                    updateMemberUI(members)
                }
            }
            override fun onFailure(call: Call<List<TeamMember>>, t: Throwable) {
                Log.e("TeamSpace", "멤버 로드 실패: ${t.message}")
            }
        })
    }

    private fun fetchDateSchedules() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDay.time)
        teamSpaceSelectedDateText.text = getString(R.string.date_format_month_day,
            selectedDay.get(Calendar.MONTH) + 1, selectedDay.get(Calendar.DAY_OF_MONTH))
        teamSpaceDayTasksContainer.removeAllViews()
        val call = if (isCalendarTeamMode) {
            teamService.getTeamSchedulesDaily(teamIdLong, dateStr)
        } else {
            teamService.getMySchedulesDaily(teamIdLong, dateStr)
        }
        call.enqueue(object : Callback<List<ScheduleDetail>> {
            override fun onResponse(call: Call<List<ScheduleDetail>>, response: Response<List<ScheduleDetail>>) {
                if (response.isSuccessful) {
                    val schedules = response.body() ?: emptyList()
                    updateScheduleListUI(schedules)
                } else {
                    addEmptyMessage("일정을 불러오지 못했습니다.")
                }
            }
            override fun onFailure(call: Call<List<ScheduleDetail>>, t: Throwable) {
                addEmptyMessage("네트워크 오류")
            }
        })
    }

    private fun fetchMonthlyData() {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startStr = String.format("%04d-%02d-01", currentYear, currentMonth + 1)
        val endStr = String.format("%04d-%02d-%02d", currentYear, currentMonth + 1, lastDay)
        serverDayCounts.clear()
        refreshGrid()
        val call = if (isCalendarTeamMode) {
            teamService.getTeamSchedulesMonthly(teamIdLong, startStr, endStr)
        } else {
            teamService.getMySchedulesMonthly(teamIdLong, startStr, endStr)
        }
        call.enqueue(object : Callback<List<ScheduleDetail>> {
            override fun onResponse(call: Call<List<ScheduleDetail>>, response: Response<List<ScheduleDetail>>) {
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    list.forEach { schedule ->
                        if (schedule.startAt.length >= 10) {
                            val dateKey = schedule.startAt.substring(0, 10)
                            val currentCount = serverDayCounts[dateKey] ?: 0
                            serverDayCounts[dateKey] = currentCount + 1
                        }
                    }
                    refreshGrid()
                }
            }
            override fun onFailure(call: Call<List<ScheduleDetail>>, t: Throwable) {}
        })
    }

    private fun refreshGrid() {
        teamSpaceMonthGrid.removeAllViews()
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in 0 until 42) {
            val cell = layoutInflater.inflate(R.layout.item_calendar_day, teamSpaceMonthGrid, false)
            val dayNumber = cell.findViewById<TextView>(R.id.calendarDayNumber)
            val countBadge = cell.findViewById<FrameLayout>(R.id.calendarDayEventCountBadge)
            val root = cell.findViewById<View>(R.id.calendarDayRoot)

            val day = i - startDayOfWeek + 1
            if (day in 1..daysInMonth) {
                dayNumber.text = day.toString()
                dayNumber.visibility = View.VISIBLE
                val dateKey = String.format("%04d-%02d-%02d", currentYear, currentMonth + 1, day)
                val count = serverDayCounts[dateKey] ?: 0

                if (count > 0) {
                    countBadge.visibility = View.VISIBLE
                    countBadge.removeAllViews()
                    val dot = View(requireContext()).apply {
                        val size = (5 * resources.displayMetrics.density).toInt()
                        layoutParams = FrameLayout.LayoutParams(size, size).apply {
                            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            bottomMargin = (4 * resources.displayMetrics.density).toInt()
                        }
                        background = GradientDrawable().apply {
                            val colorCode = if (isCalendarTeamMode) "#3FE9C0" else "#FFD8F3"
                            setColor(Color.parseColor(colorCode))
                            shape = GradientDrawable.OVAL
                        }
                    }
                    countBadge.addView(dot)
                } else {
                    countBadge.visibility = View.GONE
                }

                val isSelected = (currentYear == selectedDay.get(Calendar.YEAR) &&
                        currentMonth == selectedDay.get(Calendar.MONTH) &&
                        day == selectedDay.get(Calendar.DAY_OF_MONTH))

                if (isSelected) {
                    root.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                    dayNumber.setTextColor(Color.WHITE)
                } else {
                    root.setBackgroundResource(0)
                    dayNumber.setTextColor(Color.BLACK)
                }

                cell.setOnClickListener {
                    selectedDay.set(currentYear, currentMonth, day)
                    refreshGrid()
                    fetchDateSchedules()
                }
            } else {
                dayNumber.visibility = View.INVISIBLE
                countBadge.visibility = View.GONE
            }

            val params = GridLayout.LayoutParams(GridLayout.spec(i / 7), GridLayout.spec(i % 7)).apply {
                width = 0
                height = (48 * resources.displayMetrics.density).toInt()
                columnSpec = GridLayout.spec(i % 7, 1f)
            }
            teamSpaceMonthGrid.addView(cell, params)
        }
    }

    private fun updateScheduleListUI(schedules: List<ScheduleDetail>) {
        teamSpaceDayTasksContainer.removeAllViews()
        if (schedules.isEmpty()) {
            addEmptyMessage("등록된 일정이 없습니다.")
            return
        }
        schedules.forEach { schedule ->
            val row = layoutInflater.inflate(R.layout.item_task_row, teamSpaceDayTasksContainer, false)
            val titleTv = row.findViewById<TextView>(R.id.taskTitle)
            val checkBtn = row.findViewById<ImageButton>(R.id.taskCheck)
            val lockBtn = row.findViewById<ImageButton>(R.id.taskLock)
            titleTv.text = schedule.title
            checkBtn.setImageResource(R.drawable.ic_schedule_unselected)
            if (!isCalendarTeamMode) {
                lockBtn.visibility = View.VISIBLE
                lockBtn.setImageResource(R.drawable.ic_personal_unlock)
            } else {
                lockBtn.visibility = View.GONE
            }
            teamSpaceDayTasksContainer.addView(row)
        }
    }

    private fun addEmptyMessage(msg: String) {
        val tv = TextView(requireContext()).apply {
            text = msg
            setTextColor(Color.GRAY)
            setPadding(0, 32, 0, 0)
            gravity = Gravity.CENTER
        }
        teamSpaceDayTasksContainer.addView(tv)
    }

    private fun updateTeamInfoUI(data: TeamDetailResponse) {
        teamSpaceTeamName.text = data.team.name
        val colorHex = data.team.colorHex ?: "#CCCCCC"
        try {
            val color = Color.parseColor(colorHex)
            teamSpaceColorCircle.background = GradientDrawable().apply {
                setColor(color)
                shape = GradientDrawable.OVAL
            }
        } catch (e: Exception) {}
        teamSpaceIntroTitle.text = "${data.team.name} 팀 소개"
        teamSpaceIntroText.text = data.team.description ?: ""
        teamSpaceMembersCount.text = data.memberCount.toString()

    }

    private fun updateMemberUI(members: List<TeamMember>) {
        teamSpaceMembersIcons.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        members.forEach { member ->
            val item = inflater.inflate(R.layout.item_team_space_member, teamSpaceMembersIcons, false)
            val card = item.findViewById<MaterialCardView>(R.id.teamMemberCard)
            val nameTv = item.findViewById<TextView>(R.id.teamMemberName)

            nameTv.text = member.nickname

            // 서버에서 준 멤버 고유 색상 적용
            try {
                card.strokeColor = Color.parseColor(member.displayColorHex)
            } catch (e: Exception) {
                card.strokeColor = Color.LTGRAY
            }

            teamSpaceMembersIcons.addView(item)
        }
    }
}