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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.project.unimate.R
import com.project.unimate.auth.JwtStore
import com.project.unimate.data.entity.Team
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.TeamSummaryResponse
import com.project.unimate.network.service.AuthService
import com.project.unimate.network.service.MyPageService
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_my_page, container, false)

        val mypageUserName = root.findViewById<TextView>(R.id.mypageUserName)
        val mypageUserEmail = root.findViewById<TextView>(R.id.mypageUserEmail)
        val mypageUserIcon = root.findViewById<ImageView>(R.id.mypageUserIcon)
        val mypageProfileEdit = root.findViewById<View>(R.id.mypageProfileEdit)
        val mypageJoinButton = root.findViewById<View>(R.id.mypageJoinButton)
        val logoutButton = root.findViewById<View>(R.id.btnLogout)
        val mypageParticipatingContainer = root.findViewById<LinearLayout>(R.id.mypageParticipatingContainer)
        val mypageCompletedContainer = root.findViewById<LinearLayout>(R.id.mypageCompletedContainer)

        mypageUserName.text = DummyRepository.getCurrentUserName()
        mypageUserEmail.text = "juyenLe24@naver.com"
        applyUserProfileImage(mypageUserIcon, DummyRepository.getCurrentUserProfileImageResName())

        mypageProfileEdit.setOnClickListener {
            findNavController().navigate(R.id.action_myPage_to_editProfile)
        }
        mypageJoinButton.setOnClickListener {
            findNavController().navigate(R.id.joinTeamSpaceFragment)
        }
        logoutButton.setOnClickListener { logout() }

        bindTeamLists(layoutInflater, mypageParticipatingContainer, mypageCompletedContainer)

        // API에서 마이페이지 정보 로드
        loadMyPageFromApi(mypageUserName, mypageUserEmail)

        return root
    }

    private fun loadMyPageFromApi(nameView: TextView, emailView: TextView) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<MyPageService>(ctx)
                val response = service.getSummary()
                if (response.isSuccessful) {
                    val summary = response.body() ?: return@launch
                    summary.profile?.let { profile ->
                        profile.nickname?.let { nameView.text = it }
                        profile.email?.let { emailView.text = it }
                    }
                }
            } catch (_: Exception) {
                // API 실패 시 DummyRepository 데이터 유지
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { v ->
            lifecycleScope.launch {
                syncTeamsFromServerIfNeeded()
                withContext(Dispatchers.Main) {
                    v.findViewById<TextView>(R.id.mypageUserName)?.text = DummyRepository.getCurrentUserName()
                    v.findViewById<ImageView>(R.id.mypageUserIcon)?.let { iv ->
                        applyUserProfileImage(iv, DummyRepository.getCurrentUserProfileImageResName())
                    }
                    val participating = v.findViewById<LinearLayout>(R.id.mypageParticipatingContainer)
                    val completed = v.findViewById<LinearLayout>(R.id.mypageCompletedContainer)
                    if (participating != null && completed != null) {
                        participating.removeAllViews()
                        completed.removeAllViews()
                        bindTeamLists(layoutInflater, participating, completed)
                    }
                }
            }
        }
    }

    private suspend fun syncTeamsFromServerIfNeeded() {
        if (JwtStore.load(requireContext()).isNullOrBlank()) return
        try {
            val service = RetrofitClient.create<TeamService>(requireContext())
            val resp = service.getMyTeams()
            if (resp.isSuccessful) {
                val serverTeams = resp.body()?.mapNotNull { teamSummaryToTeam(it) } ?: emptyList()
                val merged = DummyRepository.mergeServerTeamsWithSeed(serverTeams)
                withContext(Dispatchers.Main) { DummyRepository.replaceTeamsWithServerData(merged) }
            }
        } catch (_: Exception) { }
    }

    private fun teamSummaryToTeam(r: TeamSummaryResponse): Team? {
        val id = r.id ?: return null
        val completed = r.completed == true || r.isCompleted == true
        val endMillis = parseIsoToMillis(r.endAt)
        return Team(
            id = id.toString(),
            name = r.name ?: "",
            colorHex = r.colorHex ?: "#cccccc",
            imageResName = "",
            isCompleted = completed,
            memberCount = (r.memberCount ?: 0).toInt(),
            deadlineDays = null,
            intro = r.description ?: "",
            workStartMillis = parseIsoToMillis(r.startAt),
            workEndMillis = endMillis,
            completedAtMillis = if (completed) endMillis else null
        )
    }

    private fun parseIsoToMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(iso)?.time
                ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(iso)?.time
        } catch (_: Exception) { null }
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
            val teamImage = item.findViewById<ImageView>(R.id.completedTeamImage)
            val teamLetter = item.findViewById<TextView>(R.id.completedTeamLetter)
            when {
                team.imageResName.startsWith("file:") -> {
                    val file = java.io.File(requireContext().filesDir, team.imageResName.removePrefix("file:"))
                    if (file.exists()) {
                        android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.let { teamImage.setImageBitmap(it); teamImage.setBackgroundColor(android.graphics.Color.TRANSPARENT); teamLetter.visibility = View.GONE }
                    } else {
                        teamImage.setImageDrawable(null)
                        teamImage.setBackgroundColor(Color.parseColor(team.colorHex))
                        teamLetter.text = team.name.firstOrNull()?.toString() ?: ""
                        teamLetter.visibility = View.VISIBLE
                    }
                }
                team.imageResName.isNotBlank() -> {
                    val resId = resources.getIdentifier(team.imageResName, "drawable", requireContext().packageName)
                    if (resId != 0) {
                        teamImage.setImageResource(resId)
                        teamImage.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        teamLetter.visibility = View.GONE
                    } else {
                        teamImage.setImageDrawable(null)
                        teamImage.setBackgroundColor(Color.parseColor(team.colorHex))
                        teamLetter.text = team.name.firstOrNull()?.toString() ?: ""
                        teamLetter.visibility = View.VISIBLE
                    }
                }
                else -> {
                    teamImage.setImageDrawable(null)
                    teamImage.setBackgroundColor(Color.parseColor(team.colorHex))
                    teamLetter.text = team.name.firstOrNull()?.toString() ?: ""
                    teamLetter.visibility = View.VISIBLE
                }
            }
            item.findViewById<TextView>(R.id.completedTeamName).text = team.name
            mypageCompletedContainer.addView(item)
        }
    }

    private fun applyUserProfileImage(imageView: ImageView, imageResName: String) {
        when {
            imageResName.startsWith("file:") -> {
                val file = java.io.File(requireContext().filesDir, imageResName.removePrefix("file:"))
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)?.let {
                        imageView.setImageBitmap(it)
                        imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                } else {
                    imageView.setImageResource(com.project.unimate.R.drawable.ic_user)
                    imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            }
            imageResName.isNotBlank() -> {
                val resId = resources.getIdentifier(imageResName, "drawable", requireContext().packageName)
                if (resId != 0) {
                    imageView.setImageResource(resId)
                    imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                }
            }
            else -> {
                imageView.setImageResource(com.project.unimate.R.drawable.ic_user)
                imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        }
    }

    private fun logout() {
        val jwt = JwtStore.load(requireContext())
        if (jwt.isNullOrBlank()) {
            finishLogoutLocally()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<AuthService>(requireContext())
                service.logout()
            } catch (_: Exception) { }
            finishLogoutLocally()
        }
    }

    private fun finishLogoutLocally() {
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
