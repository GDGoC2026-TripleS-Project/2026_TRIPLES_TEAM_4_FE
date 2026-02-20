package com.project.unimate.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.TaskItem
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamScheduleCreateRequest
import com.project.unimate.network.service.TeamScheduleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTeamTaskFragment : Fragment() {

    private val startCal = Calendar.getInstance().apply { set(Calendar.MINUTE, 0) }
    private val endCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
    private var allDay = false
    private var isPrivate = true
    private var selectedTeamId: String? = null

    private fun alarmMinutesFromLabel(label: String): Int? {
        return when (label.trim()) {
            "5분 전" -> 5
            "15분 전" -> 15
            "30분 전" -> 30
            "1시간 전" -> 60
            else -> null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_team_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedTeamId = arguments?.getString("teamId") ?: selectedTeamId
        val cancelBtn = view.findViewById<TextView>(R.id.addTeamCancel)
        val saveBtn = view.findViewById<TextView>(R.id.addTeamSave)
        val toggleTeam = view.findViewById<TextView>(R.id.addTeamToggleTeam)
        val togglePersonal = view.findViewById<TextView>(R.id.addTeamTogglePersonal)
        val nameEt = view.findViewById<EditText>(R.id.addTeamScheduleName)
        val teamSpaceBtn = view.findViewById<TextView>(R.id.addTeamTeamSpaceBtn)
        val teamSpaceBtnWrap = view.findViewById<View>(R.id.addTeamTeamSpaceBtnWrap)
        val teamSpaceListScroll = view.findViewById<View>(R.id.addTeamTeamSpaceListScroll)
        val teamSpaceList = view.findViewById<LinearLayout>(R.id.addTeamTeamSpaceList)
        val alldayIcon = view.findViewById<ImageView>(R.id.addTeamAlldayIcon)
        val alldayRow = alldayIcon.parent as View
        val startDateBtn = view.findViewById<Button>(R.id.addTeamStartDate)
        val startTimeBtn = view.findViewById<Button>(R.id.addTeamStartTime)
        val endDateBtn = view.findViewById<Button>(R.id.addTeamEndDate)
        val endTimeBtn = view.findViewById<Button>(R.id.addTeamEndTime)
        val notificationBtn = view.findViewById<TextView>(R.id.addTeamNotificationBtn)
        val notificationBtnWrap = view.findViewById<View>(R.id.addTeamNotificationBtnWrap)
        val notificationDropdown = view.findViewById<LinearLayout>(R.id.addTeamNotificationDropdown)
        var notificationLabel = getString(R.string.none)

        cancelBtn.setOnClickListener { findNavController().popBackStack() }
        notificationBtnWrap.setOnClickListener {
            notificationDropdown.visibility = if (notificationDropdown.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        listOf(
            R.id.addTeamNotificationItem0 to getString(R.string.none),
            R.id.addTeamNotificationItem1 to "5분 전",
            R.id.addTeamNotificationItem2 to "15분 전",
            R.id.addTeamNotificationItem3 to "30분 전",
            R.id.addTeamNotificationItem4 to "1시간 전"
        ).forEach { (id, label) ->
            view.findViewById<TextView>(id).setOnClickListener {
                notificationLabel = label
                notificationBtn.text = label
                notificationDropdown.visibility = View.GONE
            }
        }
        saveBtn.setOnClickListener { saveTeam() }
        togglePersonal.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.addPersonalTaskFragment, null)
        }
        toggleTeam.setOnClickListener { /* already team */ }

        val teams = DummyRepository.getMyTeamSpaceTeams()
        selectedTeamId?.let { id ->
            teams.find { it.id == id }?.let { teamSpaceBtn.text = it.name }
        }

        fun updateTeamRowBackgrounds() {
            val teamIds = DummyRepository.getMyTeamSpaceTeams().map { it.id }
            for (i in 0 until teamSpaceList.childCount) {
                val row = teamSpaceList.getChildAt(i) as? TextView ?: continue
                val teamId = teamIds.getOrNull(i)
                row.setBackgroundResource(if (teamId == selectedTeamId) R.drawable.bg_option_selected else android.R.color.transparent)
            }
        }
        teamSpaceBtnWrap.setOnClickListener {
            teamSpaceListScroll.visibility = if (teamSpaceListScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (teamSpaceListScroll.visibility == View.VISIBLE && teamSpaceList.childCount == 0) {
                val list = DummyRepository.getMyTeamSpaceTeams()
                list.forEachIndexed { index, team ->
                    val row = TextView(requireContext()).apply {
                        text = team.name
                        setPadding(32, 24, 32, 24)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray09))
                        setBackgroundResource(if (team.id == selectedTeamId || (selectedTeamId == null && index == 0)) R.drawable.bg_option_selected else android.R.color.transparent)
                        setOnClickListener {
                            selectedTeamId = team.id
                            teamSpaceBtn.text = team.name
                            updateTeamRowBackgrounds()
                            teamSpaceListScroll.visibility = View.GONE
                        }
                    }
                    teamSpaceList.addView(row)
                }
                if (selectedTeamId == null && list.isNotEmpty()) selectedTeamId = list[0].id
            } else if (teamSpaceListScroll.visibility == View.VISIBLE) {
                updateTeamRowBackgrounds()
            }
        }

        fun formatDate(cal: Calendar) = "${cal.get(Calendar.YEAR)}. ${cal.get(Calendar.MONTH) + 1}. ${cal.get(Calendar.DAY_OF_MONTH)}"
        fun formatTime(cal: Calendar): String {
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val amPm = if (hour < 12) "오전" else "오후"
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            return "$amPm $h:${minute.toString().padStart(2, '0')}"
        }
        fun refreshDateTime() {
            startDateBtn.text = formatDate(startCal)
            startTimeBtn.text = formatTime(startCal)
            endDateBtn.text = formatDate(endCal)
            endTimeBtn.text = formatTime(endCal)
        }
        refreshDateTime()

        // 날짜/시간 버튼
        val dateTimeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_date_time_btn)
        val gray06Color = ContextCompat.getColor(requireContext(), R.color.gray06)
        listOf(startDateBtn, startTimeBtn, endDateBtn, endTimeBtn).forEach { btn ->
            btn.backgroundTintList = null
            btn.background = dateTimeBg
            btn.setTextColor(gray06Color)
        }

        fun updateTimeButtonsEnabled() {
            startTimeBtn.isEnabled = !allDay
            startTimeBtn.isClickable = !allDay
            endTimeBtn.isEnabled = !allDay
            endTimeBtn.isClickable = !allDay
        }
        alldayRow.setOnClickListener {
            allDay = !allDay
            alldayIcon.setImageResource(if (allDay) R.drawable.ic_allday_selected else R.drawable.ic_allday_unselected)
            if (allDay) {
                startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0)
                endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
            }
            refreshDateTime()
            updateTimeButtonsEnabled()
        }
        updateTimeButtonsEnabled()

        startDateBtn.setOnClickListener {
            val ctx = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
            val dlg = DatePickerDialog(ctx, { _, y, m, d ->
                startCal.set(y, m, d)
                if (allDay) endCal.set(y, m, d)
                if (startCal.after(endCal)) {
                    endCal.set(Calendar.YEAR, startCal.get(Calendar.YEAR))
                    endCal.set(Calendar.MONTH, startCal.get(Calendar.MONTH))
                    endCal.set(Calendar.DAY_OF_MONTH, startCal.get(Calendar.DAY_OF_MONTH))
                }
                refreshDateTime()
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH))
            styleDatePicker(dlg)
            dlg.show()
        }
        fun showTimePicker(cal: Calendar, onConfirm: () -> Unit) {
            val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
            val dlg = TimePickerDialog(
                contextWrapper,
                { _, h, m ->
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, m)
                    onConfirm()
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
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
        startTimeBtn.setOnClickListener {
            showTimePicker(startCal) {
                refreshDateTime()
                if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR) && startCal.timeInMillis > endCal.timeInMillis) {
                    endCal.timeInMillis = startCal.timeInMillis
                    refreshDateTime()
                }
            }
        }
        endDateBtn.setOnClickListener {
            val ctx = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
            val dlg = DatePickerDialog(ctx, { _, y, m, d ->
                endCal.set(y, m, d)
                if (endCal.before(startCal)) {
                    startCal.set(Calendar.YEAR, endCal.get(Calendar.YEAR))
                    startCal.set(Calendar.MONTH, endCal.get(Calendar.MONTH))
                    startCal.set(Calendar.DAY_OF_MONTH, endCal.get(Calendar.DAY_OF_MONTH))
                }
                refreshDateTime()
            }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH))
            styleDatePicker(dlg)
            dlg.show()
        }
        endTimeBtn.setOnClickListener {
            showTimePicker(endCal) {
                refreshDateTime()
                if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR) && endCal.timeInMillis < startCal.timeInMillis) {
                    startCal.timeInMillis = endCal.timeInMillis
                    refreshDateTime()
                }
            }
        }
    }

    private fun saveTeam() {
        val teamId = selectedTeamId ?: return
        val name = view?.findViewById<EditText>(R.id.addTeamScheduleName)?.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) return
        if (DummyRepository.allTeams.none { it.id == teamId }) {
            DummyRepository.addTeam(Team(teamId, teamId, "#C2C2C2", "", false, 4, 7, ""))
        }
        val date = (startCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val numericTeamId = teamId.toLongOrNull()
        if (numericTeamId == null) {
            // 더미 팀(비숫자 ID): 로컬에만 추가 (서버 미연동)
            val item = TaskItem(
                id = "t-$teamId-${System.currentTimeMillis()}",
                teamId = teamId,
                title = name,
                date = date,
                startTimeMillis = startCal.timeInMillis,
                endTimeMillis = endCal.timeInMillis,
                isChecked = false,
                creatorName = null
            )
            DummyRepository.addTask(item)
            DummyRepository.saveSchedulesTo(requireContext())
            findNavController().popBackStack()
            return
        }
        // 서버에 생성 후, 서버 전체 목록으로 완전 교체 (재설치 후에도 서버에서 복구 보장)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val service = RetrofitClient.create<TeamScheduleService>(requireContext())
                val notiLabel = view?.findViewById<TextView>(R.id.addTeamNotificationBtn)?.text?.toString()
                    ?: getString(R.string.none)
                val resp = service.create(numericTeamId, TeamScheduleCreateRequest(
                    title = name,
                    startAt = isoFmt.format(Date(startCal.timeInMillis)),
                    endAt = isoFmt.format(Date(endCal.timeInMillis)),
                    category = "MEETING",
                    alarmMinutes = alarmMinutesFromLabel(notiLabel)
                ))
                if (resp.isSuccessful) {
                    val serverId = resp.body()?.id
                    if (serverId != null) {
                        withContext(Dispatchers.Main) {
                            val notiLabel = view?.findViewById<TextView>(R.id.addTeamNotificationBtn)?.text?.toString() ?: getString(R.string.none)
                            val item = TaskItem(
                                id = "t-$teamId-$serverId",
                                teamId = teamId,
                                title = name,
                                date = date,
                                startTimeMillis = startCal.timeInMillis,
                                endTimeMillis = endCal.timeInMillis,
                                isChecked = false,
                                creatorName = null
                            )
                            DummyRepository.addTask(item)
                            DummyRepository.saveSchedulesTo(requireContext())
                            findNavController().popBackStack()
                        }
                    } else {
                        withContext(Dispatchers.Main) { findNavController().popBackStack() }
                    }
                    Log.d("AddTeamTask", "팀 일정 생성 성공: teamId=$numericTeamId")
                    try {
                        val listResp = service.getByRange(numericTeamId, "2025-01-01", "2026-12-31")
                        if (listResp.isSuccessful) {
                            val taskItems = listResp.body()?.mapNotNull { s ->
                                val sid = s.id ?: return@mapNotNull null
                                val startMs = parseIsoToMillis(s.startAt)
                                val endMs = parseIsoToMillis(s.endAt)
                                if (startMs == null || endMs == null) return@mapNotNull null
                                val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                                TaskItem(
                                    id = "t-$teamId-$sid",
                                    teamId = teamId,
                                    title = s.title ?: "",
                                    date = cal,
                                    startTimeMillis = startMs,
                                    endTimeMillis = endMs,
                                    isChecked = false,
                                    creatorName = null
                                )
                            }.orEmpty()
                            Log.d("AddTeamTask", "서버 일정 재로드: ${taskItems.size}개")
                            withContext(Dispatchers.Main) {
                                DummyRepository.replaceTasksForTeam(teamId, taskItems)
                                DummyRepository.saveSchedulesTo(requireContext())
                            }
                        } else {
                            Log.w("AddTeamTask", "서버 일정 재로드 실패: ${listResp.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("AddTeamTask", "서버 일정 재로드 예외: ${e.message}")
                    }
                } else {
                    Log.w("AddTeamTask", "팀 일정 생성 실패: ${resp.code()}")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(requireContext(), "일정 저장에 실패했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AddTeamTask", "팀 일정 생성 예외: ${e.message}")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(requireContext(), "일정 저장 실패: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun parseIsoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        val s = iso.replace("Z", "").trim()
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).parse(s)?.time
                ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(s)?.time
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s)?.time
        } catch (_: Exception) { null }
    }

    private fun styleDatePicker(dialog: DatePickerDialog) {
        dialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "확인", dialog)
        dialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "취소", dialog)
        dialog.setOnShowListener {
            val colorBlack = ContextCompat.getColor(requireContext(), android.R.color.black)
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setTextColor(colorBlack)
            dialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(colorBlack)
        }
    }

    private fun applyBlackText(view: View) {
        if (view is ViewGroup) for (i in 0 until view.childCount) applyBlackText(view.getChildAt(i))
        if (view is TextView) view.setTextColor(Color.BLACK)
    }
}
