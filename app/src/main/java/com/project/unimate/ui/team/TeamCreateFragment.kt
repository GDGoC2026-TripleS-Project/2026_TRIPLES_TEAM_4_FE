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
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DeletedSeedTeamStore
import com.project.unimate.data.repository.DeletedUserTeamStore
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.data.repository.SeedTeamOverridesStore
import com.project.unimate.data.repository.TeamImageStore
import com.project.unimate.databinding.FragmentTeamCreateBinding
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamCreateRequest
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    /** 팀 생성 시 DB 저장용 색상 hex (버튼 id → hex) */
    private val colorButtonToHex: Map<Int, String> = mapOf(
        R.id.btnColorYellow to "#FFE970",
        R.id.btnColorBeige to "#FFF8D3",
        R.id.btnColorPeriwinkle to "#90A3ED",
        R.id.btnColorLavender to "#D9E1FF",
        R.id.btnColorMagenta to "#F488D4",
        R.id.btnColorPinkLight to "#FFD8F3",
        R.id.btnColorCoral to "#FF7A6E",
        R.id.btnColorCoralLight to "#FBB0A9",
        R.id.btnColorMint to "#3FE9C0",
        R.id.btnColorAqua to "#C2FFF0"
    )

    /** API용 색상 코드 (버튼 id → 서버 color enum) */
    private val colorButtonToCode: Map<Int, String> = mapOf(
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


    /** 현재 사용 중인 색상 코드 집합 (C01~C10). 화면 진입 시 API로 갱신됨 */
    private var usedColorCodes: Set<String> = emptySet()

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
        setInitialDateAndTimeToNow()
        setupColorListeners()
        setupCompleteButton()

        // 화면 진입 시 사용 중인 색상 로드 → 팔레트 비활성화 처리
        loadAvailableColors()
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

    /** 페이지 첫 진입 시 시작/종료 날짜·시간을 현재 날짜·시간으로 표시 */
    private fun setInitialDateAndTimeToNow() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dateStr = "$year. $month. $day"
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
        if (selectedButton.tag == "USED") {
            Toast.makeText(requireContext(), "이미 사용 중인 팀 색상입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        colorButtons.forEach { btn ->
            if (btn.tag != "USED") {
                btn.tag = null
                btn.setImageResource(0)
            }
        }
        selectedButton.setImageResource(R.drawable.ic_check_white)
        selectedButton.tag = "SELECTED"
    }

    // --- 3-1. 사용 중인 색상 로드 (GET /api/teams/colors/available 우선, 실패 시 GET /api/teams 폴백) ---
    private fun loadAvailableColors() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<TeamService>(ctx)
                val resp = service.getAvailableColors()
                if (resp.isSuccessful) {
                    val available = resp.body() ?: emptyList()
                    val availableCodes = available.mapNotNull { it.name?.uppercase() }.toSet()
                    val allCodes = colorButtonToCode.values.toSet()
                    usedColorCodes = allCodes - availableCodes
                    Log.d("TeamCreate", "사용 가능 색상: $availableCodes | 사용 중: $usedColorCodes")
                    updateColorButtonStates()
                } else {
                    Log.w("TeamCreate", "getAvailableColors 실패(${resp.code()}), 팀 목록 폴백")
                    loadUsedColorsFromTeams()
                }
            } catch (e: Exception) {
                Log.w("TeamCreate", "getAvailableColors 예외: ${e.message}, 팀 목록 폴백")
                loadUsedColorsFromTeams()
            }
        }
    }

    private fun loadUsedColorsFromTeams() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<TeamService>(ctx)
                val resp = service.getMyTeams()
                if (resp.isSuccessful) {
                    val teams = resp.body() ?: emptyList()
                    usedColorCodes = teams.mapNotNull { it.color?.uppercase() }.toSet()
                    Log.d("TeamCreate", "팀 목록으로 사용 중 색상 확인: $usedColorCodes")
                    updateColorButtonStates()
                }
            } catch (e: Exception) {
                Log.e("TeamCreate", "팀 목록 조회 실패: ${e.message}")
            }
        }
    }

    private fun updateColorButtonStates() {
        colorButtons.forEach { button ->
            val code = colorButtonToCode[button.id] ?: return@forEach
            if (code.uppercase() in usedColorCodes) {
                button.alpha = 0.35f
                button.tag = "USED"
                button.setImageResource(0)
            } else {
                button.alpha = 1.0f
                if (button.tag == "USED") button.tag = null
            }
        }
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
            val selectedBtn = colorButtons.find { it.tag == "SELECTED" }
            if (selectedBtn == null) {
                Toast.makeText(requireContext(), "팀 컬러를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val colorCode = colorButtonToCode[selectedBtn.id] ?: "C03"
            val colorHex = colorButtonToHex[selectedBtn.id] ?: "#90A3ED"

            // 4. 최종 중복 색상 검증 (우회 방지)
            if (colorCode.uppercase() in usedColorCodes) {
                Toast.makeText(requireContext(), "이미 사용 중인 팀 색상입니다. 다른 색상을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 로컬 더미 + API 처리
            val tempInviteCode = generateRandomCode()
            val localTeamId = "created_${System.currentTimeMillis()}"
            val (workStart, workEnd) = parseWorkDateTimes(binding.tvStartDate.text.toString(), binding.tvStartTime.text.toString(), binding.tvEndDate.text.toString(), binding.tvEndTime.text.toString())
            val imageResName = selectedImageUri?.let { saveTeamImageToFile(it, localTeamId) } ?: ""
            val intro = binding.etTeamDesc.text?.toString()?.trim() ?: ""

            // 더미 데이터에도 추가 (fallback)
            val newTeam = Team(
                id = localTeamId,
                name = teamName,
                colorHex = colorHex,
                imageResName = imageResName,
                isCompleted = false,
                memberCount = 4,
                deadlineDays = 7,
                intro = intro,
                workStartMillis = workStart,
                workEndMillis = workEnd,
                completedAtMillis = null
            )
            DummyRepository.addTeam(newTeam)

            // API 호출 — 결과에 따라 네비게이션 또는 에러 처리
            val startAtIso = workStart?.let { formatToIso(it) } ?: formatToIso(System.currentTimeMillis())
            val endAtIso = workEnd?.let { formatToIso(it) } ?: formatToIso(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)
            binding.btnCompleteCreate.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val apiResp = service.createTeam(TeamCreateRequest(
                        name = teamName,
                        description = intro.ifBlank { null },
                        color = colorCode,
                        startAt = startAtIso,
                        endAt = endAtIso
                    ))
                    if (apiResp.code() == 409) {
                        // 서버에서 색상 중복 감지
                        Log.w("TeamCreate", "409: 색상 중복 - $colorCode")
                        usedColorCodes = usedColorCodes + colorCode.uppercase()
                        updateColorButtonStates()
                        binding.btnCompleteCreate.isEnabled = true
                        Toast.makeText(requireContext(), "이미 사용 중인 팀 색상입니다. 다른 색상을 선택해주세요.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (apiResp.isSuccessful) {
                        val newTeamId = apiResp.body()?.id
                        if (newTeamId != null && imageResName.isNotBlank() && imageResName.startsWith("file:team_")) {
                            val oldFileName = imageResName.removePrefix("file:")
                            val newFileName = "team_$newTeamId.jpg"
                            withContext(Dispatchers.IO) {
                                val dir = requireContext().filesDir
                                val src = java.io.File(dir, oldFileName)
                                val dst = java.io.File(dir, newFileName)
                                if (src.exists()) src.copyTo(dst, overwrite = true)
                            }
                        }
                        val myTeamsResp = service.getMyTeams()
                        if (myTeamsResp.isSuccessful) {
                            val deletedUserIds = DeletedUserTeamStore.getDeletedIds(requireContext())
                            val list = myTeamsResp.body()?.filter { r -> r.id?.toString() !in deletedUserIds } ?: emptyList()
                            val newIdStr = newTeamId?.toString()
                            val serverTeams = list.mapNotNull { r ->
                                val id = r.id ?: return@mapNotNull null
                                val completed = r.completed == true || r.isCompleted == true
                                val endMillis = parseIsoToMillis(r.endAt)
                                val teamImageResName = if (newIdStr != null && id.toString() == newIdStr && imageResName.isNotBlank())
                                    "file:team_$newTeamId.jpg" else ""
                                Team(
                                    id = id.toString(),
                                    name = r.name ?: "",
                                    colorHex = r.colorHex ?: "#cccccc",
                                    imageResName = teamImageResName,
                                    isCompleted = completed,
                                    memberCount = (r.memberCount ?: 0).toInt(),
                                    deadlineDays = null,
                                    intro = r.description ?: "",
                                    workStartMillis = parseIsoToMillis(r.startAt),
                                    workEndMillis = endMillis,
                                    completedAtMillis = if (completed) endMillis else null
                                )
                            }
                            val deletedNames = DeletedSeedTeamStore.getDeletedNames(requireContext())
                            val merged = DummyRepository.mergeServerTeamsWithSeed(serverTeams, deletedNames)
                            val seedIds = DummyRepository.getSeedTeams().map { it.id }.toSet()
                            val withOverrides = SeedTeamOverridesStore.applyOverrides(requireContext(), merged, seedIds)
                            withContext(Dispatchers.Main) {
                                DummyRepository.replaceTeamsWithServerData(withOverrides)
                                DummyRepository.applyPersistedTeamImages(requireContext())
                                DummyRepository.applyPersistedTeamNames(requireContext())
                                if (newIdStr != null && imageResName.isNotBlank() && imageResName.startsWith("file:team_")) {
                                    TeamImageStore.save(requireContext(), newIdStr, "file:team_$newTeamId.jpg")
                                }
                            }
                        }
                    }
                    Log.d("TeamCreate", "팀 생성 API 응답: ${apiResp.code()}")
                } catch (e: Exception) {
                    Log.e("TeamCreate", "팀 생성 API 실패: ${e.message}")
                }
                if (isAdded) {
                    val bundle = Bundle().apply {
                        putString("inviteCode", tempInviteCode)
                        putString("teamName", teamName)
                    }
                    findNavController().navigate(R.id.action_teamCreate_to_teamComplete, bundle)
                }
            }
        }
    }

    /** 갤러리에서 선택한 이미지를 앱 내부 저장소에 저장. 반환: "file:team_<teamId>.jpg" (실패 시 "") */
    private fun saveTeamImageToFile(uri: Uri, teamId: String): String {
        val fileName = "team_$teamId.jpg"
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val file = File(requireContext().filesDir, fileName)
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
                "file:$fileName"
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** 시작일시/종료일시 파싱 (기본값이면 null) */
    private fun parseWorkDateTimes(startDateStr: String, startTimeStr: String, endDateStr: String, endTimeStr: String): Pair<Long?, Long?> {
        val defaultDate = "2026. 2. 23"
        if (startDateStr == defaultDate || endDateStr == defaultDate) return null to null
        val dateFmt = SimpleDateFormat("yyyy. M. d", Locale.KOREA)
        val timeFmt = SimpleDateFormat("a h:mm", Locale.KOREA)
        return try {
            val startDate = dateFmt.parse(startDateStr) ?: return null to null
            val startTime = timeFmt.parse(startTimeStr) ?: return null to null
            val endDate = dateFmt.parse(endDateStr) ?: return null to null
            val endTime = timeFmt.parse(endTimeStr) ?: return null to null
            val calStart = Calendar.getInstance().apply {
                time = startDate
                val t = Calendar.getInstance().apply { time = startTime }
                set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, t.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance().apply {
                time = endDate
                val t = Calendar.getInstance().apply { time = endTime }
                set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, t.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            calStart.timeInMillis to calEnd.timeInMillis
        } catch (e: Exception) {
            null to null
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

    private fun formatToIso(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    private fun parseIsoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(iso)?.time
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso)?.time
        } catch (_: Exception) { null }
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