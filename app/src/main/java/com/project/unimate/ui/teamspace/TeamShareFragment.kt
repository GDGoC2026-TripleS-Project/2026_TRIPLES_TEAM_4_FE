package com.project.unimate.ui.teamspace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.launch

class TeamShareFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_team_share, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val inviteCodeView = view.findViewById<TextView>(R.id.tvShareInviteCode)
        val teamId = arguments?.getString("teamId")

        // 기존 초대코드 조회 (GET), 없거나 만료 시 재발급 (POST)
        val numericTeamId = teamId?.toLongOrNull()
        if (numericTeamId != null) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val getResp = service.getInviteCode(numericTeamId)
                    if (getResp.isSuccessful && !getResp.body()?.inviteCode.isNullOrBlank()) {
                        inviteCodeView.text = getResp.body()?.inviteCode
                    } else {
                        // 기존 코드 없거나 만료 → 재발급
                        val issueResp = service.issueInviteCode(numericTeamId)
                        if (issueResp.isSuccessful) {
                            val code = issueResp.body()?.inviteCode
                            if (!code.isNullOrBlank()) {
                                inviteCodeView.text = code
                            }
                        }
                    }
                } catch (_: Exception) {
                    // API 실패 시 기존 값 유지
                }
            }
        }

        fun copyInviteCode() {
            val code = inviteCodeView.text?.toString()?.trim().orEmpty()
            if (code.isBlank()) {
                Toast.makeText(requireContext(), "복사할 초대코드가 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("TeamInviteCode", code))
            Toast.makeText(requireContext(), "초대코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.teamShareBack).setOnClickListener {
            findNavController().popBackStack()
        }
        view.findViewById<View>(R.id.btnShareCopyCode).setOnClickListener { copyInviteCode() }
        view.findViewById<View>(R.id.ivShareCopyIcon).setOnClickListener { copyInviteCode() }
    }
}
