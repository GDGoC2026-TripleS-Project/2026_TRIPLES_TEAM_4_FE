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
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.service.PokeService
import kotlinx.coroutines.launch

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

    override fun onDestroyView() {
        super.onDestroyView()
        btnSendPoke = null
    }

    private fun loadPokeTargets() {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<PokeService>(ctx)
                val response = service.getTargets()
                if (response.isSuccessful) {
                    val teams = response.body()?.teams ?: emptyList()
                    val pokeList = mutableListOf<PokeData>()
                    teams.forEach { teamSection ->
                        val teamId = teamSection.teamId ?: return@forEach
                        val teamName = teamSection.teamName ?: return@forEach
                        val teamColor = "#90A3ED"
                        pokeList.add(PokeData.Header(teamId, teamName, teamColor))
                        teamSection.members?.forEach { member ->
                            val userId = member.userId ?: return@forEach
                            pokeList.add(
                                PokeData.Member(
                                    userId = userId,
                                    teamId = teamId,
                                    teamName = teamName,
                                    teamColor = teamColor,
                                    name = member.nickname ?: ""
                                )
                            )
                        }
                    }
                    if (isAdded) {
                        pokeAdapter.submitList(pokeList)
                        updateButtonState(0)
                    }
                } else {
                    Log.e("PokeFragment", "targets 실패: ${response.code()}")
                    if (isAdded) {
                        Toast.makeText(ctx, "팀원 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PokeFragment", "targets 예외: ${e.message}")
                if (isAdded) {
                    Toast.makeText(ctx, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
