package com.project.unimate.ui.mypage

// 역할: 프로필 수정. 서버 프로필 로드·업로드. file:만 있을 때 API URL로 보완. UserService

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
import com.project.unimate.data.repository.ProfileImageStore
import com.project.unimate.network.RetrofitClient
import com.project.unimate.utils.ProfileImageLoader
import com.project.unimate.network.dto.ProfileUpsertRequest
import com.project.unimate.network.service.UserService
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class EditProfileFragment : Fragment() {

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val imageUri = result.data?.data ?: return@registerForActivityResult
        val iv = view?.findViewById<ImageView>(R.id.iv_profile_edit_placeholder)
        iv?.apply {
            setImageURI(imageUri)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.TRANSPARENT)
        }
        val saved = saveUserProfileImageToFile(imageUri)
        if (saved.isEmpty()) return@registerForActivityResult
        DummyRepository.setCurrentUserProfileImageResName(saved)
        ProfileImageStore.save(requireContext(), saved)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ctx = requireContext()
                val service = RetrofitClient.create<UserService>(ctx)
                val meResp = service.getMyInfo()
                val universityId = if (meResp.isSuccessful) (meResp.body()?.universityId ?: 0L) else 0L
                val name = withContext(Dispatchers.Main) { view?.findViewById<EditText>(R.id.et_name_edit)?.text?.toString()?.trim() ?: DummyRepository.getCurrentUserName() } ?: DummyRepository.getCurrentUserName()
                val f = File(ctx.filesDir, saved.removePrefix("file:"))
                if (!f.exists()) return@launch
                val part = MultipartBody.Part.createFormData("file", f.name, f.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                val uploadResp = service.uploadProfileImage(part)
                val newUrl = uploadResp.body()?.get("imageUrl")?.takeIf { it.isNotBlank() }
                    ?: uploadResp.body()?.get("url")?.takeIf { it.isNotBlank() }
                    ?: uploadResp.body()?.get("profileImageUrl")?.takeIf { it.isNotBlank() }
                if (!newUrl.isNullOrBlank()) {
                    service.upsertProfile(ProfileUpsertRequest(nickname = name, universityId = universityId, profileImageUrl = newUrl))
                    withContext(Dispatchers.Main) {
                        DummyRepository.setCurrentUserProfileImageResName(newUrl)
                        ProfileImageStore.save(ctx, newUrl)
                        iv?.let { ProfileImageLoader.load(it, newUrl, ctx) }
                    }
                }
            } catch (_: Exception) { }
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
        ProfileImageLoader.load(ivProfilePlaceholder, DummyRepository.getCurrentUserProfileImageResName(), requireContext())

        // 진입 시 서버에서 프로필 로드해 아이콘/이름 갱신 (file:만 있거나 비어 있을 때 보완)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<UserService>(requireContext())
                val meResp = service.getMyInfo()
                if (meResp.isSuccessful) {
                    val me = meResp.body() ?: return@launch
                    withContext(Dispatchers.Main) {
                        me.nickname?.takeIf { it.isNotBlank() }?.let { etNameEdit.setText(it) }
                        me.profileImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            ProfileImageLoader.load(ivProfilePlaceholder, url, requireContext())
                        }
                    }
                }
            } catch (_: Exception) { }
        }

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
                    val universityId: Long
                    var profileImageUrlToSend: String? = null
                    if (meResp.isSuccessful) {
                        val me = meResp.body()
                        universityId = me?.universityId ?: 0L
                        profileImageUrlToSend = me?.profileImageUrl
                    } else {
                        universityId = 0L
                    }
                    val resName = DummyRepository.getCurrentUserProfileImageResName()
                    // 이미 프로필 사진이 있는 상태에서 새 사진을 고른 경우: 로컬 file이면 반드시 업로드한 URL만 사용 (기존 URL로 덮어쓰지 않음)
                    if (resName.startsWith("file:")) {
                        val f = File(requireContext().filesDir, resName.removePrefix("file:"))
                        if (f.exists()) {
                            profileImageUrlToSend = null
                            val part = MultipartBody.Part.createFormData(
                                "file",
                                f.name,
                                f.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                            val uploadResp = service.uploadProfileImage(part)
                            if (uploadResp.isSuccessful) {
                                val body = uploadResp.body()
                                profileImageUrlToSend = body?.get("imageUrl")?.takeIf { it.isNotBlank() }
                                    ?: body?.get("url")?.takeIf { it.isNotBlank() }
                                    ?: body?.get("profileImageUrl")?.takeIf { it.isNotBlank() }
                            }
                        }
                    }
                    val upsertResp = service.upsertProfile(
                        ProfileUpsertRequest(nickname = name, universityId = universityId, profileImageUrl = profileImageUrlToSend)
                    )
                    withContext(Dispatchers.Main) {
                        if (upsertResp.isSuccessful) {
                            DummyRepository.setCurrentUserName(name)
                            com.project.unimate.data.repository.NicknameStore.save(requireContext(), name)
                            val fromUpsert = upsertResp.body()?.profileImageUrl?.takeIf { it.isNotBlank() }
                            val finalRef = if (resName.startsWith("file:")) {
                                profileImageUrlToSend?.takeIf { it.isNotBlank() } ?: resName
                            } else {
                                fromUpsert
                                    ?: profileImageUrlToSend?.takeIf { it.isNotBlank() }
                                    ?: resName.takeIf { it.isNotBlank() }
                                    ?: DummyRepository.getCurrentUserProfileImageResName()
                            }
                            val refToSave = finalRef.ifBlank { null }
                            if (refToSave != null) {
                                DummyRepository.setCurrentUserProfileImageResName(refToSave)
                                ProfileImageStore.save(requireContext(), refToSave)
                            }
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

    private fun saveUserProfileImageToFile(uri: Uri): String {
        val fileName = "user_profile_${System.currentTimeMillis()}.jpg"
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                val file = File(requireContext().filesDir, fileName)
                FileOutputStream(file).use { output -> input.copyTo(output) }
                "file:$fileName"
            } ?: ""
        } catch (e: Exception) { "" }
    }
}