package com.project.unimate.ui.team

// 역할: 팀 생성/참여 진입. 생성→TeamCreate, 참여→TeamJoin

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

        binding.btnCreateTeam.setOnClickListener {
            findNavController().navigate(R.id.action_teamAdd_to_teamCreate)
        }

        binding.btnJoinTeam.setOnClickListener {
            findNavController().navigate(R.id.action_teamAdd_to_teamJoin)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}