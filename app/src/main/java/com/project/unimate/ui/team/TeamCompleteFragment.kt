package com.project.unimate.ui.team

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

        val inviteCodeFromArgs = arguments?.getString("inviteCode")?.trim()

        if (!inviteCodeFromArgs.isNullOrBlank()) {
            viewModel.inviteCode.value = inviteCodeFromArgs
        } else {
            viewModel.inviteCode.value = ""
            android.util.Log.e("TeamComplete", "초대코드를 전달받지 못했습니다.")
        }

        viewModel.inviteCode.observe(viewLifecycleOwner) { code ->
            binding.tvInviteCode.text = if (code.isNullOrBlank()) "코드 생성 오류" else code
        }

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvInviteCode.text?.toString()?.trim().orEmpty()
            if (code == "코드 생성 오류" || code.isBlank()) {
                Toast.makeText(context, "복사할 초대코드가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TeamCode", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "코드가 복사되었습니다!", Toast.LENGTH_SHORT).show()
        }

        binding.btnGoToTeam.setOnClickListener {
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
