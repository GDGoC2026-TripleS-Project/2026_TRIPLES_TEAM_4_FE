package com.project.unimate.ui.mypage

// 역할: 초대코드로 팀 가입. TeamService joinTeam

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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamJoinRequest
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.launch

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

        joinButton.backgroundTintList = null
        joinButton.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_join_button)
        joinButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray06))

        joinButton.setOnClickListener {
            if (joinCompleted) return@setOnClickListener
            val code = codeInput.text.toString().trim()
            if (code.length != 6) {
                Toast.makeText(requireContext(), "초대코드는 숫자 6자리입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            joinButton.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val response = service.joinTeam(TeamJoinRequest(inviteCode = code))
                    if (response.isSuccessful) {
                        val joined = response.body()
                        val teamName = joined?.team?.name ?: "새 팀플"
                        val teamColorHex = joined?.team?.colorHex ?: "#EDF3D7"
                        val newTeam = Team(
                            id = joined?.team?.id?.toString() ?: "joined_${System.currentTimeMillis()}",
                            name = teamName,
                            colorHex = teamColorHex,
                            imageResName = "",
                            isCompleted = false,
                            memberCount = joined?.memberCount ?: 4,
                            deadlineDays = 7,
                            intro = joined?.team?.description ?: ""
                        )
                        DummyRepository.addTeam(newTeam)
                        onJoinSuccess(joinButton, view)
                    } else {
                        val errorCode = try {
                            val errorBody = response.errorBody()?.string() ?: ""
                            org.json.JSONObject(errorBody).optString("code", "")
                        } catch (_: Exception) { "" }
                        val message = when (errorCode) {
                            "INVITE_CODE_INVALID" -> "유효하지 않은 초대코드입니다."
                            "INVITE_CODE_EXPIRED" -> "초대코드가 만료되었습니다."
                            "ALREADY_TEAM_MEMBER" -> "이미 해당 팀의 멤버입니다."
                            else -> "초대코드 확인에 실패했습니다. (${response.code()})"
                        }
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        joinButton.isEnabled = true
                    }
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    joinButton.isEnabled = true
                }
            }
        }
    }

    private fun onJoinSuccess(joinButton: Button, view: View) {
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

    private fun showCustomToast() {
        val toast = Toast(requireContext())
        toast.duration = Toast.LENGTH_LONG
        val dm = resources.displayMetrics

        val marginH = (10 * dm.density).toInt()
        val widthPx = (dm.widthPixels - 2 * marginH).coerceAtLeast(0)
        val bottomMargin = (100 * dm.density).toInt()

        toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, bottomMargin)

        val layout = layoutInflater.inflate(R.layout.toast_team_join_complete, null)
        layout.elevation = 8f


        layout.minimumWidth = widthPx

        val heightPx = (62 * dm.density).toInt()
        layout.layoutParams = android.widget.FrameLayout.LayoutParams(widthPx, heightPx)

        val tv = layout.findViewById<TextView>(R.id.toastMessage)
        tv.text = getString(R.string.team_join_complete_toast) + " 🎉"
        tv.gravity = Gravity.CENTER

        toast.view = layout
        toast.show()
    }
}
