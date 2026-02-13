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

        // 1. 전달받은 초대코드 표시 + ViewModel에 동기화
        val inviteCodeFromArgs = arguments?.getString("inviteCode")?.trim().orEmpty()
        if (inviteCodeFromArgs.isNotBlank()) {
            viewModel.inviteCode.value = inviteCodeFromArgs
        }

        viewModel.inviteCode.observe(viewLifecycleOwner) { code ->
            binding.tvInviteCode.text = code.orEmpty()
        }

        // 2. 초대코드 복사 버튼
        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvInviteCode.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                Toast.makeText(context, "복사할 초대코드가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TeamCode", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "코드가 복사되었습니다!", Toast.LENGTH_SHORT).show()
        }

        binding.btnGoToTeam.setOnClickListener {
            // navigate의 두 번째 인자로 navOptions 블록을 확실하게 전달합니다.
            findNavController().navigate(
                R.id.homeFragment,
                null, // Bundle (전달할 데이터가 없으므로 null)
                androidx.navigation.navOptions { // 빌더를 직접 명시
                    popUpTo(R.id.nav_graph) {
                        inclusive = true
                    }
                }
            )
        }
    }
}
