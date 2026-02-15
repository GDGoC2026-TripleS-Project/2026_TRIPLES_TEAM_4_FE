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
import androidx.navigation.fragment.findNavController
import com.project.unimate.R

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
