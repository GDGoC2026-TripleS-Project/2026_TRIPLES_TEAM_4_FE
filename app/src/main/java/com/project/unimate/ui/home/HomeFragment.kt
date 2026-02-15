package com.project.unimate.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.model.HomeResponse
import com.project.unimate.network.HomeService
import com.project.unimate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var weekAnchor: Calendar = Calendar.getInstance()
    private var selectedDay: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    private var isChecklistExpanded = false
    private val maxCollapsedPersonalItems = 3

    private lateinit var homeService: HomeService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        homeService = RetrofitClient.getInstance(requireContext()).create(HomeService::class.java)

        val homePrevWeek = root.findViewById<ImageButton>(R.id.homePrevWeek)
        val homeNextWeek = root.findViewById<ImageButton>(R.id.homeNextWeek)
        val homeExpandArrow = root.findViewById<ImageButton>(R.id.homeExpandArrow)

        root.findViewById<View>(R.id.headerNotification)?.setOnClickListener {
            findNavController().navigate(R.id.notificationFragment)
        }

        homeExpandArrow.setOnClickListener {
            isChecklistExpanded = !isChecklistExpanded
            fetchHomeData(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDay.time))
        }

        homePrevWeek.setOnClickListener {
            weekAnchor.add(Calendar.WEEK_OF_YEAR, -1)
            selectedDay.add(Calendar.WEEK_OF_YEAR, -1)
            fetchHomeData(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDay.time))
        }

        homeNextWeek.setOnClickListener {
            weekAnchor.add(Calendar.WEEK_OF_YEAR, 1)
            selectedDay.add(Calendar.WEEK_OF_YEAR, 1)
            fetchHomeData(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDay.time))
        }

        fetchHomeData(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDay.time))

        return root
    }

    private fun updateUI(data: HomeResponse) {
        val view = view ?: return

        val homeMonthYear = view.findViewById<TextView>(R.id.homeMonthYear)
        val homeWeekDatesContainer = view.findViewById<LinearLayout>(R.id.homeWeekDatesContainer)
        val homeTodayTasksContainer = view.findViewById<LinearLayout>(R.id.homeTodayTasksContainer)
        val homePersonalTasksContainer = view.findViewById<LinearLayout>(R.id.homePersonalTasksContainer)
        val homePersonalLabel = view.findViewById<TextView>(R.id.homePersonalLabel)
        val homeExpandArrow = view.findViewById<ImageButton>(R.id.homeExpandArrow)
        val homeTeamSpaceIcons = view.findViewById<LinearLayout>(R.id.homeTeamSpaceIcons)

        homeMonthYear.text = getString(R.string.date_format_year_month, weekAnchor.get(Calendar.YEAR), weekAnchor.get(Calendar.MONTH) + 1)

        homeWeekDatesContainer.removeAllViews()
        data.weeklyCalendar.forEach { dayItem ->
            val column = layoutInflater.inflate(R.layout.item_home_week_column, homeWeekDatesContainer, false) as LinearLayout
            val dateTv = column.findViewById<TextView>(R.id.weekDateNumber)
            val countBadge = column.findViewById<View>(R.id.weekEventCountBadge)
            val countTv = column.findViewById<TextView>(R.id.weekEventCount)

            dateTv.text = dayItem.date.split("-").last()
            val isSelected = dayItem.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDay.time)
            column.setBackgroundResource(if (isSelected) R.drawable.bg_home_day_selected else 0)

            column.setOnClickListener {
                selectedDay.time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dayItem.date)!!
                fetchHomeData(dayItem.date)
            }

            if (dayItem.scheduleCount > 0) {
                countBadge.visibility = View.VISIBLE
                countTv.text = dayItem.scheduleCount.toString()
            } else {
                countBadge.visibility = View.GONE
            }
            homeWeekDatesContainer.addView(column)
        }

        homeTodayTasksContainer.removeAllViews()
        val teamSchedules = data.todaySchedules.teamSchedules
        val personalSchedules = data.todaySchedules.personalSchedules

        if (teamSchedules.isEmpty() && personalSchedules.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = getString(R.string.no_schedule)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_medium))
                textSize = 14f
                setPadding(0, 24 * resources.displayMetrics.density.toInt(), 0, 0)
            }
            homeTodayTasksContainer.addView(emptyTv)
            homePersonalLabel.visibility = View.GONE
            homePersonalTasksContainer.removeAllViews()
            homeExpandArrow.visibility = View.GONE
        } else {
            teamSchedules.forEach { teamGroup ->
                val teamHeader = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 12 * resources.displayMetrics.density.toInt(), 0, 4 * resources.displayMetrics.density.toInt())
                }
                val circle = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams((15 * resources.displayMetrics.density).toInt(), (15 * resources.displayMetrics.density).toInt()).apply { marginEnd = 6 * resources.displayMetrics.density.toInt() }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(teamGroup.teamColor ?: "#CCCCCC"))
                    }
                }
                teamHeader.addView(circle)
                teamHeader.addView(TextView(requireContext()).apply { text = teamGroup.teamName; setTextColor(Color.BLACK); textSize = 14f })
                homeTodayTasksContainer.addView(teamHeader)

                teamGroup.schedules.forEach { task ->
                    val row = layoutInflater.inflate(R.layout.item_task_row, homeTodayTasksContainer, false)
                    row.findViewById<TextView>(R.id.taskTitle).text = task.title
                    row.findViewById<ImageButton>(R.id.taskLock).visibility = View.GONE
                    homeTodayTasksContainer.addView(row)
                }
            }

            homePersonalLabel.visibility = if (personalSchedules.isEmpty()) View.GONE else View.VISIBLE
            homePersonalTasksContainer.removeAllViews()
            homeExpandArrow.visibility = if (personalSchedules.isNotEmpty()) View.VISIBLE else View.GONE
            homeExpandArrow.setImageResource(if (isChecklistExpanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down)

            val toShow = if (isChecklistExpanded) personalSchedules else personalSchedules.take(maxCollapsedPersonalItems)
            toShow.forEach { item ->
                val row = layoutInflater.inflate(R.layout.item_task_row, homePersonalTasksContainer, false)
                row.findViewById<TextView>(R.id.taskTitle).text = item.title
                val lock = row.findViewById<ImageButton>(R.id.taskLock)
                lock.visibility = View.VISIBLE
                lock.setImageResource(if (item.private) R.drawable.ic_personal_lock else R.drawable.ic_personal_unlock)
                homePersonalTasksContainer.addView(row)
            }
        }

        homeTeamSpaceIcons.removeAllViews()
        data.myTeamSpaces.forEach { team ->
            val item = layoutInflater.inflate(R.layout.item_home_team_icon, homeTeamSpaceIcons, false)
            item.setOnClickListener {
                findNavController().navigate(R.id.teamSpaceFragment, Bundle().apply { putString("teamId", team.teamId.toString()) })
            }
            val iconLetter = item.findViewById<TextView>(R.id.teamIconLetter)
            item.findViewById<ImageView>(R.id.teamIconImage).visibility = View.GONE
            iconLetter.visibility = View.VISIBLE
            iconLetter.text = team.teamName.firstOrNull()?.toString() ?: ""
            iconLetter.setBackgroundColor(Color.parseColor(team.teamColor ?: "#CCCCCC"))
            item.findViewById<TextView>(R.id.teamIconName).text = team.teamName
            homeTeamSpaceIcons.addView(item)
        }

        val plusBtn = layoutInflater.inflate(R.layout.item_home_team_plus, homeTeamSpaceIcons, false)
        plusBtn.findViewById<ImageButton>(R.id.teamPlusButton).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_teamAdd)
        }
        homeTeamSpaceIcons.addView(plusBtn)
    }

    private fun fetchHomeData(dateString: String) {
        homeService.getHomeSummary(dateString, true).enqueue(object : Callback<HomeResponse> {
            override fun onResponse(call: Call<HomeResponse>, response: Response<HomeResponse>) {
                if (response.isSuccessful) response.body()?.let { updateUI(it) }
                else if (response.code() == 403) Toast.makeText(context, "로그인 만료", Toast.LENGTH_SHORT).show()
            }
            override fun onFailure(call: Call<HomeResponse>, t: Throwable) { Log.e("HomeFragment", "Error: ${t.message}") }
        })
    }
}