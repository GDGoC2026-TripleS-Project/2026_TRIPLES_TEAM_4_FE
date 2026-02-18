package com.project.unimate.ui.mypage

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.ProfileUpsertRequest
import com.project.unimate.network.service.UserService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class EditProfileFragment : Fragment() {

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data ?: return@registerForActivityResult
            view?.findViewById<ImageView>(R.id.iv_profile_edit_placeholder)?.apply {
                setImageURI(imageUri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.TRANSPARENT)
            }
            val saved = saveUserProfileImageToFile(imageUri)
            if (saved.isNotEmpty()) DummyRepository.setCurrentUserProfileImageResName(saved)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editProfileBack = view.findViewById<View>(R.id.editProfileBack)
        val ivProfilePlaceholder = view.findViewById<ImageView>(R.id.iv_profile_edit_placeholder)
        val ivEditProfile = view.findViewById<ImageView>(R.id.iv_edit_profile)
        val etNameEdit = view.findViewById<EditText>(R.id.et_name_edit)
        val profileEditConfirm = view.findViewById<Button>(R.id.profile_edit_confirm)
        val profileEditCancel = view.findViewById<Button>(R.id.profile_edit_cancel)

        etNameEdit.setText(DummyRepository.getCurrentUserName())
        applyUserProfileImage(ivProfilePlaceholder, DummyRepository.getCurrentUserProfileImageResName())

        fun openGallery() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }
        ivEditProfile.setOnClickListener { openGallery() }
        ivProfilePlaceholder.setOnClickListener { openGallery() }

        fun closeFragment() {
            findNavController().popBackStack()
        }
        editProfileBack.setOnClickListener { closeFragment() }
        profileEditCancel.setOnClickListener { closeFragment() }

        profileEditConfirm.setOnClickListener {
            val name = etNameEdit.text?.toString()?.trim() ?: return@setOnClickListener
            if (name.isEmpty()) return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<UserService>(requireContext())
                    val meResp = service.getMyInfo()
                    if (!meResp.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "프로필을 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    val me = meResp.body() ?: return@launch
                    val universityId = me.universityId ?: 0L
                    val profileImageUrl = me.profileImageUrl
                    val upsertResp = service.upsertProfile(
                        ProfileUpsertRequest(nickname = name, universityId = universityId, profileImageUrl = profileImageUrl)
                    )
                    withContext(Dispatchers.Main) {
                        if (upsertResp.isSuccessful) {
                            DummyRepository.setCurrentUserName(name)
                            closeFragment()
                        } else {
                            Toast.makeText(requireContext(), "저장에 실패했습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), e.message ?: "오류", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun applyUserProfileImage(imageView: ImageView, imageResName: String) {
        when {
            imageResName.startsWith("file:") -> {
                val file = File(requireContext().filesDir, imageResName.removePrefix("file:"))
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.let {
                        imageView.setImageBitmap(it)
                        imageView.setBackgroundColor(Color.TRANSPARENT)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                }
            }
            imageResName.isNotBlank() -> {
                val resId = resources.getIdentifier(imageResName, "drawable", requireContext().packageName)
                if (resId != 0) {
                    imageView.setImageResource(resId)
                    imageView.setBackgroundColor(Color.TRANSPARENT)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
        }
    }

    private fun saveUserProfileImageToFile(uri: Uri): String {
        val fileName = "user_profile.jpg"
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val file = File(requireContext().filesDir, fileName)
                FileOutputStream(file).use { output -> input.copyTo(output) }
                "file:$fileName"
            } ?: ""
        } catch (e: Exception) { "" }
    }
}