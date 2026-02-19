package com.project.unimate.ui.team

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
                val isNotEmpty = input.isNotEmpty()
                binding.btnJoinConfirm.isEnabled = isNotEmpty
                binding.etJoinCode.isSelected = isNotEmpty
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnJoinConfirm.setOnClickListener {
            val code = binding.etJoinCode.text.toString().trim()
            if (code.isEmpty()) return@setOnClickListener

            binding.btnJoinConfirm.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val response = service.joinTeam(TeamJoinRequest(inviteCode = code))
                    if (response.isSuccessful) {
                        val teamName = response.body()?.team?.name ?: ""
                        val bundle = Bundle().apply {
                            putString("inviteCode", code)
                            putString("teamName", teamName)
                        }
                        findNavController().navigate(R.id.action_teamJoin_to_teamJoinedSuccess, bundle)
                    } else {
                        Toast.makeText(requireContext(), "유효하지 않은 초대코드입니다.", Toast.LENGTH_SHORT).show()
                        binding.btnJoinConfirm.isEnabled = true
                    }
                } catch (e: Exception) {
                    // API 실패 시 기존 동작 (바로 이동)
                    val bundle = Bundle().apply { putString("inviteCode", code) }
                    findNavController().navigate(R.id.action_teamJoin_to_teamJoinedSuccess, bundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
