package com.project.unimate.ui.team

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamAddBinding

class TeamAddFragment : Fragment(R.layout.fragment_team_add) {
    private var _binding: FragmentTeamAddBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeamAddBinding.bind(view)

        // 1. 팀스페이스 생성 버튼 클릭
        binding.btnCreateTeam.setOnClickListener {
            // team_nav.xml에 정의한 action ID를 사용하여 이동합니다.
            findNavController().navigate(R.id.action_teamAdd_to_teamCreate)
        }

        // 2. 팀스페이스 참여 버튼 클릭 (추후 구현)
        binding.btnJoinTeam.setOnClickListener {
            // 참여 관련 fragment가 준비되면 action 연결 후 아래 코드 활성화
            findNavController().navigate(R.id.action_teamAdd_to_teamJoin)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}