package com.project.unimate.ui.team

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamJoinBinding
import com.project.unimate.model.JoinTeamRequest
import com.project.unimate.model.JoinTeamResponse
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.TeamService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                val input = s.toString().trim().uppercase()

                val isValidLength = input.length == 6
                binding.btnJoinConfirm.isEnabled = isValidLength
                binding.etJoinCode.isSelected = input.isNotEmpty()
            }

            override fun afterTextChanged(s: Editable?) {
                val str = s.toString()
                if (str != str.uppercase()) {
                    binding.etJoinCode.setText(str.uppercase())
                    binding.etJoinCode.setSelection(binding.etJoinCode.length())
                }
            }
        })

        binding.btnJoinConfirm.setOnClickListener {
            val code = binding.etJoinCode.text.toString().trim().uppercase()
            performJoinTeam(code)
        }
    }

    private fun performJoinTeam(code: String) {
        val service = RetrofitClient.getInstance(requireContext()).create(TeamService::class.java)
        val request = JoinTeamRequest(code) // { "inviteCode": "ABC123" }

        // 반환 타입을 Call<Unit>에서 Call<JoinTeamResponse>로 변경
        service.joinTeam(request).enqueue(object : Callback<JoinTeamResponse> {
            override fun onResponse(call: Call<JoinTeamResponse>, response: Response<JoinTeamResponse>) {
                if (response.isSuccessful) {
                    val teamData = response.body()
                    Log.d("TeamJoin", "참여 성공: ${teamData?.team?.name}")

                    // 성공 시 다음 화면으로 이동 (필요 시 팀 정보를 전달할 수 있음)
                    findNavController().navigate(R.id.action_teamJoin_to_teamJoinedSuccess)
                } else {
                    // 에러 바디에서 구체적인 에러 코드 확인
                    val errorBody = response.errorBody()?.string()
                    val message = when {
                        errorBody?.contains("INVITE_CODE_EXPIRED") == true -> "만료된 초대코드입니다. (10분 경과)"
                        errorBody?.contains("INVITE_CODE_INVALID") == true -> "유효하지 않은 초대코드입니다."
                        else -> "팀 참여에 실패했습니다. 코드를 확인해주세요."
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JoinTeamResponse>, t: Throwable) {
                Log.e("TeamJoin", "Network Error: ${t.message}")
                Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}