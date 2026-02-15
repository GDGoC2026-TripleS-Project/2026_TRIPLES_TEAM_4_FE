package com.project.unimate.ui.team

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamCreateBinding
import com.project.unimate.model.AvailableColor
import com.project.unimate.model.CreateTeamRequest
import com.project.unimate.model.InviteCodeResponse
import com.project.unimate.model.TeamCreateResponse
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.TeamService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class TeamCreateFragment : Fragment() {

    private var _binding: FragmentTeamCreateBinding? = null
    private val binding get() = _binding!!

    private val colorButtons by lazy {
        with(binding) {
            listOf(
                btnColorYellow, btnColorBeige, btnColorPeriwinkle,
                btnColorLavender, btnColorMagenta, btnColorPinkLight,
                btnColorCoral, btnColorCoralLight, btnColorMint, btnColorAqua
            )
        }
    }

    private val colorButtonToServerCode: Map<Int, String> = mapOf(
        R.id.btnColorYellow to "C01",
        R.id.btnColorBeige to "C02",
        R.id.btnColorPeriwinkle to "C03",
        R.id.btnColorLavender to "C04",
        R.id.btnColorMagenta to "C05",
        R.id.btnColorPinkLight to "C06",
        R.id.btnColorCoral to "C07",
        R.id.btnColorCoralLight to "C08",
        R.id.btnColorMint to "C09",
        R.id.btnColorAqua to "C10"
    )

    private val serverCodeToHex: Map<String, String> = mapOf(
        "C01" to "#FFE970",
        "C02" to "#FFF8D3",
        "C03" to "#90A3ED",
        "C04" to "#D9E1FF",
        "C05" to "#F488D4",
        "C06" to "#FFD8F3",
        "C07" to "#FF7A6E",
        "C08" to "#FBB0A9",
        "C09" to "#9CE098",
        "C10" to "#D4FFD1-"
    )
//색상 중복 방지
    private fun fetchAvailableColors() {
        val service = RetrofitClient.getInstance(requireContext()).create(TeamService::class.java)

        service.getAvailableColors().enqueue(object : Callback<List<AvailableColor>> {
            override fun onResponse(call: Call<List<AvailableColor>>, response: Response<List<AvailableColor>>) {
                if (response.isSuccessful) {
                    val availableHexList = response.body()?.map {
                        it.hex.uppercase().replace("#", "")
                    } ?: emptyList()

                    colorButtons.forEach { button ->
                        val myColorCode = colorButtonToServerCode[button.id]
                        val myHex = serverCodeToHex[myColorCode]?.uppercase()?.replace("#", "")

                        val isAvailable = myHex != null && availableHexList.contains(myHex)

                        button.isEnabled = isAvailable
                        button.alpha = if (isAvailable) 1.0f else 0.3f

                        Log.d("COLOR_CHECK", "Button: ${button.id}, Hex: $myHex, Available: $isAvailable")
                    }
                }
            }

            override fun onFailure(call: Call<List<AvailableColor>>, t: Throwable) {
                Log.e("API_ERROR", "색상 로드 실패: ${t.message}")
            }
        })
    }

    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                binding.ivTeamProfile.apply {
                    setImageURI(imageUri)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = null
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        colorButtons.forEach { it.setImageResource(0) }

        fetchAvailableColors()
        setupImagePicker()
        setupDateAndTimeListeners()
        setInitialDateAndTimeToNow()
        setupColorListeners()
        setupCompleteButton()
    }

    private fun getTodayDateString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}. ${cal.get(Calendar.MONTH) + 1}. ${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun setupImagePicker() {
        binding.ivTeamProfile.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun setupDateAndTimeListeners() {
        binding.tvStartDate.setOnClickListener { showDatePicker(binding.tvStartDate) }
        binding.tvStartTime.setOnClickListener { showTimePicker(binding.tvStartTime) }
        binding.tvEndDate.setOnClickListener { showDatePicker(binding.tvEndDate) }
        binding.tvEndTime.setOnClickListener { showTimePicker(binding.tvEndTime) }
    }

    private fun setInitialDateAndTimeToNow() {
        val cal = Calendar.getInstance()
        val dateStr = getTodayDateString()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val amPm = if (h < 12) "오전" else "오후"
        val hour = if (h % 12 == 0) 12 else h % 12
        val timeStr = "$amPm ${hour}:${String.format("%02d", m)}"
        binding.tvStartDate.text = dateStr
        binding.tvStartTime.text = timeStr
        binding.tvEndDate.text = dateStr
        binding.tvEndTime.text = timeStr
    }

    private fun showDatePicker(textView: TextView) {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val datePickerDialog = DatePickerDialog(
            contextWrapper,
            { _, year, month, day ->
                textView.text = "${year}. ${month + 1}. ${day}"
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        styleDatePicker(datePickerDialog)
        datePickerDialog.show()
    }

    private fun showTimePicker(textView: TextView) {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val timePickerDialog = TimePickerDialog(
            contextWrapper,
            { _, h, m ->
                val amPm = if (h < 12) "오전" else "오후"
                val hour = if (h % 12 == 0) 12 else h % 12
                textView.text = "$amPm ${hour}:${String.format("%02d", m)}"
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
        styleTimePicker(timePickerDialog)
        timePickerDialog.show()
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

    private fun styleTimePicker(dialog: TimePickerDialog) {
        dialog.setButton(TimePickerDialog.BUTTON_POSITIVE, "확인", dialog)
        dialog.setButton(TimePickerDialog.BUTTON_NEGATIVE, "취소", dialog)
        dialog.setOnShowListener {
            val colorBlack = ContextCompat.getColor(requireContext(), android.R.color.black)
            dialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(colorBlack)
            dialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(colorBlack)
        }
    }

    private fun setupColorListeners() {
        colorButtons.forEach { button ->
            button.setOnClickListener { onColorSelected(button) }
        }
    }

    private fun onColorSelected(selectedButton: ImageButton) {
        colorButtons.forEach { it.setImageResource(0) }
        colorButtons.forEach { it.tag = null }
        selectedButton.setImageResource(R.drawable.ic_check_white)
        selectedButton.tag = "SELECTED"
    }

    private fun setupCompleteButton() {
        binding.btnCompleteCreate.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()
            if (teamName.isEmpty()) {
                Toast.makeText(requireContext(), "팀 명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidDateRange()) return@setOnClickListener

            val isColorSelected = colorButtons.any { it.tag == "SELECTED" }
            if (!isColorSelected) {
                Toast.makeText(requireContext(), "팀 컬러를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val teamDesc = binding.etTeamDesc.text.toString().trim()

            val serverColorCode = colorButtons.find { it.tag == "SELECTED" }?.id?.let { colorButtonToServerCode[it] } ?: "C01"

            val startAt = formatDateTimeForServer(binding.tvStartDate.text.toString(), binding.tvStartTime.text.toString())
            val endAt = formatDateTimeForServer(binding.tvEndDate.text.toString(), binding.tvEndTime.text.toString())

            performCreateTeam(teamName, teamDesc, serverColorCode, startAt, endAt)
        }
    }

    private fun formatDateTimeForServer(dateStr: String, timeStr: String): String {
        val inputFormat = SimpleDateFormat("yyyy. M. d a h:mm", Locale.KOREA)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
        return try {
            val date = inputFormat.parse("$dateStr $timeStr")
            outputFormat.format(date!!)
        } catch (e: Exception) {
            ""
        }
    }

    private fun performCreateTeam(name: String, desc: String, color: String, start: String, end: String) {
        val service = RetrofitClient.getInstance(requireContext()).create(TeamService::class.java)

        val request = CreateTeamRequest(name, desc, color, start, end)


        service.createTeam(request).enqueue(object : Callback<TeamCreateResponse> {
            override fun onResponse(call: Call<TeamCreateResponse>, response: Response<TeamCreateResponse>) {
                if (response.isSuccessful) {
                    // [수정] response.body()?.teamId 라고 쓰면 에러가 납니다.
                    // 위에서 만든 모델 구조에 따라 아래와 같이 단계별로 접근해야 합니다.
                    val createdTeamId = response.body()?.team?.id ?: 0L

                    Log.d("CREATE_CHECK", "생성된 팀 ID: $createdTeamId")

                    // 이제 이 ID를 가지고 초대코드 API를 호출합니다.
                    service.issueInviteCode(createdTeamId).enqueue(object : Callback<InviteCodeResponse> {
                        override fun onResponse(call: Call<InviteCodeResponse>, res: Response<InviteCodeResponse>) {
                            if (res.isSuccessful) {
                                val realInviteCode = res.body()?.inviteCode

                                val bundle = Bundle().apply {
                                    putString("inviteCode", realInviteCode)
                                    putString("teamName", name)
                                }
                                findNavController().navigate(R.id.action_teamCreate_to_teamComplete, bundle)
                            }
                        }
                        override fun onFailure(call: Call<InviteCodeResponse>, t: Throwable) {
                            Toast.makeText(requireContext(), "코드 발급 실패", Toast.LENGTH_SHORT).show()
                        }
                    })
                } else {
                    Toast.makeText(requireContext(), "팀 생성 실패", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<TeamCreateResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isValidDateRange(): Boolean {
        val startDateStr = binding.tvStartDate.text.toString()
        val startTimeStr = binding.tvStartTime.text.toString()
        val endDateStr = binding.tvEndDate.text.toString()
        val endTimeStr = binding.tvEndTime.text.toString()

        val dateFormat = SimpleDateFormat("yyyy. M. d a h:mm", Locale.KOREA)
        return try {
            val startFullDate = dateFormat.parse("$startDateStr $startTimeStr")
            val endFullDate = dateFormat.parse("$endDateStr $endTimeStr")
            if (startFullDate != null && endFullDate != null) {
                if (startFullDate.after(endFullDate)) {
                    showErrorDialog("종료 일시가 시작 일시보다 빠릅니다.\n날짜와 시간을 다시 확인해주세요.")
                    false
                } else if (startFullDate == endFullDate) {
                    showErrorDialog("시작 일시와 종료 일시가 같습니다.\n다시 확인해주세요.")
                    false
                } else true
            } else true
        } catch (e: Exception) {
            true
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("날짜 설정 오류")
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}