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
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.MyScheduleCreateRequest
import com.project.unimate.network.service.MyScheduleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddPersonalTaskFragment : Fragment() {

    private val startCal = Calendar.getInstance().apply { set(Calendar.MINUTE, 0) }
    private val endCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1); set(Calendar.MINUTE, 0) }
    private var allDay = false
    private var notificationOn = false
    private var isPrivate = true
    private var category: String? = null
    private var categoryEtText: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_personal_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cancelBtn = view.findViewById<TextView>(R.id.addPersonalCancel)
        val titleTv = view.findViewById<TextView>(R.id.addPersonalTitle)
        val saveBtn = view.findViewById<TextView>(R.id.addPersonalSave)
        val toggleTeam = view.findViewById<TextView>(R.id.addPersonalToggleTeam)
        val togglePersonal = view.findViewById<TextView>(R.id.addPersonalTogglePersonal)
        val nameEt = view.findViewById<EditText>(R.id.addPersonalScheduleName)
        val alldayIcon = view.findViewById<ImageView>(R.id.addPersonalAlldayIcon)
        val alldayRow = alldayIcon.parent as View
        val startDateBtn = view.findViewById<Button>(R.id.addPersonalStartDate)
        val startTimeBtn = view.findViewById<Button>(R.id.addPersonalStartTime)
        val endDateBtn = view.findViewById<Button>(R.id.addPersonalEndDate)
        val endTimeBtn = view.findViewById<Button>(R.id.addPersonalEndTime)
        val notificationBtn = view.findViewById<TextView>(R.id.addPersonalNotificationBtn)
        val notificationBtnWrap = view.findViewById<View>(R.id.addPersonalNotificationBtnWrap)
        val notificationDropdown = view.findViewById<LinearLayout>(R.id.addPersonalNotificationDropdown)
        val categoryBtn = view.findViewById<TextView>(R.id.addPersonalCategoryBtn)
        val categoryBtnWrap = view.findViewById<View>(R.id.addPersonalCategoryBtnWrap)
        val categoryEt = view.findViewById<EditText>(R.id.addPersonalCategoryEt)
        val categoryListScroll = view.findViewById<ScrollView>(R.id.addPersonalCategoryListScroll)
        val categoryList = view.findViewById<LinearLayout>(R.id.addPersonalCategoryList)
        val publicBtn = view.findViewById<TextView>(R.id.addPersonalPublicBtn)
        val privateBtn = view.findViewById<TextView>(R.id.addPersonalPrivateBtn)

        cancelBtn.setOnClickListener { findNavController().popBackStack() }
        saveBtn.setOnClickListener { savePersonal() }
        toggleTeam.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.addTeamTaskFragment, null)
        }
        togglePersonal.setOnClickListener {

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
        val gray06 = ContextCompat.getColor(requireContext(), R.color.gray06)
        listOf(startDateBtn, startTimeBtn, endDateBtn, endTimeBtn).forEach { btn ->
            btn.backgroundTintList = null
            btn.background = dateTimeBg
            btn.setTextColor(gray06)
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
        startTimeBtn.setOnClickListener { showTimePicker(startCal) { refreshDateTime() } }
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

        notificationBtnWrap.setOnClickListener {
            notificationDropdown.visibility = if (notificationDropdown.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        listOf(
            R.id.addPersonalNotificationItem0 to getString(R.string.none),
            R.id.addPersonalNotificationItem1 to "5분 전",
            R.id.addPersonalNotificationItem2 to "15분 전",
            R.id.addPersonalNotificationItem3 to "30분 전",
            R.id.addPersonalNotificationItem4 to "1시간 전"
        ).forEach { (id, label) ->
            view.findViewById<TextView>(id).setOnClickListener {
                notificationBtn.text = label
                notificationDropdown.visibility = View.GONE
            }
        }

        val categories = listOf(
            getString(R.string.category_part_time),
            getString(R.string.category_other_team),
            getString(R.string.category_meeting),
            getString(R.string.category_exam),
            getString(R.string.category_health),
            getString(R.string.category_etc)
        )
        fun updateCategoryRowBackgrounds(selectedLabel: String?) {
            for (i in 0 until categoryList.childCount) {
                val row = categoryList.getChildAt(i) as? TextView ?: continue
                val isSelected = (row.text.toString() == selectedLabel) || (selectedLabel == null && i == 0)
                row.setBackgroundResource(if (isSelected) R.drawable.bg_option_selected else android.R.color.transparent)
            }
        }
        categoryBtnWrap.setOnClickListener {
            if (categoryEt.visibility == View.VISIBLE) {
                categoryEt.visibility = View.GONE
                categoryBtn.visibility = View.VISIBLE
                categoryBtn.text = categoryEt.text.toString().trim().takeIf { it.isNotEmpty() } ?: getString(R.string.category_etc)
                categoryListScroll.visibility = View.GONE
                return@setOnClickListener
            }
            categoryListScroll.visibility = if (categoryListScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (categoryListScroll.visibility == View.VISIBLE && categoryList.childCount == 0) {
                categories.forEachIndexed { index, label ->
                    val row = TextView(requireContext()).apply {
                        text = label
                        setPadding(32, 24, 32, 24)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray09))
                        setBackgroundResource(if (label == category || (category == null && index == 0)) R.drawable.bg_option_selected else android.R.color.transparent)
                        setOnClickListener {
                            if (label == getString(R.string.category_etc)) {
                                categoryBtn.visibility = View.GONE
                                categoryEt.visibility = View.VISIBLE
                                categoryEt.requestFocus()
                                category = label
                                categoryListScroll.visibility = View.GONE
                            } else {
                                category = label
                                categoryBtn.text = label
                                categoryBtn.visibility = View.VISIBLE
                                categoryEt.visibility = View.GONE
                                categoryListScroll.visibility = View.GONE
                            }
                            updateCategoryRowBackgrounds(category)
                        }
                    }
                    categoryList.addView(row)
                }
            } else if (categoryListScroll.visibility == View.VISIBLE) {
                updateCategoryRowBackgrounds(category ?: categories.firstOrNull())
            }
        }
        categoryEt.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                categoryEtText = categoryEt.text.toString().trim()
                categoryBtn.text = if (categoryEtText?.isNotEmpty() == true) categoryEtText else getString(R.string.category_etc)
                categoryBtn.visibility = View.VISIBLE
                categoryEt.visibility = View.GONE
                categoryListScroll.visibility = View.GONE
            }
        }

        publicBtn.setOnClickListener { isPrivate = false; updateVisibilityButtons(publicBtn, privateBtn) }
        privateBtn.setOnClickListener { isPrivate = true; updateVisibilityButtons(publicBtn, privateBtn) }
        updateVisibilityButtons(publicBtn, privateBtn)
    }

    private fun updateVisibilityButtons(publicBtn: TextView, privateBtn: TextView) {
        publicBtn.setBackgroundResource(if (isPrivate) R.drawable.bg_schedule_toggle_unselected else R.drawable.bg_schedule_visibility_selected)
        publicBtn.setTextColor(ContextCompat.getColor(requireContext(), if (isPrivate) R.color.gray07 else R.color.white))
        privateBtn.setBackgroundResource(if (isPrivate) R.drawable.bg_schedule_visibility_selected else R.drawable.bg_schedule_toggle_unselected)
        privateBtn.setTextColor(ContextCompat.getColor(requireContext(), if (isPrivate) R.color.white else R.color.gray07))
    }

    private fun savePersonal() {
        val name = view?.findViewById<EditText>(R.id.addPersonalScheduleName)?.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) return
        val catEt = view?.findViewById<EditText>(R.id.addPersonalCategoryEt)
        val cat = if (catEt?.visibility == View.VISIBLE) (catEt.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: getString(R.string.category_etc)) else (category ?: "없음")
        val date = (startCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val notiLabel = view?.findViewById<TextView>(R.id.addPersonalNotificationBtn)?.text?.toString() ?: getString(R.string.none)
        val teams = DummyRepository.getMyTeamSpaceTeams()
        val firstTeamId = teams.firstOrNull()?.id?.toLongOrNull()
        if (firstTeamId == null) {
            val item = PersonalScheduleItem(
                id = "p-${System.currentTimeMillis()}",
                title = name,
                date = date,
                startTimeMillis = startCal.timeInMillis,
                endTimeMillis = endCal.timeInMillis,
                isLocked = isPrivate,
                isChecked = false,
                notificationCategory = notiLabel,
                scheduleCategory = cat
            )
            DummyRepository.addPersonalSchedule(item)
            DummyRepository.saveSchedulesTo(requireContext())
            findNavController().popBackStack()
            return
        }
        // 서버에 생성 후, 서버 전체 개인일정으로 완전 교체 (재설치 후에도 서버에서 복구 보장)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val service = RetrofitClient.create<MyScheduleService>(requireContext())
                val resp = service.create(firstTeamId, MyScheduleCreateRequest(
                    title = name,
                    startAt = isoFmt.format(Date(startCal.timeInMillis)),
                    endAt = isoFmt.format(Date(endCal.timeInMillis)),
                    isPrivate = isPrivate,
                    category = if (cat == "없음") "OTHER" else cat
                ))
                if (resp.isSuccessful) {
                    Log.d("AddPersonalTask", "개인 일정 생성 성공: teamId=$firstTeamId")
                    // 서버에서 개인 일정 전체 재로드 → 로컬 완전 교체
                    try {
                        val markedResp = service.getMarkedDates(firstTeamId, "2025-01-01", "2026-12-31")
                        if (markedResp.isSuccessful) {
                            val dates = markedResp.body()?.markedDates ?: emptyList()
                            val personalItems = mutableListOf<PersonalScheduleItem>()
                            for (dateStr in dates) {
                                val dayResp = service.getDaySchedules(firstTeamId, dateStr)
                                if (dayResp.isSuccessful) {
                                    dayResp.body()?.forEach { s ->
                                        val sid = s.id ?: return@forEach
                                        val startMs = parseIsoToMillis(s.startAt)
                                        val endMs = parseIsoToMillis(s.endAt)
                                        if (startMs == null || endMs == null) return@forEach
                                        val cal = Calendar.getInstance().apply { timeInMillis = startMs }
                                        personalItems.add(PersonalScheduleItem(
                                            id = "p-server-$sid",
                                            title = s.title ?: "",
                                            date = cal,
                                            startTimeMillis = startMs,
                                            endTimeMillis = endMs,
                                            isLocked = s.isPrivate == true,
                                            isChecked = false,
                                            notificationCategory = notiLabel,
                                            scheduleCategory = s.category ?: cat
                                        ))
                                    }
                                }
                            }
                            Log.d("AddPersonalTask", "서버 개인일정 재로드: ${personalItems.size}개")
                            withContext(Dispatchers.Main) {
                                DummyRepository.replacePersonalSchedulesFromServer(personalItems)
                                DummyRepository.saveSchedulesTo(requireContext())
                            }
                        } else {
                            Log.w("AddPersonalTask", "서버 개인일정 재로드 실패: ${markedResp.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("AddPersonalTask", "서버 개인일정 재로드 예외: ${e.message}")
                    }
                    withContext(Dispatchers.Main) { findNavController().popBackStack() }
                } else {
                    Log.w("AddPersonalTask", "개인 일정 생성 실패: ${resp.code()}")
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(requireContext(), "일정 저장에 실패했습니다.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("AddPersonalTask", "개인 일정 생성 예외: ${e.message}")
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
