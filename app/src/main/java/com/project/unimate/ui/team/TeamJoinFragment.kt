package com.project.unimate.ui.team

// 역할: 초대코드 입력·팀 가입. TeamService joinTeam

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamJoinBinding
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamJoinRequest
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.launch

class TeamJoinFragment : Fragment(R.layout.fragment_team_join) {

    private var _binding: FragmentTeamJoinBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeamJoinBinding.bind(view)

        binding.btnJoinConfirm.isEnabled = false

        binding.etJoinCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim()
                val isValid = input.length == 6
                binding.btnJoinConfirm.isEnabled = isValid
                binding.etJoinCode.isSelected = input.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnJoinConfirm.setOnClickListener {
            val code = binding.etJoinCode.text.toString().trim()
            if (code.length != 6) {
                Toast.makeText(requireContext(), "초대코드는 숫자 6자리입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnJoinConfirm.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val response = service.joinTeam(TeamJoinRequest(inviteCode = code))
                    if (response.isSuccessful) {
                        val body = response.body()
                        val teamName = body?.team?.name ?: ""
                        val teamId = body?.team?.id
                        val bundle = Bundle().apply {
                            putString("inviteCode", code)
                            putString("teamName", teamName)
                            teamId?.let { putString("teamId", it.toString()) }
                        }
                        findNavController().navigate(R.id.action_teamJoin_to_teamJoinedSuccess, bundle)
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
                        binding.btnJoinConfirm.isEnabled = true
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    binding.btnJoinConfirm.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
