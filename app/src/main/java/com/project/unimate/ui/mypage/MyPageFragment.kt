package com.project.unimate.ui.mypage

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.project.unimate.R
import com.project.unimate.auth.JwtStore
import com.project.unimate.model.MyPageResponse
import com.project.unimate.model.MyPageTeam
import com.project.unimate.network.MyPageService
import com.project.unimate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyPageFragment : Fragment() {

    private lateinit var mypageUserName: TextView
    private lateinit var mypageUserEmail: TextView
    private lateinit var mypageUserIcon: ImageView
    private lateinit var mypageParticipatingContainer: LinearLayout
    private lateinit var mypageCompletedContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_my_page, container, false)

        mypageUserName = root.findViewById(R.id.mypageUserName)
        mypageUserEmail = root.findViewById(R.id.mypageUserEmail)
        mypageUserIcon = root.findViewById(R.id.mypageUserIcon)
        mypageParticipatingContainer = root.findViewById(R.id.mypageParticipatingContainer)
        mypageCompletedContainer = root.findViewById(R.id.mypageCompletedContainer)

        val mypageProfileEdit = root.findViewById<View>(R.id.mypageProfileEdit)
        val mypageJoinButton = root.findViewById<View>(R.id.mypageJoinButton)
        val logoutButton = root.findViewById<View>(R.id.btnLogout)

        mypageProfileEdit.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_editProfile)
        }
        mypageJoinButton.setOnClickListener {
            findNavController().navigate(R.id.joinTeamSpaceFragment)
        }
        logoutButton.setOnClickListener { logout() }

        return root
    }

    override fun onResume() {
        super.onResume()
        fetchMyPageData()
    }


    private fun fetchMyPageData() {
        val service = RetrofitClient.getInstance(requireContext()).create(MyPageService::class.java)

        service.getMyPageSummary().enqueue(object : Callback<MyPageResponse> {
            override fun onResponse(call: Call<MyPageResponse>, response: Response<MyPageResponse>) {
                if (response.isSuccessful) {
                    val data = response.body()
                    if (data != null) {
                        updateUI(data)
                    }
                } else {
                    Log.e("MyPageFragment", "통신 실패: ${response.code()}")
                    if (response.code() == 403) {
                        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<MyPageResponse>, t: Throwable) {
                Log.e("MyPageFragment", "네트워크 오류: ${t.message}")
            }
        })
    }

    private fun updateUI(data: MyPageResponse) {
        mypageUserName.text = data.profile.nickname
        mypageUserEmail.text = data.profile.email
        mypageUserIcon.setImageResource(R.drawable.ic_user)


        mypageParticipatingContainer.removeAllViews()
        data.activeTeams.forEach { team ->
            val card = layoutInflater.inflate(R.layout.item_mypage_team_card, mypageParticipatingContainer, false)

            card.setOnClickListener {
                findNavController().navigate(R.id.teamSpaceFragment, Bundle().apply { putString("teamId", team.teamId.toString()) })
            }

            card.findViewById<TextView>(R.id.cardTeamName).text = team.name
            card.findViewById<TextView>(R.id.cardTeamStatus).text =
                getString(R.string.status_progress) + " · " + getString(R.string.status_members, team.memberCount)

            val deadlineTv = card.findViewById<TextView>(R.id.cardDeadline)
            if (!team.dday.isNullOrBlank()) {
                deadlineTv.visibility = View.VISIBLE
                deadlineTv.text = team.dday
            } else {
                deadlineTv.visibility = View.GONE
            }

            card.findViewById<View>(R.id.cardEditButton).setOnClickListener {
                findNavController().navigate(R.id.editTeamSpaceFragment, Bundle().apply { putString("teamId", team.teamId.toString()) })
            }
            mypageParticipatingContainer.addView(card)
        }


        mypageCompletedContainer.removeAllViews()
        data.completedTeams.forEach { team ->
            val item = layoutInflater.inflate(R.layout.item_mypage_completed_team, mypageCompletedContainer, false)

            item.setOnClickListener {
                findNavController().navigate(R.id.teamSpaceFragment, Bundle().apply { putString("teamId", team.teamId.toString()) })
            }

            val completedCard = item.findViewById<MaterialCardView>(R.id.completedTeamCard)
            val teamImage = item.findViewById<ImageView>(R.id.completedTeamImage)
            val teamLetter = item.findViewById<TextView>(R.id.completedTeamLetter)

            try {
                val colorCode = Color.parseColor(team.colorHex ?: "#CCCCCC")
                completedCard.strokeColor = colorCode
                teamImage.setImageDrawable(null)
                teamImage.setBackgroundColor(colorCode)
                teamLetter.text = team.name.firstOrNull()?.toString() ?: ""
                teamLetter.visibility = View.VISIBLE
            } catch (e: Exception) {
                // 색상 파싱 실패 시  처리
            }

            item.findViewById<TextView>(R.id.completedTeamName).text = team.name
            mypageCompletedContainer.addView(item)
        }
    }

    private fun logout() {
        JwtStore.clear(requireContext())
        findNavController().navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .setLaunchSingleTop(true)
                .build()
        )
    }
}