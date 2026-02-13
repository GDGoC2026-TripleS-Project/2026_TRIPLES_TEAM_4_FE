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
import androidx.navigation.fragment.findNavController
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
        val mypageJoinButton = root.findViewById<View>(R.id.mypageJoinButton)
        val mypageParticipatingContainer = root.findViewById<LinearLayout>(R.id.mypageParticipatingContainer)
        val mypageCompletedContainer = root.findViewById<LinearLayout>(R.id.mypageCompletedContainer)

        mypageUserName.text = "이주연"
        mypageUserEmail.text = "juyenLe24@naver.com"

        mypageJoinButton.setOnClickListener {
            findNavController().navigate(R.id.joinTeamSpaceFragment)
        }

        bindTeamLists(layoutInflater, mypageParticipatingContainer, mypageCompletedContainer)
        return root
    }

    override fun onResume() {
        super.onResume()
        view?.let { v ->
            val participating = v.findViewById<LinearLayout>(R.id.mypageParticipatingContainer)
            val completed = v.findViewById<LinearLayout>(R.id.mypageCompletedContainer)
            if (participating != null && completed != null) {
                participating.removeAllViews()
                completed.removeAllViews()
                bindTeamLists(layoutInflater, participating, completed)
            }
        }
    }

    private fun bindTeamLists(
        inflater: LayoutInflater,
        mypageParticipatingContainer: LinearLayout,
        mypageCompletedContainer: LinearLayout
    ) {
        DummyRepository.getParticipatingTeamProjects().forEach { team ->
            val card = inflater.inflate(R.layout.item_mypage_team_card, mypageParticipatingContainer, false)
            card.isClickable = true
            card.isFocusable = true
            card.setOnClickListener {
                findNavController().navigate(R.id.teamSpaceFragment, Bundle().apply { putString("teamId", team.id) })
            }
            card.findViewById<TextView>(R.id.cardTeamName).text = team.name
            card.findViewById<TextView>(R.id.cardTeamStatus).text =
                getString(R.string.status_progress) + " · " + getString(R.string.status_members, team.memberCount)
            val deadlineTv = card.findViewById<TextView>(R.id.cardDeadline)
            val daysUntilEnd: Int? = if (team.workEndMillis != null) {
                val now = System.currentTimeMillis()
                val end = team.workEndMillis!!
                val dayMs = 24 * 60 * 60 * 1000L
                ((end - now) / dayMs).toInt()
            } else team.deadlineDays
            if (daysUntilEnd != null) {
                deadlineTv.visibility = View.VISIBLE
                deadlineTv.text = getString(R.string.deadline_d, daysUntilEnd.coerceAtLeast(0))
            } else {
                deadlineTv.visibility = View.GONE
            }
            card.findViewById<View>(R.id.cardEditButton).setOnClickListener {
                findNavController().navigate(R.id.editTeamSpaceFragment, Bundle().apply { putString("teamId", team.id) })
            }
            mypageParticipatingContainer.addView(card)
        }

        DummyRepository.getCompletedTeamProjects().forEach { team ->
            val item = inflater.inflate(R.layout.item_mypage_completed_team, mypageCompletedContainer, false)
            item.isClickable = true
            item.isFocusable = true
            item.setOnClickListener {
                findNavController().navigate(R.id.teamSpaceFragment, Bundle().apply { putString("teamId", team.id) })
            }
            val completedCard = item.findViewById<MaterialCardView>(R.id.completedTeamCard)
            completedCard.strokeColor = Color.parseColor(team.colorHex)
            val resId = resources.getIdentifier(team.imageResName, "drawable", requireContext().packageName)
            if (resId != 0) item.findViewById<ImageView>(R.id.completedTeamImage).setImageResource(resId)
            item.findViewById<TextView>(R.id.completedTeamName).text = team.name
            mypageCompletedContainer.addView(item)
        }
    }
}
