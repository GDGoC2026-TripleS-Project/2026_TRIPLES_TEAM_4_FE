package com.project.unimate.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.R
import com.project.unimate.databinding.FragmentProfileCreateBinding

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
                        findNavController().navigate(R.id.action_profileCreate_to_team_nav)
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

    /**
     * 학교 검색 리스트를 위한 리사이클러뷰 설정
     */
    private fun setupRecyclerView() {
        schoolAdapter = SchoolAdapter { selectedSchool ->
            // 아이템 클릭 시 에디트텍스트에 값 입력 후 리스트 숨김
            isSelectingSchool = true
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
                val query = s.toString().trim()
                if (!isSelectingSchool) {
                    selectedUniversityId = null
                }

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
