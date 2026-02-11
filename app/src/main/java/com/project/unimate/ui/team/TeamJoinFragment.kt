package com.project.unimate.ui.team

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamJoinBinding

class TeamJoinFragment : Fragment(R.layout.fragment_team_join) {

    // 1. 바인딩 변수 선언
    private var _binding: FragmentTeamJoinBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeamJoinBinding.bind(view)

        // 초기 상태: 버튼 비활성화
        binding.btnJoinConfirm.isEnabled = false

        // 실시간 텍스트 감지하여 버튼 활성화 상태 제어
        binding.etJoinCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim()
                val isNotEmpty = input.isNotEmpty()

                // 입력값이 있을 때만 버튼 활성화 및 배경 상태 변경
                binding.btnJoinConfirm.isEnabled = isNotEmpty
                binding.etJoinCode.isSelected = isNotEmpty
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // [추가된 부분] 참여하기 버튼 클릭 시 로직
        binding.btnJoinConfirm.setOnClickListener {
            val code = binding.etJoinCode.text.toString().trim()

            // 1. (나중에) 여기에 서버로 코드를 보내서 실제 존재하는 팀인지 확인하는 로직이 들어갑니다.

            // 2. 성공 화면(TeamJoinedSuccessFragment)으로 이동
            // action_teamJoin_to_teamJoinedSuccess ID가 nav_graph에 있는지 확인하세요!
            findNavController().navigate(R.id.action_teamJoin_to_teamJoinedSuccess)
        }
    }

    // View가 파괴될 때 바인딩 메모리 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}