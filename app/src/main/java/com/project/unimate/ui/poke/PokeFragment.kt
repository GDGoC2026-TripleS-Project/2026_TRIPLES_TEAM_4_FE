package com.project.unimate.ui.poke

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R
import com.project.unimate.data.repository.DummyRepository
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.PokeTeamSection
import com.project.unimate.network.service.PokeService
import kotlinx.coroutines.launch
import kotlin.math.abs

class PokeFragment : Fragment() {

    private lateinit var pokeAdapter: PokeAdapter
    private var btnSendPoke: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poke, container, false)
        val rvPokeList = view.findViewById<RecyclerView>(R.id.rvPokeList)
        btnSendPoke = view.findViewById(R.id.btnSendPoke)

        pokeAdapter = PokeAdapter { selectedCount -> updateButtonState(selectedCount) }
        rvPokeList.layoutManager = LinearLayoutManager(context)
        rvPokeList.adapter = pokeAdapter

        updateButtonState(0)
        buildListMerged(emptyList()).let { pokeAdapter.submitList(it) }

        btnSendPoke?.setOnClickListener {
            val selected = pokeAdapter.getSelectedMembers()
            if (selected.isEmpty()) {
                Toast.makeText(context, "찌를 팀원을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bundle = Bundle().apply {
                putParcelableArrayList("selected_members", ArrayList(selected))
            }
            try {
                findNavController().navigate(R.id.action_pokeFragment_to_pokeDetailFragment, bundle)
            } catch (e: Exception) {
                Log.e("PokeFragment", "Navigation Error: ${e.message}")
                Toast.makeText(context, "페이지 이동 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        loadPokeTargets()
        return view
    }

    override fun onResume() {
        super.onResume()
        loadPokeTargets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        btnSendPoke = null
    }

    /**
     * 로컬 팀 + API 팀/팀원 정보를 합쳐 목록 구성 (#33 함수명 loadPokeTargets 유지).
     * 팀 이름·색: 로컬(DummyRepository) 우선. 팀원 없으면 NoMembersMessage.
     */
    private fun loadPokeTargets() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<PokeService>(ctx)
                val response = service.getTargets()
                val apiTeams = if (response.isSuccessful) response.body()?.teams ?: emptyList() else emptyList()
                val pokeList = buildListMerged(apiTeams)
                if (isAdded) {
                    pokeAdapter.submitList(pokeList)
                    updateButtonState(pokeAdapter.getSelectedMembers().size)
                }
            } catch (e: Exception) {
                Log.e("PokeFragment", "targets 예외: ${e.message}")
                if (isAdded) {
                    pokeAdapter.submitList(buildListMerged(emptyList()))
                    Toast.makeText(ctx, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 로컬 팀 목록 + API 팀/팀원 정보를 합침.
     * - 팀 이름·색: 로컬 우선, 없으면 API 값 사용.
     * - 팀원: API에 있으면 API 팀원(다른 사용자), 없으면 로컬 팀원. 나(현재 사용자)는 제외.
     * - Header/Member는 #33 스타일(teamId: Long, userId: Long) 사용.
     */
    private fun buildListMerged(apiTeams: List<PokeTeamSection>): List<PokeData> {
        val result = mutableListOf<PokeData>()
        val currentUserName = DummyRepository.getCurrentUserName()
        val apiByTeamId = apiTeams.associate { (it.teamId?.toString() ?: "") to it }
        val apiByTeamName = apiTeams.associateBy { it.teamName ?: "" }
        val localTeamIds = DummyRepository.getMyTeamSpaceTeams().map { it.id }.toSet()
        val apiTeamIdsAdded = mutableSetOf<String>()
        var syntheticUserId = 1L

        fun localTeamIdToLong(teamIdStr: String): Long =
            -abs(teamIdStr.hashCode().toLong()).let { if (it == Long.MIN_VALUE) -1L else it }

        DummyRepository.getMyTeamSpaceTeams().forEach { team ->
            val teamIdStr = team.id
            val teamName = team.name
            val teamColor = team.colorHex.ifBlank { "#90A3ED" }
            val apiSection = apiByTeamId[teamIdStr] ?: apiByTeamName[teamName]
            val localTeamIdLong = localTeamIdToLong(teamIdStr)
            val effectiveTeamId = apiSection?.teamId?.let { it } ?: localTeamIdLong

            val members: List<Pair<Long, String>> = if (apiSection != null) {
                apiSection.members
                    ?.mapNotNull { m -> m.userId?.let { id -> m.nickname?.takeIf { it != currentUserName }?.let { nick -> id to nick } } }
                    ?.distinctBy { it.first } ?: emptyList()
            } else {
                DummyRepository.getTeamMembers(team.id)
                    .filter { it.id != "me" && it.name != currentUserName }
                    .mapIndexed { i, tm -> (syntheticUserId + i) to tm.name }
                    .also { syntheticUserId += it.size }
            }

            result.add(PokeData.Header(teamId = effectiveTeamId, title = teamName, teamColor = teamColor))
            if (members.isEmpty()) {
                result.add(PokeData.NoMembersMessage(teamName = teamName, teamColor = teamColor))
            } else {
                members.forEach { (uid, name) ->
                    result.add(
                        PokeData.Member(
                            userId = uid,
                            teamId = effectiveTeamId,
                            teamName = teamName,
                            teamColor = teamColor,
                            name = name
                        )
                    )
                }
            }
            apiSection?.teamId?.toString()?.let { apiTeamIdsAdded.add(it) }
        }

        apiTeams.forEach { teamSection ->
            val teamIdStr = teamSection.teamId?.toString() ?: return@forEach
            if (teamIdStr in localTeamIds || teamIdStr in apiTeamIdsAdded) return@forEach
            apiTeamIdsAdded.add(teamIdStr)
            val teamId = teamSection.teamId ?: return@forEach
            val teamName = teamSection.teamName ?: return@forEach
            val teamColor = DummyRepository.getTeamById(teamIdStr)?.colorHex?.takeIf { it.isNotBlank() } ?: "#90A3ED"
            result.add(PokeData.Header(teamId = teamId, title = teamName, teamColor = teamColor))
            val others = teamSection.members
                ?.mapNotNull { m -> m.userId?.let { id -> m.nickname?.takeIf { it != currentUserName }?.let { nick -> id to nick } } }
                ?.distinctBy { it.first } ?: emptyList()
            if (others.isEmpty()) {
                result.add(PokeData.NoMembersMessage(teamName = teamName, teamColor = teamColor))
            } else {
                others.forEach { (uid, name) ->
                    result.add(
                        PokeData.Member(
                            userId = uid,
                            teamId = teamId,
                            teamName = teamName,
                            teamColor = teamColor,
                            name = name
                        )
                    )
                }
            }
        }

        return result
    }

    private fun updateButtonState(selectedCount: Int) {
        val button = btnSendPoke ?: return
        if (!isAdded) return
        val typeFace = ResourcesCompat.getFont(requireContext(), R.font.pretendard_semibold)
        button.typeface = typeFace
        if (selectedCount > 0) {
            button.isEnabled = true
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.green05)
            )
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            button.text = "찌르기 ($selectedCount)"
        } else {
            button.isEnabled = false
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.gray01)
            )
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray05))
            button.text = "찌르기"
        }
    }
}
