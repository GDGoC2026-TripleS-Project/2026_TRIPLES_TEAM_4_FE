package com.project.unimate.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
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
import com.project.unimate.network.dto.TeamScheduleUpdateRequest
import com.project.unimate.network.service.TeamScheduleService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditTeamTaskFragment : Fragment() {

    private var taskId: String? = null
    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()
    private var allDay = false
    private var isPrivate = true
    private var selectedTeamId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getString("taskId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_team_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val task = taskId?.let { DummyRepository.getTaskById(it) } ?: run {
            findNavController().popBackStack()
            return
        }
        selectedTeamId = task.teamId

        view.findViewById<TextView>(R.id.addTeamTitle).text = getString(R.string.schedule_edit)
        view.findViewById<TextView>(R.id.addTeamCancel).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<TextView>(R.id.addTeamSave).setOnClickListener { saveTeam(task) }
        // 일정 편집 페이지: 팀/개인 토글 선택 불가
        view.findViewById<TextView>(R.id.addTeamToggleTeam).apply {
            isClickable = false
            isEnabled = false
            alpha = 0.8f
        }
        view.findViewById<TextView>(R.id.addTeamTogglePersonal).apply {
            isClickable = false
            isEnabled = false
            alpha = 0.8f
        }

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

        nameEt.setText(task.title)
        teamSpaceBtn.text = DummyRepository.getTeamById(task.teamId)?.name ?: task.teamId
        val taskTeam = DummyRepository.getTeamById(task.teamId)
        val isCompletedTeam = taskTeam?.isCompleted == true
        if (isCompletedTeam) {
            teamSpaceBtnWrap.isEnabled = false
            teamSpaceBtnWrap.isClickable = false
            teamSpaceBtnWrap.alpha = 0.6f
            alldayRow.isEnabled = false
            alldayRow.isClickable = false
            alldayRow.alpha = 0.6f
            startDateBtn.isEnabled = false
            startDateBtn.isClickable = false
            startTimeBtn.isEnabled = false
            startTimeBtn.isClickable = false
            endDateBtn.isEnabled = false
            endDateBtn.isClickable = false
            endTimeBtn.isEnabled = false
            endTimeBtn.isClickable = false
            listOf(startDateBtn, startTimeBtn, endDateBtn, endTimeBtn).forEach { it.alpha = 0.6f }
        }
        startCal.timeInMillis = task.startTimeMillis
        endCal.timeInMillis = task.endTimeMillis
        allDay = startCal.get(Calendar.HOUR_OF_DAY) == 0 && startCal.get(Calendar.MINUTE) == 0 &&
            endCal.get(Calendar.HOUR_OF_DAY) == 23 && endCal.get(Calendar.MINUTE) == 59
        alldayIcon.setImageResource(if (allDay) R.drawable.ic_allday_selected else R.drawable.ic_allday_unselected)

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
                notificationBtn.text = label
                notificationDropdown.visibility = View.GONE
            }
        }
        val teams = DummyRepository.getMyTeamSpaceTeams()
        fun updateTeamRowBackgrounds() {
            val teamIds = teams.map { it.id }
            for (i in 0 until teamSpaceList.childCount) {
                val row = teamSpaceList.getChildAt(i) as? TextView ?: continue
                val teamId = teamIds.getOrNull(i)
                row.setBackgroundResource(if (teamId == selectedTeamId) R.drawable.bg_option_selected else android.R.color.transparent)
            }
        }
        teamSpaceBtnWrap.setOnClickListener {
            teamSpaceListScroll.visibility = if (teamSpaceListScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (teamSpaceListScroll.visibility == View.VISIBLE && teamSpaceList.childCount == 0) {
                teams.forEachIndexed { index, team ->
                    val row = TextView(requireContext()).apply {
                        text = team.name
                        setPadding(32, 24, 32, 24)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray09))
                        setBackgroundResource(if (team.id == selectedTeamId) R.drawable.bg_option_selected else if (index == 0 && selectedTeamId == null) R.drawable.bg_option_selected else android.R.color.transparent)
                        setOnClickListener {
                            selectedTeamId = team.id
                            teamSpaceBtn.text = team.name
                            updateTeamRowBackgrounds()
                            teamSpaceListScroll.visibility = View.GONE
                        }
                    }
                    teamSpaceList.addView(row)
                }
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
            if (isCompletedTeam) return
            startTimeBtn.isEnabled = !allDay
            startTimeBtn.isClickable = !allDay
            endTimeBtn.isEnabled = !allDay
            endTimeBtn.isClickable = !allDay
        }
        alldayRow.setOnClickListener {
            if (isCompletedTeam) return@setOnClickListener
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
            if (isCompletedTeam) return@setOnClickListener
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
            if (!isCompletedTeam) showTimePicker(startCal) {
                refreshDateTime()
                if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR) && startCal.timeInMillis > endCal.timeInMillis) {
                    endCal.timeInMillis = startCal.timeInMillis
                    refreshDateTime()
                }
            }
        }
        endDateBtn.setOnClickListener {
            if (isCompletedTeam) return@setOnClickListener
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
            if (isCompletedTeam) return@setOnClickListener
            showTimePicker(endCal) {
                refreshDateTime()
                if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR) && endCal.timeInMillis < startCal.timeInMillis) {
                    startCal.timeInMillis = endCal.timeInMillis
                    refreshDateTime()
                }
            }
        }
    }

    private fun saveTeam(original: TaskItem) {
        val teamId = selectedTeamId ?: return
        val name = view?.findViewById<EditText>(R.id.addTeamScheduleName)?.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) return
        if (DummyRepository.allTeams.none { it.id == teamId }) {
            DummyRepository.addTeam(Team(teamId, teamId, "#C2C2C2", "", false, 4, 7, ""))
        }
        val date = (startCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val updated = original.copy(
            teamId = teamId,
            title = name,
            date = date,
            startTimeMillis = startCal.timeInMillis,
            endTimeMillis = endCal.timeInMillis
        )
        DummyRepository.updateTask(updated)
        DummyRepository.saveSchedulesTo(requireContext())

        // API 호출 (팀 일정 수정) - id 형식: t-{teamId}-{scheduleId}
        val numericTeamId = teamId.toLongOrNull()
        val scheduleId = original.id.split("-").getOrNull(2)?.toLongOrNull()
        if (numericTeamId != null && scheduleId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val service = RetrofitClient.create<TeamScheduleService>(requireContext())
                    service.update(numericTeamId, scheduleId, TeamScheduleUpdateRequest(
                        title = name,
                        startAt = isoFmt.format(Date(startCal.timeInMillis)),
                        endAt = isoFmt.format(Date(endCal.timeInMillis))
                    ))
                } catch (_: Exception) { }
            }
        }

        findNavController().popBackStack()
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
