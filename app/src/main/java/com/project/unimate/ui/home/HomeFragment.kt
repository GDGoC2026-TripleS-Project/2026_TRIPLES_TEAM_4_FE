package com.project.unimate.ui.home

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import java.io.File
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.HomeSummaryResponse
import com.project.unimate.network.dto.TeamSummaryResponse
import com.project.unimate.data.repository.ProfileImageStore
import com.project.unimate.network.service.HomeService
import com.project.unimate.utils.ProfileImageLoader
import com.project.unimate.network.service.TeamScheduleService
import com.project.unimate.network.service.TeamService
import com.project.unimate.network.service.UserService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var weekAnchor: Calendar = Calendar.getInstance()
    private var selectedDay: Calendar = Calendar.getInstance().apply { timeInMillis = weekAnchor.timeInMillis }
    private var isChecklistExpanded = false
    private val maxCollapsedPersonalItems = 3
    private var homeSummary: HomeSummaryResponse? = null

    /** 서버 동기화 완료 후 주간 캘린더·오늘 할일을 갱신하는 콜백. onCreateView에서 설정. */
    private var onSyncComplete: (() -> Unit)? = null


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

        root.findViewById<View>(R.id.headerNotification)?.setOnClickListener {
            findNavController().navigate(R.id.notificationFragment)
        }

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
                val teamHeaderRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(0, 12.dpToPx(), 0, 4.dpToPx())
                }
                val circle = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(15.dpToPx(), 15.dpToPx()).apply {
                        marginEnd = 6.dpToPx()
                    }
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setShape(android.graphics.drawable.GradientDrawable.OVAL)
                        setColor(Color.parseColor(team.colorHex))
                    }
                }
                val teamNameTv = TextView(requireContext()).apply {
                    text = team.name
                    setTextColor(Color.BLACK)
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                }
                teamHeaderRow.addView(circle)
                teamHeaderRow.addView(teamNameTv)
                homeTodayTasksContainer.addView(teamHeaderRow)
                tasks.forEach { task ->
                    val row = inflater.inflate(R.layout.item_task_row, homeTodayTasksContainer, false)
                    val checkBtn = row.findViewById<ImageButton>(R.id.taskCheck)
                    val titleTv = row.findViewById<TextView>(R.id.taskTitle)
                    checkBtn.setImageResource(if (task.isChecked) R.drawable.ic_schedule_selected else R.drawable.ic_schedule_unselected)
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
                        DummyRepository.saveSchedulesTo(requireContext())
                        refreshTodayTasks()
                    }
                    titleTv.setOnClickListener {
                        findNavController().navigate(R.id.editTeamTaskFragment, Bundle().apply { putString("taskId", task.id) })
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
                    checkBtn.setImageResource(if (item.isChecked) R.drawable.ic_schedule_selected else R.drawable.ic_schedule_unselected)
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
                        DummyRepository.saveSchedulesTo(requireContext())
                        refreshTodayTasks()
                    }
                    checkBtn.setOnClickListener {
                        DummyRepository.setPersonalChecked(item.id, !item.isChecked)
                        DummyRepository.saveSchedulesTo(requireContext())
                        refreshTodayTasks()
                    }
                    titleTv.setOnClickListener {
                        findNavController().navigate(R.id.editPersonalTaskFragment, Bundle().apply { putString("personalId", item.id) })
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
                homeCardContentWrapper.setMinimumHeight(0)
                homeCardContentWrapper.layoutParams = lp
                homeCardContentWrapper.post {
                    val contentH = homeCardContentWrapper.getChildAt(0)?.height ?: 0
                    if (contentH in 1 until collapsedContentH) {
                        val lp2 = homeCardContentWrapper.layoutParams as? LinearLayout.LayoutParams ?: return@post
                        lp2.height = collapsedContentH.coerceAtLeast(0)
                        homeCardContentWrapper.layoutParams = lp2
                    }
                }
            } else {
                homeCardContentWrapper.setMinimumHeight(0)
                lp.height = collapsedContentH.coerceAtLeast(0)
                homeCardContentWrapper.layoutParams = lp
            }
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

                val layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f                                      // 비율을 1로 설정 (7개 칸이 똑같이 나눠 가짐)
                )
                column.layoutParams = layoutParams

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

        // onSyncComplete: 서버 동기화 완료 후 주간 뷰와 오늘 할일 갱신
        onSyncComplete = {
            refreshWeek()
            refreshTodayTasks()
        }

        refreshTeamIcons(root)
        loadHomeSummary()
        syncTeamsFromServerAndRefresh(root)

        return root
    }

    override fun onResume() {
        super.onResume()
        view?.let { root ->
            DummyRepository.applyPersistedTeamImages(requireContext())
            DummyRepository.applyPersistedTeamNames(requireContext())
            refreshTeamIcons(root)
            syncTeamsFromServerAndRefresh(root)
            syncMyProfileFromServer()
        }
    }

    private fun syncMyProfileFromServer() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<UserService>(ctx)
                val resp = service.getMyInfo()
                if (resp.isSuccessful) {
                    val url = resp.body()?.profileImageUrl?.takeIf { it.isNotBlank() } ?: return@launch
                    if (DummyRepository.getCurrentUserProfileImageResName() != url) {
                        DummyRepository.setCurrentUserProfileImageResName(url)
                        ProfileImageStore.save(ctx, url)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun syncTeamsFromServerAndRefresh(root: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1) 팀 목록 로드
                val teamService = RetrofitClient.create<TeamService>(requireContext())
                val resp = teamService.getMyTeams()
                if (!resp.isSuccessful) {
                    android.util.Log.w("HomeFragment", "서버 로드 실패: ${resp.code()} ${resp.message()}")
                    return@launch
                }
                val serverTeams = resp.body()?.listOrEmpty()?.mapNotNull { teamSummaryToTeam(it) } ?: emptyList()
                android.util.Log.d("HomeFragment", "서버 팀 로드: ${serverTeams.size}개")
                val merged = DummyRepository.mergeServerTeamsWithSeed(serverTeams)
                withContext(Dispatchers.Main) {
                    DummyRepository.replaceTeamsWithServerData(merged)
                    refreshTeamIcons(root)
                }

                // 2) 서버 팀(numeric ID) 별 일정 로드 → replaceTasksForTeam으로 완전 교체
                val scheduleService = RetrofitClient.create<TeamScheduleService>(requireContext())
                var totalLoaded = 0
                for (team in serverTeams) {
                    val numericId = team.id.toLongOrNull() ?: continue
                    try {
                        val listResp = scheduleService.getByRange(numericId, "2025-01-01", "2026-12-31")
                        if (listResp.isSuccessful) {
                            val taskItems = listResp.body()?.mapNotNull { s ->
                                val sid = s.id ?: return@mapNotNull null
                                val startMs = parseIsoToMillis(s.startAt)
                                val endMs = parseIsoToMillis(s.endAt)
                                if (startMs == null || endMs == null) return@mapNotNull null
                                val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                                TaskItem(
                                    id = "t-${team.id}-$sid",
                                    teamId = team.id,
                                    title = s.title ?: "",
                                    date = cal,
                                    startTimeMillis = startMs,
                                    endTimeMillis = endMs,
                                    isChecked = false,
                                    creatorName = null
                                )
                            }.orEmpty()
                            totalLoaded += taskItems.size
                            withContext(Dispatchers.Main) {
                                DummyRepository.replaceTasksForTeam(team.id, taskItems)
                            }
                        } else {
                            android.util.Log.w("HomeFragment", "팀 ${team.id} 일정 로드 실패: ${listResp.code()}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("HomeFragment", "팀 ${team.id} 일정 로드 예외: ${e.message}")
                    }
                }
                android.util.Log.d("HomeFragment", "서버 일정 로드: ${totalLoaded}개")

                // 3) 저장 + UI 갱신
                withContext(Dispatchers.Main) {
                    DummyRepository.saveSchedulesTo(requireContext())
                    onSyncComplete?.invoke()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "서버 로드 실패: ${e.message}")
            }
        }
    }

    private fun refreshTeamIcons(root: View) {
        val homeTeamSpaceIcons = root.findViewById<LinearLayout>(R.id.homeTeamSpaceIcons) ?: return
        val inflater = layoutInflater
        homeTeamSpaceIcons.removeAllViews()
        DummyRepository.getMyTeamSpaceTeams().forEach { team ->
            val item = inflater.inflate(R.layout.item_home_team_icon, homeTeamSpaceIcons, false)
            item.isClickable = true
            item.isFocusable = true
            item.setOnClickListener {
                findNavController().navigate(R.id.teamSpaceFragment, Bundle().apply { putString("teamId", team.id) })
            }
            val card = item.findViewById<com.google.android.material.card.MaterialCardView>(R.id.teamIconCard)
            card.strokeColor = Color.parseColor(team.colorHex)
            val iconImage = item.findViewById<ImageView>(R.id.teamIconImage)
            val iconLetter = item.findViewById<TextView>(R.id.teamIconLetter)
            when {
                team.imageResName.startsWith("file:") -> {
                    val file = File(requireContext().filesDir, team.imageResName.removePrefix("file:"))
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)?.let { bmp ->
                            iconImage.visibility = View.VISIBLE
                            iconImage.setImageBitmap(bmp)
                            iconLetter.visibility = View.GONE
                        } ?: run {
                            iconImage.visibility = View.GONE
                            iconLetter.visibility = View.VISIBLE
                            iconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                            iconLetter.setBackgroundColor(Color.parseColor(team.colorHex))
                        }
                    } else {
                        iconImage.visibility = View.GONE
                        iconLetter.visibility = View.VISIBLE
                        iconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                        iconLetter.setBackgroundColor(Color.parseColor(team.colorHex))
                    }
                }
                team.imageResName.startsWith("http://") || team.imageResName.startsWith("https://") -> {
                    iconImage.visibility = View.VISIBLE
                    ProfileImageLoader.load(iconImage, team.imageResName, requireContext())
                    iconLetter.visibility = View.GONE
                }
                team.imageResName.isNotBlank() -> {
                    val resId = resources.getIdentifier(team.imageResName, "drawable", requireContext().packageName)
                    if (resId != 0) {
                        iconImage.visibility = View.VISIBLE
                        iconImage.setImageResource(resId)
                        iconLetter.visibility = View.GONE
                    } else {
                        iconImage.visibility = View.GONE
                        iconLetter.visibility = View.VISIBLE
                        iconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                        iconLetter.setBackgroundColor(Color.parseColor(team.colorHex))
                    }
                }
                else -> {
                    iconImage.visibility = View.GONE
                    iconLetter.visibility = View.VISIBLE
                    iconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                    iconLetter.setBackgroundColor(Color.parseColor(team.colorHex))
                }
            }
            item.findViewById<TextView>(R.id.teamIconName).text = team.name
            homeTeamSpaceIcons.addView(item)
        }
        val plusBtn = inflater.inflate(R.layout.item_home_team_plus, homeTeamSpaceIcons, false)
        plusBtn.findViewById<ImageButton>(R.id.teamPlusButton).setOnClickListener { findNavController().navigate(R.id.action_home_to_teamAdd) }
        homeTeamSpaceIcons.addView(plusBtn)
        (plusBtn.layoutParams as? LinearLayout.LayoutParams)?.gravity = android.view.Gravity.CENTER_VERTICAL
    }

    private fun teamSummaryToTeam(r: TeamSummaryResponse): Team? {
        val id = r.id ?: return null
        val completed = r.completed == true || r.isCompleted == true
        val endMillis = parseIsoToMillis(r.endAt)
        return Team(
            id = id.toString(),
            name = r.name ?: "",
            colorHex = r.colorHex ?: "#cccccc",
            imageResName = "",
            isCompleted = completed,
            memberCount = (r.memberCount ?: 0).toInt(),
            deadlineDays = null,
            intro = r.description ?: "",
            workStartMillis = parseIsoToMillis(r.startAt),
            workEndMillis = endMillis,
            completedAtMillis = if (completed) endMillis else null
        )
    }

    private fun parseIsoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(iso)?.time
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso)?.time
        } catch (_: Exception) { null }
    }

    private fun loadHomeSummary() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<HomeService>(ctx)
                val response = service.getHomeSummary()
                if (response.isSuccessful) {
                    homeSummary = response.body()
                }
            } catch (_: Exception) {
                // API 실패 시 더미 데이터 유지
            }
        }
    }

}
