package com.project.unimate.ui.team

// 역할: 팀 생성 완료. 초대코드 표시·복사·ViewModel 동기화. TeamViewModel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.project.unimate.R
import com.project.unimate.databinding.FragmentTeamCompleteBinding
import com.project.unimate.viewmodel.TeamViewModel

class TeamCompleteFragment : Fragment(R.layout.fragment_team_complete) {
    private var _binding: FragmentTeamCompleteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TeamViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentTeamCompleteBinding.bind(view)

        val inviteCodeFromArgs = arguments?.getString("inviteCode")?.trim().orEmpty()
        if (inviteCodeFromArgs.isNotBlank()) {
            viewModel.inviteCode.value = inviteCodeFromArgs
        }

        viewModel.inviteCode.observe(viewLifecycleOwner) { code ->
            binding.tvInviteCode.text = code.orEmpty()
        }

        // 2. 초대코드 복사 (버튼 + 코드 옆 복사 아이콘 동일 동작)
        fun copyInviteCode() {
            val code = binding.tvInviteCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                Toast.makeText(context, "복사할 초대코드가 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TeamCode", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "코드가 복사되었습니다!", Toast.LENGTH_SHORT).show()
        }
        binding.btnCopyCode.setOnClickListener { copyInviteCode() }
        binding.ivCopyIcon.setOnClickListener { copyInviteCode() }

        binding.btnGoToTeam.setOnClickListener {
            // 팀 생성 완료 후 해당 팀 스페이스로 바로 이동
            val teamId = arguments?.getLong("teamId", -1L) ?: -1L
            if (teamId >= 0) {
                val bundle = Bundle().apply { putString("teamId", teamId.toString()) }
                findNavController().navigate(
                    R.id.teamSpaceFragment,
                    bundle,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.team_nav) { inclusive = true }
                    }
                )
            } else {
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.nav_graph) { inclusive = true }
                    }
                )
            }
        }
    }
}
