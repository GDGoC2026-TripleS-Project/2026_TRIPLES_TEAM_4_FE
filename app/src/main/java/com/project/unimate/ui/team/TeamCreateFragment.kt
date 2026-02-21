package com.project.unimate.ui.team

// 역할: 팀 생성. 색상(API 사용 중 갱신)·날짜/시간·이미지. TeamService. 초대코드는 서버 발급만 사용

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

    private val colorButtons by lazy {
        with(binding) {
            listOf(
                btnColorYellow, btnColorBeige, btnColorPeriwinkle,
                btnColorLavender, btnColorMagenta, btnColorPinkLight,
                btnColorCoral, btnColorCoralLight, btnColorMint, btnColorAqua
            )
        }
    }

    /** 버튼 id → hex (로컬 저장·UI) */
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

    /** 버튼 id → 서버 color 코드 (C01~C10) */
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


    /** 사용 중인 색상 코드(C01~C10). GET colors/available 또는 팀 목록으로 갱신 */
    private var usedColorCodes: Set<String> = emptySet()

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

        setupImagePicker()
        setupDateAndTimeListeners()
        setInitialDateAndTimeToNow()
        setupColorListeners()
        setupCompleteButton()

        loadAvailableColors()
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
        binding.tvStartDate.setOnClickListener { showStartDatePicker() }
        binding.tvStartTime.setOnClickListener { showStartTimePicker() }
        binding.tvEndDate.setOnClickListener { showEndDatePicker() }
        binding.tvEndTime.setOnClickListener { showEndTimePicker() }
    }

    /** 시작/종료 날짜·시간을 현재 시각으로 초기화 */
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

    /** 시작일 선택 시 종료일이 더 이전이면 종료일을 시작일로 맞춤 */
    private fun showStartDatePicker() {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val datePickerDialog = DatePickerDialog(
            contextWrapper,
            { _, year, month, day ->
                binding.tvStartDate.text = "${year}. ${month + 1}. ${day}"
                val startDateStr = binding.tvStartDate.text.toString()
                val endDateStr = binding.tvEndDate.text.toString()
                val (workStart, workEnd) = parseWorkDateTimes(startDateStr, "오전 12:00", endDateStr, "오전 12:00")
                if (workStart != null && workEnd != null && workStart > workEnd) {
                    binding.tvEndDate.text = startDateStr
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        styleDatePicker(datePickerDialog)
        datePickerDialog.show()
    }

    /** 종료일 선택 시 시작일이 더 이후면 시작일을 종료일로 맞춤 */
    private fun showEndDatePicker() {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val datePickerDialog = DatePickerDialog(
            contextWrapper,
            { _, year, month, day ->
                binding.tvEndDate.text = "${year}. ${month + 1}. ${day}"
                val startDateStr = binding.tvStartDate.text.toString()
                val endDateStr = binding.tvEndDate.text.toString()
                val (workStart, workEnd) = parseWorkDateTimes(startDateStr, "오전 12:00", endDateStr, "오전 12:00")
                if (workStart != null && workEnd != null && workEnd < workStart) {
                    binding.tvStartDate.text = endDateStr
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        styleDatePicker(datePickerDialog)
        datePickerDialog.show()
    }

    /** 같은 날일 때 시작 시간이 종료보다 늦으면 종료 시간을 시작과 맞춤 */
    private fun showStartTimePicker() {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val timePickerDialog = TimePickerDialog(
            contextWrapper,
            { _, h, m ->
                val amPm = if (h < 12) "오전" else "오후"
                val hour = if (h % 12 == 0) 12 else h % 12
                val timeStr = "$amPm ${hour}:${String.format("%02d", m)}"
                binding.tvStartTime.text = timeStr
                val startDateStr = binding.tvStartDate.text.toString()
                val endDateStr = binding.tvEndDate.text.toString()
                if (startDateStr == endDateStr) {
                    val (workStart, workEnd) = parseWorkDateTimes(startDateStr, timeStr, endDateStr, binding.tvEndTime.text.toString())
                    if (workStart != null && workEnd != null && workStart > workEnd) {
                        binding.tvEndTime.text = timeStr
                    }
                }
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
        styleTimePicker(timePickerDialog)
        timePickerDialog.show()
    }

    /** 같은 날일 때 종료 시간이 시작보다 이전이면 시작 시간을 종료와 맞춤 */
    private fun showEndTimePicker() {
        val cal = Calendar.getInstance()
        val contextWrapper = ContextThemeWrapper(requireContext(), R.style.MyDatePickerDialogTheme)
        val timePickerDialog = TimePickerDialog(
            contextWrapper,
            { _, h, m ->
                val amPm = if (h < 12) "오전" else "오후"
                val hour = if (h % 12 == 0) 12 else h % 12
                val timeStr = "$amPm ${hour}:${String.format("%02d", m)}"
                binding.tvEndTime.text = timeStr
                val startDateStr = binding.tvStartDate.text.toString()
                val endDateStr = binding.tvEndDate.text.toString()
                if (startDateStr == endDateStr) {
                    val (workStart, workEnd) = parseWorkDateTimes(startDateStr, binding.tvStartTime.text.toString(), endDateStr, timeStr)
                    if (workStart != null && workEnd != null && workEnd < workStart) {
                        binding.tvStartTime.text = timeStr
                    }
                }
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

    /** GET colors/available 우선, 실패 시 팀 목록으로 사용 중 색상 갱신 */
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
                    val teams = resp.body()?.listOrEmpty() ?: emptyList()
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

    private fun setupCompleteButton() {
        binding.btnCompleteCreate.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()

            if (teamName.isEmpty()) {
                Toast.makeText(requireContext(), "팀 명을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val startDateStr = binding.tvStartDate.text.toString()
            val endDateStr = binding.tvEndDate.text.toString()
            val defaultDate = "2026. 2. 23"

            if (startDateStr != defaultDate && endDateStr != defaultDate) {
                val sdf = java.text.SimpleDateFormat("yyyy. MM. dd", java.util.Locale.KOREA)
                try {
                    val startDate = sdf.parse(startDateStr)
                    val endDate = sdf.parse(endDateStr)

                    if (startDate != null && endDate != null && startDate.after(endDate)) {
                        Toast.makeText(requireContext(), "시작일은 종료일보다 빨라야 합니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val selectedBtn = colorButtons.find { it.tag == "SELECTED" }
            if (selectedBtn == null) {
                Toast.makeText(requireContext(), "팀 컬러를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val colorCode = colorButtonToCode[selectedBtn.id] ?: "C03"
            val colorHex = colorButtonToHex[selectedBtn.id] ?: "#90A3ED"

            if (colorCode.uppercase() in usedColorCodes) {
                Toast.makeText(requireContext(), "이미 사용 중인 팀 색상입니다. 다른 색상을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 초대코드는 서버에서만 발급. 로컬 더미는 네비/폴백용
            val localTeamId = "created_${System.currentTimeMillis()}"
            val (workStart, workEnd) = parseWorkDateTimes(binding.tvStartDate.text.toString(), binding.tvStartTime.text.toString(), binding.tvEndDate.text.toString(), binding.tvEndTime.text.toString())
            val imageResName = selectedImageUri?.let { saveTeamImageToFile(it, localTeamId) } ?: ""
            val intro = binding.etTeamDesc.text?.toString()?.trim() ?: ""

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
                        Log.w("TeamCreate", "409: 색상 중복 - $colorCode")
                        usedColorCodes = usedColorCodes + colorCode.uppercase()
                        updateColorButtonStates()
                        binding.btnCompleteCreate.isEnabled = true
                        Toast.makeText(requireContext(), "이미 사용 중인 팀 색상입니다. 다른 색상을 선택해주세요.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (!apiResp.isSuccessful) {
                        Log.w("TeamCreate", "팀 생성 실패: ${apiResp.code()}")
                        binding.btnCompleteCreate.isEnabled = true
                        Toast.makeText(requireContext(), "팀 생성에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val createdTeamId = apiResp.body()?.id
                    Log.d("TeamCreate", "팀 생성 성공, teamId=$createdTeamId")
                    if (createdTeamId == null) {
                        Log.w("TeamCreate", "팀 ID가 null — 네비게이션 불가")
                        binding.btnCompleteCreate.isEnabled = true
                        Toast.makeText(requireContext(), "팀 생성 응답 오류입니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    if (imageResName.isNotBlank() && imageResName.startsWith("file:team_")) {
                        val oldFileName = imageResName.removePrefix("file:")
                        val newFileName = "team_$createdTeamId.jpg"
                        withContext(Dispatchers.IO) {
                            val dir = requireContext().filesDir
                            val src = java.io.File(dir, oldFileName)
                            val dst = java.io.File(dir, newFileName)
                            if (src.exists()) src.copyTo(dst, overwrite = true)
                        }
                    }
                    try {
                        val myTeamsResp = service.getMyTeams()
                        if (myTeamsResp.isSuccessful) {
                            val deletedUserIds = DeletedUserTeamStore.getDeletedIds(requireContext())
                            val list = myTeamsResp.body()?.listOrEmpty()?.filter { r -> r.id?.toString() !in deletedUserIds } ?: emptyList()
                            val createdIdStr = createdTeamId.toString()
                            val serverTeams = list.mapNotNull { r ->
                                val id = r.id ?: return@mapNotNull null
                                val completed = r.completed == true || r.isCompleted == true
                                val endMillis = parseIsoToMillis(r.endAt)
                                val teamImageResName = if (id.toString() == createdIdStr && imageResName.isNotBlank() && imageResName.startsWith("file:team_"))
                                    "file:team_$createdTeamId.jpg" else ""
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
                                if (imageResName.isNotBlank() && imageResName.startsWith("file:team_")) {
                                    TeamImageStore.save(requireContext(), createdIdStr, "file:team_$createdTeamId.jpg")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TeamCreate", "팀 목록 갱신 실패: ${e.message}")
                    }
                    val inviteResp = try {
                        service.issueInviteCode(createdTeamId)
                    } catch (e: Exception) {
                        Log.e("TeamCreate", "초대코드 발급 예외: ${e.message}")
                        null
                    }
                    val serverInviteCode = inviteResp?.body()?.inviteCode.orEmpty()
                    val serverExpiresAt = inviteResp?.body()?.expiresAt.orEmpty()
                    Log.d("TeamCreate", "초대코드 발급: code=$serverInviteCode, expires=$serverExpiresAt")
                    if (isAdded) {
                        val bundle = Bundle().apply {
                            putString("inviteCode", serverInviteCode)
                            putString("teamName", teamName)
                            putLong("teamId", createdTeamId)
                            putString("expiresAt", serverExpiresAt)
                        }
                        findNavController().navigate(R.id.action_teamCreate_to_teamComplete, bundle)
                    }
                } catch (e: Exception) {
                    Log.e("TeamCreate", "팀 생성 예외: ${e.message}")
                    if (isAdded) {
                        binding.btnCompleteCreate.isEnabled = true
                        Toast.makeText(requireContext(), "오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    }
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


    /** 시작/종료 일시 유효성 검사. 잘못되면 다이얼로그 후 false */
    private fun isValidDateRange(): Boolean {
        val startDateStr = binding.tvStartDate.text.toString()
        val startTimeStr = binding.tvStartTime.text.toString()
        val endDateStr = binding.tvEndDate.text.toString()
        val endTimeStr = binding.tvEndTime.text.toString()

        if (startDateStr == "2026. 2. 23" || endDateStr == "2026. 2. 23") {
            Toast.makeText(context, "시작 및 종료 날짜를 모두 설정해주세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        val dateFormat = SimpleDateFormat("yyyy. M. d a h:mm", Locale.KOREA)

        try {
            val startFullDate = dateFormat.parse("$startDateStr $startTimeStr")
            val endFullDate = dateFormat.parse("$endDateStr $endTimeStr")

            if (startFullDate != null && endFullDate != null) {
                if (startFullDate.after(endFullDate)) {
                    showErrorDialog("종료 일시가 시작 일시보다 빠릅니다.\n날짜와 시간을 다시 확인해주세요.")
                    return false
                }
                if (startFullDate == endFullDate) {
                    showErrorDialog("시작 일시와 종료 일시가 같습니다.\n다시 확인해주세요.")
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }

        return true
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}