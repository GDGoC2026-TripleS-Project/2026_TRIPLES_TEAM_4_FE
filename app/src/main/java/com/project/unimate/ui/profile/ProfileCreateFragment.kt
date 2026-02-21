package com.project.unimate.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.R
import com.project.unimate.auth.FcmRegistrar
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DeletedSeedTeamStore
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.data.repository.SeedTeamOverridesStore
import com.project.unimate.databinding.FragmentProfileCreateBinding
import com.project.unimate.network.Env
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamCreateRequest
import com.project.unimate.network.dto.TeamSummaryResponse
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileCreateFragment : Fragment(R.layout.fragment_profile_create) {

    private var _binding: FragmentProfileCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var schoolAdapter: SchoolAdapter
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var selectedUniversityId: Long? = null
    private val profileApi = ProfileApi()
    private var searchSeq = 0
    private var isSelectingSchool = false

    // 이미지 선택 결과 처리를 위한 런처
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                binding.ivProfilePlaceholder.apply {
                    setImageURI(imageUri)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = null
                }

                // 이미지 업로드
                profileApi.uploadProfileImage(requireContext(), imageUri) { url, err ->
                    requireActivity().runOnUiThread {
                        if (!url.isNullOrBlank()) {
                            uploadedImageUrl = url
                            Toast.makeText(requireContext(), "프로필 이미지가 업로드되었습니다.", Toast.LENGTH_SHORT).show()
                        } else if (!err.isNullOrBlank()) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileCreateBinding.bind(view)

        // 초기 설정 함수 호출
        setupRecyclerView()
        setupSearchLogic()

        // 1. 프로필 이미지 영역 클릭 리스너 (이미지 선택)
        binding.cvProfile.setOnClickListener {
            openGallery()
        }

        // 2. 등록 버튼 클릭 리스너 (팀 설정 화면으로 이동)
        binding.btnProfileRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val school = binding.etSchoolSearch.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (school.isEmpty() || selectedUniversityId == null) {
                Toast.makeText(requireContext(), "학교를 검색해서 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            profileApi.upsertProfile(
                context = requireContext(),
                nickname = name,
                universityId = selectedUniversityId!!,
                profileImageUrl = uploadedImageUrl
            ) { ok, err ->
                requireActivity().runOnUiThread {
                    if (ok) {
                        DummyRepository.setCurrentUserName(name)
                        com.project.unimate.data.repository.NicknameStore.save(requireContext(), name)
                        FcmRegistrar.registerIfPossible(requireContext(), Env.BASE_URL)
                        seedDummyTeamsAndNavigate()
                    } else {
                        Toast.makeText(requireContext(), err ?: "프로필 등록 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 3. 학교 검색 버튼 클릭
        binding.btnSchoolSearch.setOnClickListener {
            val query = binding.etSchoolSearch.text.toString().trim()
            runSchoolSearch(query, forceToast = true)
        }
    }

    /** 프로필 등록 성공 직후: 서버에 더미 팀 7개 생성 → getMyTeams로 동기화 → 팀 설정 화면으로 이동 */
    private fun seedDummyTeamsAndNavigate() {
        lifecycleScope.launch {
            val ctx = context ?: return@launch
            try {
                val service = RetrofitClient.create<TeamService>(ctx)
                for (team in DummyRepository.getSeedTeams()) {
                    val startAt = team.workStartMillis?.let { formatToIso(it) } ?: formatToIso(System.currentTimeMillis())
                    val endAt = team.workEndMillis?.let { formatToIso(it) } ?: formatToIso(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)
                    val req = TeamCreateRequest(
                        name = team.name,
                        description = team.intro.ifBlank { null },
                        color = team.colorHex,
                        startAt = startAt,
                        endAt = endAt
                    )
                    val resp = service.createTeam(req)
                    if (!resp.isSuccessful) {
                        Log.w("ProfileCreate", "seed createTeam failed: ${team.name} code=${resp.code()}")
                    }
                }
                val myTeamsResp = service.getMyTeams()
                if (myTeamsResp.isSuccessful) {
                    val list = myTeamsResp.body()?.listOrEmpty() ?: emptyList()
                    val serverTeams = list.mapNotNull { r -> teamSummaryToTeam(r) }
                    val deletedNames = DeletedSeedTeamStore.getDeletedNames(ctx)
                    val merged = DummyRepository.mergeServerTeamsWithSeed(serverTeams, deletedNames)
                    val seedIds = DummyRepository.getSeedTeams().map { it.id }.toSet()
                    val withOverrides = SeedTeamOverridesStore.applyOverrides(ctx, merged, seedIds)
                    withContext(Dispatchers.Main) {
                        DummyRepository.replaceTeamsWithServerData(withOverrides)
                        DummyRepository.applyPersistedTeamImages(ctx)
                        DummyRepository.applyPersistedTeamNames(ctx)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileCreate", "seed or getMyTeams failed", e)
            }
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    findNavController().navigate(R.id.action_profileCreate_to_team_nav)
                }
            }
        }
    }

    private fun formatToIso(millis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date(millis))
    }

    private fun parseIsoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(iso)?.time
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso)?.time
        } catch (_: Exception) { null }
    }

    private fun teamSummaryToTeam(r: TeamSummaryResponse): Team? {
        val id = r.id ?: return null
        val completed = r.completed == true || r.isCompleted == true
        val endMillis = parseIsoToMillis(r.endAt)
        return Team(
            id = id.toString(),
            name = r.name ?: "",
            colorHex = r.colorHex ?: "#cccccc",
            imageResName = r.imageUrl?.takeIf { it.isNotBlank() } ?: "",
            isCompleted = completed,
            memberCount = (r.memberCount ?: 0).toInt(),
            deadlineDays = null,
            intro = r.description ?: "",
            workStartMillis = parseIsoToMillis(r.startAt),
            workEndMillis = endMillis,
            completedAtMillis = if (completed) endMillis else null
        )
    }

    /**
     * 학교 검색 리스트를 위한 리사이클러뷰 설정
     */
    private fun setupRecyclerView() {
        schoolAdapter = SchoolAdapter { selectedSchool ->
            isSelectingSchool = true

            searchSeq++

            selectedUniversityId = selectedSchool.id

            binding.etSchoolSearch.setText(selectedSchool.name)

            binding.rvSchoolList.visibility = View.GONE

            binding.etSchoolSearch.clearFocus()

            isSelectingSchool = false
        }

        binding.rvSchoolList.apply {
            adapter = schoolAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    /**
     * 학교 이름 검색 로직 설정
     */
    private fun setupSearchLogic() {
        binding.etSchoolSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isSelectingSchool) {
                    return
                }

                selectedUniversityId = null

                val query = s.toString().trim()
                runSchoolSearch(query, forceToast = false)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun runSchoolSearch(query: String, forceToast: Boolean) {
        if (query.isEmpty()) {
            binding.rvSchoolList.visibility = View.GONE
            if (forceToast) {
                Toast.makeText(requireContext(), "학교명을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val seq = ++searchSeq
        profileApi.searchUniversities(requireContext(), query, limit = 10) { list, err ->
            if (!isAdded || seq != searchSeq) return@searchUniversities
            requireActivity().runOnUiThread {
                if (list.isNotEmpty()) {
                    schoolAdapter.submitList(list)
                    binding.rvSchoolList.visibility = View.VISIBLE
                } else {
                    binding.rvSchoolList.visibility = View.GONE
                    if (forceToast) {
                        Toast.makeText(requireContext(), err ?: "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 갤러리 열기 함수
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 뷰 바인딩 메모리 누수 방지
        _binding = null
    }
}
