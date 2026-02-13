package com.project.unimate.ui.mypage

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository

class JoinTeamSpaceFragment : Fragment() {

    private var joinCompleted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_join_team_space, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cancelBtn = view.findViewById<TextView>(R.id.joinTeamSpaceCancel)
        val codeInput = view.findViewById<EditText>(R.id.joinTeamSpaceCodeInput)
        val joinButton = view.findViewById<Button>(R.id.joinTeamSpaceButton)

        cancelBtn.setOnClickListener { findNavController().popBackStack() }

        fun updateInputAndButtonStyle(hasText: Boolean) {
            codeInput.background = ContextCompat.getDrawable(
                requireContext(),
                if (hasText) R.drawable.bg_join_code_input_focused else R.drawable.bg_join_code_input
            )
            if (!joinCompleted) {
                joinButton.backgroundTintList = null
                joinButton.background = ContextCompat.getDrawable(
                    requireContext(),
                    if (hasText) R.drawable.bg_join_button_active else R.drawable.bg_join_button
                )
                joinButton.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (hasText) R.color.button_green_text else R.color.gray06
                    )
                )
            }
        }

        codeInput.setOnFocusChangeListener { _, hasFocus ->
            updateInputAndButtonStyle(!codeInput.text.isNullOrBlank())
        }
        codeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateInputAndButtonStyle(!s.isNullOrBlank())
            }
        })

        // 초기 상태
        joinButton.backgroundTintList = null
        joinButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_join_button)
        joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))

        joinButton.setOnClickListener {
            if (joinCompleted) return@setOnClickListener
            val newId = "joined_${System.currentTimeMillis()}"
            val newTeam = Team(
                id = newId,
                name = "새 팀플",
                colorHex = "#EDF3D7",
                imageResName = "",
                isCompleted = false,
                memberCount = 1,
                deadlineDays = 7
            )
            DummyRepository.addTeam(newTeam)

            joinCompleted = true
            joinButton.text = getString(R.string.join_complete)
            joinButton.backgroundTintList = null
            joinButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_join_button_complete)
            joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))

            view.postDelayed({
                findNavController().popBackStack()
                showCustomToast()
            }, 1200)
        }
    }

    private fun showCustomToast() {
        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_LONG
        val dm = resources.displayMetrics
        val marginH = (10 * dm.density).toInt()
        val bottomMargin = (100 * dm.density).toInt()
        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, bottomMargin)
        val layout = layoutInflater.inflate(R.layout.toast_team_join_complete, null)
        layout.elevation = 8f
        val widthPx = (dm.widthPixels - 2 * marginH).coerceAtLeast(0)
        val heightPx = (62 * dm.density).toInt()
        layout.layoutParams = android.widget.FrameLayout.LayoutParams(widthPx, heightPx)
        val tv = layout.findViewById<TextView>(R.id.toastMessage)
        tv.text = getString(R.string.team_join_complete_toast) + " 🎉"
        toast.view = layout
        toast.show()
    }
}
