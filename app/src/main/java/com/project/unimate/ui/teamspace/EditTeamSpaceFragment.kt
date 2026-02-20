package com.project.unimate.ui.teamspace

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.repository.DeletedSeedTeamStore
import com.project.unimate.data.repository.DeletedUserTeamStore
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.data.repository.PendingCompletionPopupStore
import com.project.unimate.data.repository.SyncManager
import com.project.unimate.data.repository.SeedTeamOverridesStore
import com.project.unimate.data.repository.TeamImageStore
import com.project.unimate.data.repository.TeamNameStore
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamUpdateRequest
import com.project.unimate.network.service.TeamService
import com.project.unimate.utils.ProfileImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditTeamSpaceFragment : Fragment() {

    private var teamId: String? = null
    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()
    private var setEndedTeam = false
    private var selectedTeamImageResName: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val imageUri = result.data?.data ?: return@registerForActivityResult
        val tid = teamId ?: return@registerForActivityResult
        val team = DummyRepository.getTeamById(tid) ?: return@registerForActivityResult
        view?.findViewById<ImageView>(R.id.editTeamSpaceUserIcon)?.apply {
            setImageURI(imageUri)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.TRANSPARENT)
        }
        view?.findViewById<TextView>(R.id.editTeamSpaceUserIconLetter)?.visibility = View.GONE
        val saved = saveTeamImageToFile(imageUri, tid)
        if (saved.isEmpty()) return@registerForActivityResult
        selectedTeamImageResName = saved
        TeamImageStore.save(requireContext(), tid, saved)
        DummyRepository.updateTeam(
            teamId = tid,
            name = team.name,
            intro = team.intro,
            workStartMillis = team.workStartMillis,
            workEndMillis = team.workEndMillis,
            setCompleted = team.isCompleted,
            completedAtMillis = team.completedAtMillis,
            imageResName = saved
        )
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
            team.imageResName.startsWith("http://") || team.imageResName.startsWith("https://") -> {
                ProfileImageLoader.load(userIcon, team.imageResName, requireContext())
                userIconLetter.visibility = View.GONE
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

        fun showTimePicker(cal: Calendar, onSet: (hour: Int, minute: Int) -> Unit) {
            val contextWrapper = android.view.ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
            val dlg = TimePickerDialog(
                contextWrapper,
                { _, h, m ->
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, m)
                    onSet(h, m)
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
        startTimeBtn.setOnClickListener {
            showTimePicker(startCal) { _, _ ->
                refreshDateTimeButtons()
                if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR) && startCal.timeInMillis > endCal.timeInMillis) {
                    endCal.timeInMillis = startCal.timeInMillis
                    refreshDateTimeButtons()
                }
            }
        }
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
        endTimeBtn.setOnClickListener {
            showTimePicker(endCal) { _, _ ->
                refreshDateTimeButtons()
                if (startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR) && endCal.timeInMillis < startCal.timeInMillis) {
                    startCal.timeInMillis = endCal.timeInMillis
                    refreshDateTimeButtons()
                }
            }
        }

        setEndedLayout.setOnClickListener {
            setEndedTeam = !setEndedTeam
            endCheckIcon.setImageResource(
                if (setEndedTeam) R.drawable.ic_end_selected else R.drawable.ic_end_unselected
            )
            endCheckIcon.colorFilter = null
        }
        // 종료된 팀플(종료 체크했거나 마감일이 이미 지남)이면 항상 종료 체크 표시
        val nowInit = System.currentTimeMillis()
        if (team.isCompleted || (team.workEndMillis != null && team.workEndMillis < nowInit)) {
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
            val workEnd = endCal.timeInMillis
            // 종료 체크를 누르지 않았어도 마감일을 과거로 설정하고 저장하면 종료 처리
            val effectivelyEnded = setEndedTeam || (workEnd < now)
            val completedAt = when {
                effectivelyEnded && !team.isCompleted -> now
                effectivelyEnded && team.isCompleted -> (team.completedAtMillis ?: now)
                else -> null
            }
            val imageResNameToSave = selectedTeamImageResName ?: team.imageResName
            if (imageResNameToSave.isNotBlank()) {
                TeamImageStore.save(requireContext(), team.id, imageResNameToSave)
            }
            if (name.isNotBlank()) {
                TeamNameStore.save(requireContext(), team.id, name)
            }
            DummyRepository.updateTeam(
                teamId = team.id,
                name = name,
                intro = intro,
                workStartMillis = workStart,
                workEndMillis = workEnd,
                setCompleted = effectivelyEnded,
                completedAtMillis = completedAt,
                imageResName = imageResNameToSave
            )
            if (DummyRepository.getSeedTeams().any { it.id == team.id }) {
                SeedTeamOverridesStore.save(requireContext(), team.id, effectivelyEnded, workEnd, workStart)
            }
            // API 호출 (팀 정보 수정, 종료 상태 포함) 후 서버 동기화하고 화면 전환
            val numericId = team.id.toLongOrNull()
            fun navigateAfterSave() {
                if (effectivelyEnded && !team.isCompleted) {
                    PendingCompletionPopupStore.add(requireContext(), team.id)
                    showTeamEndDialog(team.id)
                } else {
                    findNavController().popBackStack()
                }
            }
            if (numericId != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val ctx = requireContext()
                        val imageUrlToSend: String? = if (imageResNameToSave.startsWith("http://") || imageResNameToSave.startsWith("https://")) imageResNameToSave else null
                        val service = RetrofitClient.create<TeamService>(ctx)
                        val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val endAtForApi = if (effectivelyEnded && workEnd > now) isoFmt.format(Date(now)) else isoFmt.format(Date(workEnd))
                        val resp = service.updateTeam(numericId, TeamUpdateRequest(
                            name = name,
                            description = intro.ifBlank { null },
                            startAt = isoFmt.format(Date(workStart)),
                            endAt = endAtForApi,
                            completed = effectivelyEnded,
                            isCompleted = effectivelyEnded,
                            imageUrl = imageUrlToSend
                        ))
                        if (resp.isSuccessful) {
                            SyncManager.syncAllDataFromServer(ctx)
                        }
                        withContext(Dispatchers.Main) { navigateAfterSave() }
                    } catch (_: Exception) {
                        withContext(Dispatchers.Main) { navigateAfterSave() }
                    }
                }
            } else {
                navigateAfterSave()
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
            val team = DummyRepository.getTeamById(teamId)
            if (team != null) {
                if (DummyRepository.getSeedTeams().any { it.name == team.name }) {
                    DeletedSeedTeamStore.add(requireContext(), team.name)
                } else {
                    DeletedUserTeamStore.add(requireContext(), teamId)
                }
            }
            DummyRepository.deleteTeam(teamId)
            // API 호출 (팀 삭제)
            val numericId = teamId.toLongOrNull()
            if (numericId != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val service = RetrofitClient.create<TeamService>(requireContext())
                        service.deleteTeam(numericId)
                    } catch (_: Exception) { }
                }
            }
            dialog.dismiss()
            findNavController().popBackStack()
        }
        dialogView.findViewById<View>(R.id.dialogTeamEndCancel).setOnClickListener { dialog.dismiss() }
        dialog.setOnCancelListener { }
        dialog.show()
    }

    private fun showTeamEndDialog(teamId: String) {
        val view = layoutInflater.inflate(R.layout.dialog_team_space_ended, null)
        val dialog = AlertDialog.Builder(requireContext(), R.style.TeamEndDialogTheme)
            .setView(view)
            .setCancelable(false)
            .create()
        view.findViewById<View>(R.id.dialogTeamEndConfirm).setOnClickListener {
            PendingCompletionPopupStore.remove(requireContext(), teamId)
            dialog.dismiss() }
        dialog.setOnDismissListener { findNavController().popBackStack() }
        val dm = resources.displayMetrics
        val wPx = (dm.widthPixels * 0.9f).toInt()
        val hPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260f, dm).toInt()
        dialog.window?.let { window: Window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params: WindowManager.LayoutParams? = window.attributes
            if (params != null) {
                params.width = wPx
                params.height = hPx
                window.attributes = params
            }
            window.setDimAmount(0.6f)
        }
        dialog.show()
        dialog.window?.let { window: Window ->
            window.setLayout(wPx, hPx)
            view.post { window.setLayout(wPx, hPx) }
        }
    }
}
