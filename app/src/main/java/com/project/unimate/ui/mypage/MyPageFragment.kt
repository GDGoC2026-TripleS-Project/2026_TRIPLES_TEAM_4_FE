package com.project.unimate.ui.mypage

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_my_page, container, false)

        val mypageUserName = root.findViewById<TextView>(R.id.mypageUserName)
        val mypageUserEmail = root.findViewById<TextView>(R.id.mypageUserEmail)
        val mypageParticipatingContainer = root.findViewById<LinearLayout>(R.id.mypageParticipatingContainer)
        val mypageCompletedContainer = root.findViewById<LinearLayout>(R.id.mypageCompletedContainer)

        mypageUserName.text = "이주연"
        mypageUserEmail.text = "juyenLe24@naver.com"

        DummyRepository.getParticipatingTeamProjects().forEach { team ->
            val card = inflater.inflate(R.layout.item_mypage_team_card, mypageParticipatingContainer, false)
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener { /* 버튼으로만, 기능 없음 */ }
            card.findViewById<TextView>(R.id.cardTeamName).text = team.name
            card.findViewById<TextView>(R.id.cardTeamStatus).text =
                getString(R.string.status_progress) + " · " + getString(R.string.status_members, team.memberCount)
            val deadlineTv = card.findViewById<TextView>(R.id.cardDeadline)
            if (team.deadlineDays != null) {
                deadlineTv.visibility = View.VISIBLE
                deadlineTv.text = getString(R.string.deadline_d, team.deadlineDays)
            } else {
                deadlineTv.visibility = View.GONE
            }
            mypageParticipatingContainer.addView(card)
        }

        DummyRepository.getCompletedTeamProjects().forEach { team ->
            val item = inflater.inflate(R.layout.item_mypage_completed_team, mypageCompletedContainer, false)
            item.isClickable = true
            item.isFocusable = true
            item.setOnClickListener { /* 버튼으로만, 기능 없음 */ }
            val completedCard = item.findViewById<MaterialCardView>(R.id.completedTeamCard)
            completedCard.strokeColor = Color.parseColor(team.colorHex)
            val resId = resources.getIdentifier(team.imageResName, "drawable", requireContext().packageName)
            if (resId != 0) item.findViewById<ImageView>(R.id.completedTeamImage).setImageResource(resId)
            item.findViewById<TextView>(R.id.completedTeamName).text = team.name
            mypageCompletedContainer.addView(item)
        }

        return root
    }
}
