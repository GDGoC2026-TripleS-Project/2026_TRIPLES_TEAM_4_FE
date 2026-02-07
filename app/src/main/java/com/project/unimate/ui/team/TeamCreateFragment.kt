package com.project.unimate.ui.team


import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamCreateBinding
import java.text.SimpleDateFormat
import java.util.*

class TeamCreateFragment : Fragment() {

    private var _binding: FragmentTeamCreateBinding? = null
    private val binding get() = _binding!!

    // 선택된 이미지 URI 저장


    // 색상 버튼 리스트
    private val colorButtons by lazy {
        with(binding) {
            listOf(
                btnColorYellow, btnColorBeige, btnColorPeriwinkle,
                btnColorLavender, btnColorMagenta, btnColorPinkLight,
                btnColorCoral, btnColorCoralLight, btnColorMint, btnColorAqua
            )
        }
    }


    // 갤러리 이미지 선택 런처
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                // ivProfilePlaceholder 대신 실제 XML에 정의된 ID인 ivTeamProfile 사용
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

        // 초기화: 색상 버튼 체크 해제
        colorButtons.forEach { it.setImageResource(0) }

        setupImagePicker() // 이미지 선택 리스너 연결
        setupDateAndTimeListeners()
        setupColorListeners()
        setupCompleteButton()
    }

    // --- 1. 이미지 선택 로직 ---
    private fun setupImagePicker() {
        // 프로필 이미지 영역 클릭 시 갤러리 열기
        binding.ivTeamProfile.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    // --- 2. 날짜/시간 선택 로직 ---
    private fun setupDateAndTimeListeners() {
        binding.tvStartDate.setOnClickListener { showDatePicker(binding.tvStartDate) }
        binding.tvStartTime.setOnClickListener { showTimePicker(binding.tvStartTime) }
        binding.tvEndDate.setOnClickListener { showDatePicker(binding.tvEndDate) }
        binding.tvEndTime.setOnClickListener { showTimePicker(binding.tvEndTime) }
    }

    private fun showDatePicker(textView: TextView) {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)

        val datePickerDialog = DatePickerDialog(
            contextWrapper,
            { _, year, month, day ->
                // 포맷 통일: yyyy. M. d
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
                // 포맷 통일: 오전/오후 h:mm
                textView.text = "$amPm ${hour}:${String.format("%02d", m)}"
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
        styleTimePicker(timePickerDialog)
        timePickerDialog.show()
    }

    // 다이얼로그 버튼 색상 스타일링 (코드 중복 제거)
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

    // --- 3. 컬러 선택 로직 ---
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

    // --- 4. 완료 버튼 및 유효성 검사 (팝업 포함) ---
    // --- 완료 버튼 로직 ---
    private fun setupCompleteButton() {
        binding.btnCompleteCreate.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()

            // 1. 팀 명 유효성 검사 (필수)
            if (teamName.isEmpty()) {
                Toast.makeText(requireContext(), "팀 명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 날짜 논리 유효성 검사 (시작일이 종료일보다 뒤에 있으면 토스트 메시지)
            val startDateStr = binding.tvStartDate.text.toString()
            val endDateStr = binding.tvEndDate.text.toString()
            val defaultDate = "2026. 2. 23" // XML에 설정된 기본값

            // 시작일과 종료일이 모두 선택된 경우에만 비교
            if (startDateStr != defaultDate && endDateStr != defaultDate) {
                val sdf = java.text.SimpleDateFormat("yyyy. MM. dd", java.util.Locale.KOREA)
                try {
                    val startDate = sdf.parse(startDateStr)
                    val endDate = sdf.parse(endDateStr)

                    if (startDate != null && endDate != null && startDate.after(endDate)) {
                        //  토스트 메시지 사용
                        Toast.makeText(requireContext(), "시작일은 종료일보다 빨라야 합니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. 컬러 선택 유효성 검사 (필수)
            val isColorSelected = colorButtons.any { it.tag == "SELECTED" }
            if (!isColorSelected) {
                Toast.makeText(requireContext(), "팀 컬러를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 모든 검사 통과 시 다음 화면으로 이동
            val tempInviteCode = generateRandomCode()
            val bundle = Bundle().apply {
                putString("inviteCode", tempInviteCode)
                putString("teamName", teamName)
            }
            findNavController().navigate(R.id.action_teamCreate_to_teamComplete, bundle)
        }
    }


    /**
     * 시작 일시와 종료 일시를 비교하는 함수
     * 문제가 있으면 Alert Dialog를 띄우고 false를 반환
     */
    private fun isValidDateRange(): Boolean {
        val startDateStr = binding.tvStartDate.text.toString()
        val startTimeStr = binding.tvStartTime.text.toString()
        val endDateStr = binding.tvEndDate.text.toString()
        val endTimeStr = binding.tvEndTime.text.toString()

        // 기본 텍스트("2026. 2. 23" 등)가 그대로인지 확인 (날짜를 선택하지 않은 경우)
        // 실제 앱에서는 기본값인지 확인하는 로직을 더 정교하게 하거나, 빈 값으로 시작하는 게 좋음
        if (startDateStr == "2026. 2. 23" || endDateStr == "2026. 2. 23") {
            Toast.makeText(context, "시작 및 종료 날짜를 모두 설정해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        // 날짜 포맷: "yyyy. M. d a h:mm" (Locale.KOREA 기준 "오전/오후" 파싱 가능)
        val dateFormat = SimpleDateFormat("yyyy. M. d a h:mm", Locale.KOREA)

        try {
            // 날짜와 시간을 합쳐서 Date 객체로 변환
            // 예: "2026. 2. 23" + " " + "오후 1:05"
            val startFullDate = dateFormat.parse("$startDateStr $startTimeStr")
            val endFullDate = dateFormat.parse("$endDateStr $endTimeStr")

            if (startFullDate != null && endFullDate != null) {
                if (startFullDate.after(endFullDate)) {
                    // [팝업] 종료일이 시작일보다 빠름
                    showErrorDialog("종료 일시가 시작 일시보다 빠릅니다.\n날짜와 시간을 다시 확인해주세요.")
                    return false
                }
                if (startFullDate == endFullDate) {
                    // [팝업] 시작과 종료가 같음 (필요 시 주석 처리)
                    showErrorDialog("시작 일시와 종료 일시가 같습니다.\n다시 확인해주세요.")
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 파싱 에러 시 그냥 통과시키거나 에러 메시지
            return true
        }

        return true
    }

    /**
     * 경고 팝업창 띄우기
     */
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("날짜 설정 오류")
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun generateRandomCode(): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { charset.random() }.joinToString("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}