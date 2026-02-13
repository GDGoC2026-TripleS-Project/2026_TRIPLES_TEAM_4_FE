package com.project.unimate.ui.calendar

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.PersonalScheduleItem
import com.project.unimate.data.repository.DummyRepository
import java.util.Calendar

class EditPersonalTaskFragment : Fragment() {

    private var personalId: String? = null
    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()
    private var allDay = false
    private var notificationOn = false
    private var isPrivate = true
    private var category: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        personalId = arguments?.getString("personalId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_personal_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val item = personalId?.let { DummyRepository.getPersonalById(it) } ?: run {
            findNavController().popBackStack()
            return
        }

        view.findViewById<TextView>(R.id.addPersonalTitle).text = getString(R.string.schedule_edit)
        view.findViewById<TextView>(R.id.addPersonalCancel).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<TextView>(R.id.addPersonalSave).setOnClickListener { savePersonal(item) }
        // 일정 편집 페이지: 팀/개인 토글 선택 불가
        view.findViewById<TextView>(R.id.addPersonalToggleTeam).apply {
            isClickable = false
            isEnabled = false
            alpha = 0.8f
        }
        view.findViewById<TextView>(R.id.addPersonalTogglePersonal).apply {
            isClickable = false
            isEnabled = false
            alpha = 0.8f
        }

        val nameEt = view.findViewById<EditText>(R.id.addPersonalScheduleName)
        val alldayIcon = view.findViewById<ImageView>(R.id.addPersonalAlldayIcon)
        val alldayRow = alldayIcon.parent as View
        val startDateBtn = view.findViewById<Button>(R.id.addPersonalStartDate)
        val startTimeBtn = view.findViewById<Button>(R.id.addPersonalStartTime)
        val endDateBtn = view.findViewById<Button>(R.id.addPersonalEndDate)
        val endTimeBtn = view.findViewById<Button>(R.id.addPersonalEndTime)
        val notificationBtn = view.findViewById<TextView>(R.id.addPersonalNotificationBtn)
        val categoryBtn = view.findViewById<TextView>(R.id.addPersonalCategoryBtn)
        val categoryEt = view.findViewById<EditText>(R.id.addPersonalCategoryEt)
        val categoryListScroll = view.findViewById<ScrollView>(R.id.addPersonalCategoryListScroll)
        val categoryList = view.findViewById<LinearLayout>(R.id.addPersonalCategoryList)
        val publicBtn = view.findViewById<TextView>(R.id.addPersonalPublicBtn)
        val privateBtn = view.findViewById<TextView>(R.id.addPersonalPrivateBtn)

        nameEt.setText(item.title)
        startCal.timeInMillis = item.startTimeMillis
        endCal.timeInMillis = item.endTimeMillis
        allDay = item.startTimeMillis == (Calendar.getInstance().apply {
            timeInMillis = item.startTimeMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        }.timeInMillis)
        notificationOn = item.notificationCategory == "있음"
        isPrivate = item.isLocked
        category = item.scheduleCategory.takeIf { it != "없음" }
        categoryBtn.text = category ?: getString(R.string.select)
        notificationBtn.text = if (notificationOn) getString(R.string.yes_notification) else getString(R.string.none)
        alldayIcon.setImageResource(if (allDay) R.drawable.ic_allday_selected else R.drawable.ic_allday_unselected)

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

        alldayRow.setOnClickListener {
            allDay = !allDay
            alldayIcon.setImageResource(if (allDay) R.drawable.ic_allday_selected else R.drawable.ic_allday_unselected)
            if (allDay) {
                startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0)
                endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59)
            }
            refreshDateTime()
        }
        startDateBtn.setOnClickListener {
            val dlg = DatePickerDialog(requireContext(), { _, y, m, d ->
                startCal.set(y, m, d); if (allDay) endCal.set(y, m, d)
                refreshDateTime()
            }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH))
            dlg.show()
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(android.R.color.black, null))
        }
        fun showTimeOptionPicker(cal: Calendar, onConfirm: () -> Unit) {
            val v = layoutInflater.inflate(R.layout.dialog_time_option, null)
            val amPmSpinner = v.findViewById<Spinner>(R.id.dialogTimeAmPm)
            val hourSpinner = v.findViewById<Spinner>(R.id.dialogTimeHour)
            val confirmBtn = v.findViewById<Button>(R.id.dialogTimeConfirm)
            amPmSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("오전", "오후"))
            hourSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, (1..12).map { "$it" })
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val isPm = hourOfDay >= 12
            val hour12 = if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay
            amPmSpinner.setSelection(if (isPm) 1 else 0)
            hourSpinner.setSelection(hour12 - 1)
            val dialog = AlertDialog.Builder(requireContext()).setView(v).create()
            confirmBtn.setOnClickListener {
                val pm = amPmSpinner.selectedItemPosition == 1
                val h12 = hourSpinner.selectedItemPosition + 1
                val h = if (pm) if (h12 == 12) 12 else h12 + 12 else if (h12 == 12) 0 else h12
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, 0)
                onConfirm()
                dialog.dismiss()
            }
            dialog.show()
        }
        startTimeBtn.setOnClickListener { showTimeOptionPicker(startCal) { refreshDateTime() } }
        endDateBtn.setOnClickListener {
            val dlg = DatePickerDialog(requireContext(), { _, y, m, d ->
                endCal.set(y, m, d); refreshDateTime()
            }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH))
            dlg.show()
            dlg.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(android.R.color.black, null))
        }
        endTimeBtn.setOnClickListener { showTimeOptionPicker(endCal) { refreshDateTime() } }
        notificationBtn.setOnClickListener {
            notificationOn = !notificationOn
            notificationBtn.text = if (notificationOn) getString(R.string.yes_notification) else getString(R.string.none)
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
        categoryBtn.setOnClickListener {
            categoryListScroll.visibility = if (categoryListScroll.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (categoryListScroll.visibility == View.VISIBLE && categoryList.childCount == 0) {
                categories.forEachIndexed { index, label ->
                    val row = TextView(requireContext()).apply {
                        text = label
                        setPadding(32, 24, 32, 24)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gray09))
                        setBackgroundResource(if ((category == label) || (category == null && index == 0)) R.drawable.bg_option_selected else android.R.color.transparent)
                        setOnClickListener {
                            if (label == getString(R.string.category_etc)) {
                                categoryBtn.visibility = View.GONE
                                categoryEt.visibility = View.VISIBLE
                                categoryEt.hint = getString(R.string.category_etc)
                                category = label
                            } else {
                                category = label
                                categoryBtn.text = label
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

    private fun savePersonal(original: PersonalScheduleItem) {
        val name = view?.findViewById<EditText>(R.id.addPersonalScheduleName)?.text?.toString()?.trim() ?: ""
        if (name.isEmpty()) return
        val catEt = view?.findViewById<EditText>(R.id.addPersonalCategoryEt)
        val cat = if (catEt?.visibility == View.VISIBLE) (catEt.text?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: getString(R.string.category_etc)) else (category ?: "없음")
        val date = (startCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val updated = original.copy(
            title = name,
            date = date,
            startTimeMillis = startCal.timeInMillis,
            endTimeMillis = endCal.timeInMillis,
            isLocked = isPrivate,
            notificationCategory = if (notificationOn) "있음" else "없음",
            scheduleCategory = cat
        )
        DummyRepository.updatePersonalSchedule(updated)
        findNavController().popBackStack()
    }
}
