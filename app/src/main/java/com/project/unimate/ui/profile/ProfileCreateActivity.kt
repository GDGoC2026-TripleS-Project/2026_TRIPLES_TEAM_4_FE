package com.project.unimate.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.unimate.databinding.ActivityProfileCreateBinding
import com.project.unimate.R // 중요: 본인 프로젝트의 R을 임포트해야 합니다.

class ProfileCreateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileCreateBinding
    private lateinit var schoolAdapter: SchoolAdapter

    private var selectedImageUri: Uri? = null

    // 요청하신 대로 3개 학교만 리스트에 넣었습니다.
    private val allSchools = listOf("성공회대학교", "성신여자대학교", "서울여자대학교")

    private val pickImageLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            if (imageUri != null) {
                selectedImageUri = imageUri
                binding.ivProfilePlaceholder.apply {
                    setImageURI(imageUri)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupSearchLogic()

        binding.cvProfile.setOnClickListener {
            openGallery()
        }

        binding.btnProfileRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val school = binding.etSchoolSearch.text.toString()

            if (name.isNotEmpty() && school.isNotEmpty()) {
                // TODO: 서버 전송 로직
            }
        }
    }

    private fun setupRecyclerView() {
        schoolAdapter = SchoolAdapter { selectedSchool ->
            binding.etSchoolSearch.setText(selectedSchool)
            hideSchoolList() // 아이템 클릭 시 리스트 숨기고 배경 원복
            binding.etSchoolSearch.clearFocus()
        }

        binding.rvSchoolList.apply {
            adapter = schoolAdapter
            layoutManager = LinearLayoutManager(this@ProfileCreateActivity)
        }
    }

    private fun setupSearchLogic() {
        binding.etSchoolSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()

                if (query.isNotEmpty()) {
                    val filteredList = allSchools.filter { it.contains(query) }
                    if (filteredList.isNotEmpty()) {
                        schoolAdapter.submitList(filteredList)
                        showSchoolList() // 리스트 보여주며 배경 변경
                    } else {
                        hideSchoolList()
                    }
                } else {
                    hideSchoolList()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 리스트를 보여주고 입력창 하단을 각지게 만듦 (일체형 디자인)
    private fun showSchoolList() {
        binding.etSchoolSearch.setBackgroundResource(R.drawable.shape_edittext_opened)
        binding.rvSchoolList.visibility = View.VISIBLE
    }

    // 리스트를 숨기고 입력창을 다시 둥글게 만듦
    private fun hideSchoolList() {
        binding.etSchoolSearch.setBackgroundResource(R.drawable.shape_edittext_default)
        binding.rvSchoolList.visibility = View.GONE
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }
}