package com.project.unimate.ui.team

// 역할: 팀 가입 성공. 초대코드 표시·복사·팀스페이스 이동

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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

    private var inviteCode: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeamJoinedSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 이전 화면(TeamCreateFragment)에서 넘겨준 초대 코드 받기
        // (Navigation의 Arguments나 Bundle을 사용한다고 가정)
        arguments?.let {
            inviteCode = it.getString("inviteCode", "CODE_ERROR") // 값이 없으면 기본값
        }

        // 2. 화면에 초대 코드 표시
        binding.tvInviteCode.text = inviteCode

        // 3. 초대 코드 복사 기능 (버튼 + 코드 옆 복사 아이콘 동일 동작)
        fun copyInviteCode() {
            val code = inviteCode.trim()
            if (code.isBlank()) {
                Toast.makeText(context, "복사할 초대코드가 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Invite Code", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "초대 코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }
        binding.btnCopyCode.setOnClickListener { copyInviteCode() }
        binding.ivCopyIcon.setOnClickListener { copyInviteCode() }

        // 4. 팀으로 이동하기 버튼 → 해당 팀 스페이스로 바로 이동
        binding.btnGoToTeam.setOnClickListener {
            val teamIdStr = arguments?.getString("teamId")
            if (!teamIdStr.isNullOrBlank()) {
                val bundle = Bundle().apply { putString("teamId", teamIdStr) }
                findNavController().navigate(
                    R.id.teamSpaceFragment,
                    bundle,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.team_nav) { inclusive = true }
                    }
                )
            } else {
                findNavController().navigate(R.id.action_success_to_home)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
