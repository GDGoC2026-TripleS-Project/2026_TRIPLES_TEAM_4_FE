package com.project.unimate.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository

class EditProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editProfileBack = view.findViewById<View>(R.id.editProfileBack)
        val etNameEdit = view.findViewById<EditText>(R.id.et_name_edit)
        val profileEditConfirm = view.findViewById<Button>(R.id.profile_edit_confirm)
        val profileEditCancel = view.findViewById<Button>(R.id.profile_edit_cancel)

        etNameEdit.setText(DummyRepository.getCurrentUserName())

        fun closeFragment() {
            findNavController().popBackStack()
        }
        editProfileBack.setOnClickListener { closeFragment() }
        profileEditCancel.setOnClickListener { closeFragment() }

        profileEditConfirm.setOnClickListener {
            val name = etNameEdit.text?.toString()?.trim() ?: return@setOnClickListener
            if (name.isNotEmpty()) {
                DummyRepository.setCurrentUserName(name)
                closeFragment()
            }
        }
    }
}