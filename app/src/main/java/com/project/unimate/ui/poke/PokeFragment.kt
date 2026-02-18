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

class PokeFragment : Fragment() {

    private lateinit var pokeAdapter: PokeAdapter
    private val dataList = mutableListOf<PokeData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poke, container, false)

        buildListFromRepository()

        val rvPokeList = view.findViewById<RecyclerView>(R.id.rvPokeList)
        val btnPokeAction = view.findViewById<Button>(R.id.btnSendPoke)

        updateButtonState(btnPokeAction)

        pokeAdapter = PokeAdapter(dataList) {
            updateButtonState(btnPokeAction)
        }

        rvPokeList.layoutManager = LinearLayoutManager(context)
        rvPokeList.adapter = pokeAdapter

        btnPokeAction.setOnClickListener {
            val selectedMembers = dataList
                .filterIsInstance<PokeData.Member>()
                .filter { it.isSelected }

            if (selectedMembers.isNotEmpty()) {
                val arrayList = ArrayList(selectedMembers)
                val bundle = Bundle().apply {
                    putParcelableArrayList("selected_members", arrayList)
                }
                try {
                    findNavController().navigate(R.id.action_pokeFragment_to_pokeDetailFragment, bundle)
                } catch (e: Exception) {
                    Log.e("PokeFragment", "Navigation Error: ${e.message}")
                    Toast.makeText(context, "페이지 이동 오류: NavGraph를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loadPokeTargetsFromApi()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadPokeTargetsFromApi()
    }

    /** 로컬 팀 정보(이름·색) + API 팀원 정보를 합쳐서 표시. 연동된 팀에는 API에서 받은 다른 사용자가 뜨도록. */
    private fun loadPokeTargetsFromApi() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<PokeService>(ctx)
                val response = service.getTargets()
                val apiTeams = if (response.isSuccessful) response.body()?.teams ?: emptyList() else emptyList()
                buildListMerged(apiTeams)
                if (::pokeAdapter.isInitialized) {
                    pokeAdapter.notifyDataSetChanged()
                }
                view?.findViewById<Button>(R.id.btnSendPoke)?.let { updateButtonState(it) }
            } catch (_: Exception) {
                buildListMerged(emptyList())
                if (::pokeAdapter.isInitialized) pokeAdapter.notifyDataSetChanged()
                view?.findViewById<Button>(R.id.btnSendPoke)?.let { updateButtonState(it) }
            }
        }
    }

    /**
     * 로컬 팀 목록 + API 팀/팀원 정보를 합침.
     * - 팀 이름·색: 로컬(DummyRepository) 우선, 없으면 API 값 사용.
     * - 팀원: 해당 팀이 API에 있으면 API 팀원(다른 사용자) 사용, 없으면 로컬 팀원. 나(현재 사용자)는 항상 제외.
     */
    private fun buildListMerged(apiTeams: List<PokeTeamSection>) {
        dataList.clear()
        val currentUserName = DummyRepository.getCurrentUserName()
        val apiByTeamId: Map<String, PokeTeamSection> = apiTeams.associate { team -> Pair(team.teamId?.toString() ?: "", team) }
        val apiByTeamName: Map<String, PokeTeamSection> = apiTeams.associateBy { it.teamName ?: "" }
        var memberId = 1
        val localTeamIds = DummyRepository.allTeams.map { it.id }.toSet()
        val apiTeamIdsAdded = mutableSetOf<String>()

        DummyRepository.allTeams.forEach { team ->
            val teamIdStr = team.id
            val teamName = team.name
            val teamColor = team.colorHex.ifBlank { "#90A3ED" }
            val apiSection = apiByTeamId[teamIdStr] ?: apiByTeamName[teamName]
            val memberNames = if (apiSection != null) {
                apiSection.members?.mapNotNull { m -> m.nickname?.takeIf { it != currentUserName } }?.distinct() ?: emptyList()
            } else {
                DummyRepository.getTeamMembers(team.id)
                    .filter { it.id != "me" && it.name != currentUserName }
                    .map { it.name }
            }
            dataList.add(PokeData.Header(teamName, teamColor))
            if (memberNames.isEmpty()) {
                dataList.add(PokeData.NoMembersMessage(teamName = teamName, teamColor = teamColor))
            } else {
                memberNames.forEach { name ->
                    dataList.add(PokeData.Member(id = memberId++, name = name, teamName = teamName, teamColor = teamColor))
                }
            }
            apiSection?.teamId?.toString()?.let { apiTeamIdsAdded.add(it) }
        }

        apiTeams.forEach { teamSection ->
            val teamIdStr = teamSection.teamId?.toString() ?: return@forEach
            if (teamIdStr in localTeamIds || teamIdStr in apiTeamIdsAdded) return@forEach
            apiTeamIdsAdded.add(teamIdStr)
            val teamName = teamSection.teamName ?: return@forEach
            val teamColor = teamSection.teamId?.let { DummyRepository.getTeamById(it.toString())?.colorHex }?.takeIf { it.isNotBlank() }
                ?: "#90A3ED"
            dataList.add(PokeData.Header(teamName, teamColor))
            val others = teamSection.members?.mapNotNull { m -> m.nickname?.takeIf { it != currentUserName } }?.distinct() ?: emptyList()
            if (others.isEmpty()) {
                dataList.add(PokeData.NoMembersMessage(teamName = teamName, teamColor = teamColor))
            } else {
                others.forEach { name ->
                    dataList.add(PokeData.Member(id = memberId++, name = name, teamName = teamName, teamColor = teamColor))
                }
            }
        }
    }

    /** API 호출 없이 로컬만으로 목록 구성 (초기 표시용). 팀원에서 나(현재 사용자)는 제외. */
    private fun buildListFromRepository() {
        buildListMerged(emptyList())
    }

    private fun updateButtonState(button: Button) {
        // 선택된 멤버 수 계산
        val selectedCount = dataList.count { it is PokeData.Member && it.isSelected }

        val typeFace = ResourcesCompat.getFont(requireContext(), R.font.pretendard_semibold)
        button.typeface = typeFace

        if (selectedCount > 0) {
            // [활성화 상태] - 메인 그린 색상
            button.isEnabled = true
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.green05)
            )
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            button.text = "찌르기 (${selectedCount})" // (선택사항) 몇 명인지 표시
        } else {
            // [비활성화 상태] - 회색
            button.isEnabled = false
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.gray01)
            )
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray05))
            button.text = "찌르기"
        }
    }
}