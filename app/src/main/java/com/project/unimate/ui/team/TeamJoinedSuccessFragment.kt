package com.project.unimate.ui.team

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamJoinedSuccessBinding

class TeamJoinedSuccessFragment : Fragment() {

    private var _binding: FragmentTeamJoinedSuccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamJoinedSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 초대 코드 복사 기능
        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvInviteCode.text.toString()
            Toast.makeText(context, "초대 코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 2. 팀으로 이동하기 버튼 (프래그먼트 네비게이션 사용)
        binding.btnGoToTeam.setOnClickListener {
            // [수정] Intent 대신 Navigation 액션을 사용합니다.
            // action_success_to_home 액션은 nav_graph 혹은 team_nav에 정의되어 있어야 합니다.
            findNavController().navigate(R.id.action_success_to_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}