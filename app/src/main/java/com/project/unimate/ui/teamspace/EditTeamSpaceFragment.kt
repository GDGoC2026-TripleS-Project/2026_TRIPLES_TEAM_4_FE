package com.project.unimate.ui.teamspace

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class EditTeamSpaceFragment : Fragment() {

    private var teamId: String? = null
    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()
    private var setEndedTeam = false
    private var selectedTeamImageResName: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data ?: return@registerForActivityResult
            val tid = teamId ?: return@registerForActivityResult
            view?.findViewById<ImageView>(R.id.editTeamSpaceUserIcon)?.apply {
                setImageURI(imageUri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.TRANSPARENT)
            }
            view?.findViewById<TextView>(R.id.editTeamSpaceUserIconLetter)?.visibility = View.GONE
            val saved = saveTeamImageToFile(imageUri, tid)
            if (saved.isNotEmpty()) selectedTeamImageResName = saved
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        teamId = arguments?.getString("teamId")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_team_space, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val team = teamId?.let { DummyRepository.getTeamById(it) } ?: run {
            findNavController().popBackStack()
            return
        }

        val back = view.findViewById<ImageButton>(R.id.editTeamSpaceBack)
        val userIcon = view.findViewById<ImageView>(R.id.editTeamSpaceUserIcon)
        val userIconLetter = view.findViewById<TextView>(R.id.editTeamSpaceUserIconLetter)
        val photoEdit = view.findViewById<ImageButton>(R.id.editTeamSpacePhotoEdit)
        val nameEt = view.findViewById<EditText>(R.id.editTeamSpaceName)
        val introEt = view.findViewById<EditText>(R.id.editTeamSpaceIntro)
        val startDateBtn = view.findViewById<Button>(R.id.editTeamSpaceStartDate)
        val startTimeBtn = view.findViewById<Button>(R.id.editTeamSpaceStartTime)
        val endDateBtn = view.findViewById<Button>(R.id.editTeamSpaceEndDate)
        val endTimeBtn = view.findViewById<Button>(R.id.editTeamSpaceEndTime)
        val setEndedLayout = view.findViewById<LinearLayout>(R.id.editTeamSpaceSetEnded)
        val endCheckIcon = view.findViewById<ImageView>(R.id.editTeamSpaceEndCheckIcon)
        val completeBtn = view.findViewById<Button>(R.id.editTeamSpaceComplete)
        val deleteTeamSpace = view.findViewById<View>(R.id.deleteTeamSpace)

        back.setOnClickListener { findNavController().popBackStack() }
        photoEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        nameEt.setText(team.name)
        introEt.setText(team.intro)

        when {
            team.imageResName.startsWith("file:") -> {
                val file = File(requireContext().filesDir, team.imageResName.removePrefix("file:"))
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.let { userIcon.setImageBitmap(it); userIcon.setBackgroundColor(Color.TRANSPARENT); userIconLetter.visibility = View.GONE }
                } else {
                    userIcon.setImageDrawable(null)
                    userIcon.setBackgroundColor(Color.parseColor(team.colorHex))
                    userIconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                    userIconLetter.visibility = View.VISIBLE
                }
            }
            team.imageResName.isNotBlank() -> {
                val resId = resources.getIdentifier(team.imageResName, "drawable", requireContext().packageName)
                if (resId != 0) {
                    userIcon.setImageResource(resId)
                    userIcon.setBackgroundColor(Color.TRANSPARENT)
                    userIconLetter.visibility = View.GONE
                } else {
                    userIcon.setImageDrawable(null)
                    userIcon.setBackgroundColor(Color.parseColor(team.colorHex))
                    userIconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                    userIconLetter.visibility = View.VISIBLE
                }
            }
            else -> {
                userIcon.setImageDrawable(null)
                userIcon.setBackgroundColor(Color.parseColor(team.colorHex))
                userIconLetter.text = team.name.firstOrNull()?.toString() ?: ""
                userIconLetter.visibility = View.VISIBLE
            }
        }
        userIcon.scaleType = ImageView.ScaleType.CENTER_CROP

        startCal.timeInMillis = team.workStartMillis ?: System.currentTimeMillis()
        endCal.timeInMillis = team.workEndMillis ?: System.currentTimeMillis()

        fun formatDate(cal: Calendar): String =
            "${cal.get(Calendar.YEAR)}. ${cal.get(Calendar.MONTH) + 1}. ${cal.get(Calendar.DAY_OF_MONTH)}"
        fun formatTime(cal: Calendar): String {
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            val amPm = if (hour < 12) "오전" else "오후"
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            return "$amPm $h:${minute.toString().padStart(2, '0')}"
        }

        fun refreshDateTimeButtons() {
            startDateBtn.text = formatDate(startCal)
            startTimeBtn.text = formatTime(startCal)
            endDateBtn.text = formatDate(endCal)
            endTimeBtn.text = formatTime(endCal)
        }
        refreshDateTimeButtons()

        // 날짜/시간 버튼
        val dateTimeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_date_time_btn)
        val gray06 = ContextCompat.getColor(requireContext(), R.color.gray06)
        listOf(startDateBtn, startTimeBtn, endDateBtn, endTimeBtn).forEach { btn ->
            btn.backgroundTintList = null
            btn.background = dateTimeBg
            btn.setTextColor(gray06)
        }

        fun showDatePicker(cal: Calendar, onSet: (y: Int, m: Int, d: Int) -> Unit) {
            val dlg = DatePickerDialog(
                requireContext(),
                R.style.MyDatePickerDialogTheme,
                { _, y, m, d -> onSet(y, m, d) },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dlg.setButton(DatePickerDialog.BUTTON_POSITIVE, "확인", dlg)
            dlg.setButton(DatePickerDialog.BUTTON_NEGATIVE, "취소", dlg)
            dlg.setOnShowListener {
                val colorBlack = ContextCompat.getColor(requireContext(), android.R.color.black)
                dlg.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(colorBlack)
                dlg.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(colorBlack)
            }
            dlg.show()
        }

        fun showTimeOptionPicker(cal: Calendar, onSet: (hour: Int, minute: Int) -> Unit) {
            val view = layoutInflater.inflate(R.layout.dialog_time_option, null)
            val amPmSpinner = view.findViewById<Spinner>(R.id.dialogTimeAmPm)
            val hourSpinner = view.findViewById<Spinner>(R.id.dialogTimeHour)
            val confirmBtn = view.findViewById<Button>(R.id.dialogTimeConfirm)
            amPmSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("오전", "오후"))
            hourSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, (1..12).map { "$it" })
            val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
            val isPm = hourOfDay >= 12
            val hour12 = if (hourOfDay == 0) 12 else if (hourOfDay > 12) hourOfDay - 12 else hourOfDay
            amPmSpinner.setSelection(if (isPm) 1 else 0)
            hourSpinner.setSelection(hour12 - 1)
            val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
            confirmBtn.setOnClickListener {
                val pm = amPmSpinner.selectedItemPosition == 1
                val h12 = hourSpinner.selectedItemPosition + 1
                val h = if (pm) if (h12 == 12) 12 else h12 + 12 else if (h12 == 12) 0 else h12
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, 0)
                refreshDateTimeButtons()
                dialog.dismiss()
            }
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(resources.getColor(android.R.color.black, null))
        }

        startDateBtn.setOnClickListener {
            showDatePicker(startCal) { y, m, d ->
                startCal.set(Calendar.YEAR, y)
                startCal.set(Calendar.MONTH, m)
                startCal.set(Calendar.DAY_OF_MONTH, d)
                if (startCal.after(endCal)) {
                    endCal.set(Calendar.YEAR, startCal.get(Calendar.YEAR))
                    endCal.set(Calendar.MONTH, startCal.get(Calendar.MONTH))
                    endCal.set(Calendar.DAY_OF_MONTH, startCal.get(Calendar.DAY_OF_MONTH))
                }
                refreshDateTimeButtons()
            }
        }
        startTimeBtn.setOnClickListener { showTimeOptionPicker(startCal) { _, _ -> refreshDateTimeButtons() } }
        endDateBtn.setOnClickListener {
            showDatePicker(endCal) { y, m, d ->
                endCal.set(Calendar.YEAR, y)
                endCal.set(Calendar.MONTH, m)
                endCal.set(Calendar.DAY_OF_MONTH, d)
                if (endCal.before(startCal)) {
                    startCal.set(Calendar.YEAR, endCal.get(Calendar.YEAR))
                    startCal.set(Calendar.MONTH, endCal.get(Calendar.MONTH))
                    startCal.set(Calendar.DAY_OF_MONTH, endCal.get(Calendar.DAY_OF_MONTH))
                }
                refreshDateTimeButtons()
            }
        }
        endTimeBtn.setOnClickListener { showTimeOptionPicker(endCal) { _, _ -> refreshDateTimeButtons() } }

        setEndedLayout.setOnClickListener {
            setEndedTeam = !setEndedTeam
            endCheckIcon.setImageResource(
                if (setEndedTeam) R.drawable.ic_end_selected else R.drawable.ic_end_unselected
            )
            endCheckIcon.colorFilter = null
        }
        if (team.isCompleted) {
            setEndedTeam = true
            endCheckIcon.setImageResource(R.drawable.ic_end_selected)
            endCheckIcon.colorFilter = null
        }

        completeBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                nameEt.error = getString(R.string.team_name_required)
                return@setOnClickListener
            }
            val intro = introEt.text.toString().trim()
            val workStart = startCal.timeInMillis
            val now = System.currentTimeMillis()
            val workEnd = if (setEndedTeam && !team.isCompleted) now else endCal.timeInMillis
            val completedAt = when {
                setEndedTeam && !team.isCompleted -> now
                setEndedTeam && team.isCompleted -> team.completedAtMillis
                else -> null
            }
            val imageResNameToSave = selectedTeamImageResName ?: team.imageResName
            DummyRepository.updateTeam(
                teamId = team.id,
                name = name,
                intro = intro,
                workStartMillis = workStart,
                workEndMillis = workEnd,
                setCompleted = setEndedTeam,
                completedAtMillis = completedAt,
                imageResName = imageResNameToSave
            )
            if (setEndedTeam) {
                showTeamEndDialog()
            } else {
                findNavController().popBackStack()
            }
        }

        deleteTeamSpace.setOnClickListener { showDeleteConfirmDialog(team.id) }
    }

    private fun saveTeamImageToFile(uri: Uri, teamId: String): String {
        val fileName = "team_$teamId.jpg"
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val file = File(requireContext().filesDir, fileName)
                FileOutputStream(file).use { output -> input.copyTo(output) }
                "file:$fileName"
            } ?: ""
        } catch (e: Exception) { "" }
    }

    private fun showDeleteConfirmDialog(teamId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_team_delete_confirm, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        dialogView.findViewById<View>(R.id.dialogTeamEndConfirm).setOnClickListener {
            DummyRepository.deleteTeam(teamId)
            dialog.dismiss()
            findNavController().popBackStack()
        }
        dialogView.findViewById<View>(R.id.dialogTeamEndCancel).setOnClickListener { dialog.dismiss() }
        dialog.setOnCancelListener { }
        dialog.show()
    }

    private fun showTeamEndDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_team_space_ended, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.TeamEndDialogTheme)
            .setView(view)
            .setCancelable(false)
            .create()
        view.findViewById<View>(R.id.dialogTeamEndConfirm).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { findNavController().popBackStack() }
        val dm = resources.displayMetrics
        val wPx = (dm.widthPixels * 0.9f).toInt()
        val hPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260f, dm).toInt()
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.attributes?.let { params ->
                params.width = wPx
                params.height = hPx
                window.attributes = params
            }
            window.setDimAmount(0.6f)
        }
        dialog.show()
        dialog.window?.let { window ->
            window.setLayout(wPx, hPx)
            view.post { window.setLayout(wPx, hPx) }
        }
    }
}
