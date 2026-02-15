package com.project.unimate.ui.teamspace

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.model.TeamDetailResponse
import com.project.unimate.model.UpdateTeamRequest
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.TeamService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditTeamSpaceFragment : Fragment() {

    private var teamId: String? = null
    private val teamIdLong: Long get() = teamId?.toLongOrNull() ?: 0L
    private val startCal = Calendar.getInstance()
    private val endCal = Calendar.getInstance()
    private var setEndedTeam = false
    private lateinit var teamService: TeamService


    private var amILeader: Boolean = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data ?: return@registerForActivityResult
            view?.findViewById<ImageView>(R.id.editTeamSpaceUserIcon)?.apply {
                setImageURI(imageUri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.TRANSPARENT)
            }
            view?.findViewById<TextView>(R.id.editTeamSpaceUserIconLetter)?.visibility = View.GONE
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
        teamService = RetrofitClient.getInstance(requireContext()).create(TeamService::class.java)

        val back = view.findViewById<ImageButton>(R.id.editTeamSpaceBack)
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

        fetchInitialData(view)

        back.setOnClickListener { findNavController().popBackStack() }

        // [수정] Intent 경고 해결: setDataAndType 사용
        photoEdit.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            pickImageLauncher.launch(intent)
        }

        startDateBtn.setOnClickListener {
            showDatePicker(startCal) { y, m, d ->
                startCal.set(y, m, d)
                refreshDateTimeButtons(view)
            }
        }
        startTimeBtn.setOnClickListener {
            showTimeOptionPicker(startCal) { refreshDateTimeButtons(view) }
        }

        endDateBtn.setOnClickListener {
            showDatePicker(endCal) { y, m, d ->
                endCal.set(y, m, d)
                refreshDateTimeButtons(view)
            }
        }
        endTimeBtn.setOnClickListener {
            showTimeOptionPicker(endCal) { refreshDateTimeButtons(view) }
        }

        setEndedLayout.setOnClickListener {
            setEndedTeam = !setEndedTeam
            endCheckIcon.setImageResource(
                if (setEndedTeam) R.drawable.ic_end_selected else R.drawable.ic_end_unselected
            )
        }

        completeBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            if (name.isEmpty()) {
                nameEt.error = getString(R.string.team_name_required)
                return@setOnClickListener
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val request = UpdateTeamRequest(
                name = name,
                description = introEt.text.toString().trim(),
                startAt = sdf.format(startCal.time),
                endAt = sdf.format(endCal.time)
            )

            teamService.updateTeam(teamIdLong, request).enqueue(object : Callback<Response<Unit>> {
                override fun onResponse(call: Call<Response<Unit>>, response: Response<Response<Unit>>) {
                    if (response.isSuccessful) {
                        if (setEndedTeam) showTeamEndDialog()
                        else findNavController().popBackStack()
                    } else {
                        Toast.makeText(context, "수정 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Response<Unit>>, t: Throwable) {
                    Log.e("EditTeam", "Fail: ${t.message}")
                }
            })
        }

        deleteTeamSpace.setOnClickListener {
            if (amILeader) {
                showDeleteTeamDialog()
            } else {
                showLeaveTeamDialog()
            }
        }
    }

    private fun fetchInitialData(view: View) {
        teamService.getTeamDetail(teamIdLong).enqueue(object : Callback<TeamDetailResponse> {
            override fun onResponse(call: Call<TeamDetailResponse>, response: Response<TeamDetailResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        view.findViewById<EditText>(R.id.editTeamSpaceName).setText(data.team.name)
                        view.findViewById<EditText>(R.id.editTeamSpaceIntro).setText(data.team.description)

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        data.team.startAt?.let { sdf.parse(it)?.let { date -> startCal.time = date } }
                        data.team.endAt?.let { sdf.parse(it)?.let { date -> endCal.time = date } }

                        refreshDateTimeButtons(view)

                        val userIcon = view.findViewById<ImageView>(R.id.editTeamSpaceUserIcon)
                        val userIconLetter = view.findViewById<TextView>(R.id.editTeamSpaceUserIconLetter)
                        userIcon.setBackgroundColor(Color.parseColor(data.team.colorHex ?: "#CCCCCC"))
                        userIconLetter.text = data.team.name.firstOrNull()?.toString() ?: ""

                        // [참고] TeamDetailResponse에 isLeader 필드가 없다면 여기서 빨간 줄이 뜹니다.
                        // 데이터 클래스에 val isLeader: Boolean = false 등을 추가해주세요.
                        amILeader = data.isLeader

                        val deleteLabel = view.findViewById<TextView>(R.id.deleteTeamSpace)
                        if (deleteLabel != null) {
                            if (amILeader) {
                                deleteLabel.text = "팀 스페이스 삭제"
                                deleteLabel.setTextColor(Color.GRAY)
                            } else {
                                deleteLabel.text = "팀 나가기"
                                deleteLabel.setTextColor(Color.GRAY)
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<TeamDetailResponse>, t: Throwable) {}
        })
    }

    private fun refreshDateTimeButtons(view: View) {
        val startDateBtn = view.findViewById<Button>(R.id.editTeamSpaceStartDate)
        val startTimeBtn = view.findViewById<Button>(R.id.editTeamSpaceStartTime)
        val endDateBtn = view.findViewById<Button>(R.id.editTeamSpaceEndDate)
        val endTimeBtn = view.findViewById<Button>(R.id.editTeamSpaceEndTime)

        startDateBtn.text = String.format("%d. %d. %d", startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH) + 1, startCal.get(Calendar.DAY_OF_MONTH))
        startTimeBtn.text = formatTime(startCal)
        endDateBtn.text = String.format("%d. %d. %d", endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH) + 1, endCal.get(Calendar.DAY_OF_MONTH))
        endTimeBtn.text = formatTime(endCal)
    }

    private fun formatTime(cal: Calendar): String {
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (hour < 12) "오전" else "오후"
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return "$amPm $h:${minute.toString().padStart(2, '0')}"
    }

    private fun showDatePicker(cal: Calendar, onSet: (y: Int, m: Int, d: Int) -> Unit) {
        DatePickerDialog(requireContext(), R.style.MyDatePickerDialogTheme, { _, y, m, d -> onSet(y, m, d) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimeOptionPicker(cal: Calendar, onSet: () -> Unit) {
        val view = layoutInflater.inflate(R.layout.dialog_time_option, null)
        val amPmSpinner = view.findViewById<Spinner>(R.id.dialogTimeAmPm)
        val hourSpinner = view.findViewById<Spinner>(R.id.dialogTimeHour)
        val confirmBtn = view.findViewById<Button>(R.id.dialogTimeConfirm)

        amPmSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("오전", "오후"))
        hourSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, (1..12).map { "$it" })

        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY)
        amPmSpinner.setSelection(if (hourOfDay >= 12) 1 else 0)
        hourSpinner.setSelection((if (hourOfDay % 12 == 0) 12 else hourOfDay % 12) - 1)

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        confirmBtn.setOnClickListener {
            val pm = amPmSpinner.selectedItemPosition == 1
            val h12 = hourSpinner.selectedItemPosition + 1
            val h = if (pm) (if (h12 == 12) 12 else h12 + 12) else (if (h12 == 12) 0 else h12)
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, 0)
            onSet()
            dialog.dismiss()
        }
        dialog.show()
    }

    // [팀원용] 팀 나가기 다이얼로그
    private fun showLeaveTeamDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_team_delete_confirm, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        // [수정] 제공해주신 xml 구조에 맞춰 deleteConfirmMessage만 사용
        val msgView = dialogView.findViewById<TextView>(R.id.deleteConfirmMessage)
        val confirmBtn = dialogView.findViewById<Button>(R.id.dialogTeamEndConfirm)

        msgView.text = "정말 이 팀을 나가시겠습니까?"
        confirmBtn.text = "나가기" // 버튼 텍스트도 상황에 맞게 변경

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            performLeaveTeam()
        }

        dialogView.findViewById<View>(R.id.dialogTeamEndCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performLeaveTeam() {
        if (teamIdLong == 0L) return

        teamService.leaveTeam(teamIdLong).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "팀에서 탈퇴되었습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.myPageFragment, null, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build())
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "팀장은 탈퇴할 수 없습니다. 팀 삭제를 이용해주세요."
                        403 -> "권한이 없습니다."
                        404 -> "존재하지 않는 팀입니다."
                        else -> "탈퇴 실패 (코드: ${response.code()})"
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.e("API_ERROR", "Leave team fail: ${t.message}")
                Toast.makeText(requireContext(), "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // [팀장용] 팀 삭제 다이얼로그
    private fun showDeleteTeamDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_team_delete_confirm, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        // [수정] 제공해주신 xml 구조에 맞춰 deleteConfirmMessage만 사용
        val msgView = dialogView.findViewById<TextView>(R.id.deleteConfirmMessage)
        val confirmBtn = dialogView.findViewById<Button>(R.id.dialogTeamEndConfirm)

        msgView.text = "팀 스페이스를 삭제하시겠습니까?" // 문구 변경
        confirmBtn.text = "삭제"

        confirmBtn.setOnClickListener {
            dialog.dismiss()
            performDeleteTeam()
        }

        dialogView.findViewById<View>(R.id.dialogTeamEndCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performDeleteTeam() {
        if (teamIdLong == 0L) return

        teamService.deleteTeam(teamIdLong).enqueue(object : Callback<Unit> {
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "팀이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.myPageFragment, null, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build())
                } else {
                    Toast.makeText(requireContext(), "삭제 실패 (코드: ${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Toast.makeText(requireContext(), "네트워크 연결을 확인해주세요.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showTeamEndDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("팀 종료")
            .setMessage("팀 활동이 종료되었습니다.")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }
}