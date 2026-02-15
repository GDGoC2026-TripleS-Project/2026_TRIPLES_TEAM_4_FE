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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R
import com.project.unimate.model.PokeTargetsResponse
import com.project.unimate.network.PokeService
import com.project.unimate.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PokeFragment : Fragment() {

    private lateinit var pokeAdapter: PokeAdapter
    private val dataList = mutableListOf<PokeData>()
    private lateinit var pokeService: PokeService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poke, container, false)

        pokeService = RetrofitClient.getInstance(requireContext()).create(PokeService::class.java)

        val rvPokeList = view.findViewById<RecyclerView>(R.id.rvPokeList)
        val btnPokeAction = view.findViewById<Button>(R.id.btnSendPoke)

        updateButtonState(btnPokeAction)

        pokeAdapter = PokeAdapter(dataList) {
            updateButtonState(btnPokeAction)
        }

        rvPokeList.layoutManager = LinearLayoutManager(context)
        rvPokeList.adapter = pokeAdapter

        fetchPokeTargets(btnPokeAction)

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
                }
            }
        }

        return view
    }

    private fun fetchPokeTargets(button: Button) {
        pokeService.getPokeTargets().enqueue(object : Callback<PokeTargetsResponse> {
            override fun onResponse(call: Call<PokeTargetsResponse>, response: Response<PokeTargetsResponse>) {
                if (response.isSuccessful) {
                    // response.body()는 이제 PokeTargetsResponse 객체입니다.
                    val teamsData = response.body()?.teams ?: return

                    if (teamsData.isEmpty()) {
                        Log.d("PokeAPI", "소속된 팀이 없거나 찌를 수 있는 팀원이 없습니다.")
                        return
                    }

                    dataList.clear()
                    // 명세서에 팀별 색상 필드가 없으므로, 임시로 기본 색상을 사용하거나
                    // 이전 더미의 색상 로직을 활용할 수 있습니다.
                    val defaultColor = "#3FE9C0"

                    teamsData.forEach { team ->
                        // 1. 팀 헤더 추가
                        dataList.add(PokeData.Header(team.teamName, defaultColor))

                        // 2. 해당 팀의 멤버들 추가
                        team.members.forEach { member ->
                            dataList.add(
                                PokeData.Member(
                                    userId = member.userId,
                                    name = member.nickname,
                                    teamName = team.teamName,
                                    teamColor = defaultColor,
                                    teamId = team.teamId
                                )
                            )
                        }
                    }
                    pokeAdapter.notifyDataSetChanged()
                    updateButtonState(button)
                }
            }

            override fun onFailure(call: Call<PokeTargetsResponse>, t: Throwable) {
                Log.e("PokeFragment", "데이터 조회 실패: ${t.message}")
            }
        })
    }
    private fun updateButtonState(button: Button) {
        val selectedCount = dataList.count { it is PokeData.Member && it.isSelected }
        val typeFace = ResourcesCompat.getFont(requireContext(), R.font.pretendard_semibold)
        button.typeface = typeFace

        if (selectedCount > 0) {
            button.isEnabled = true
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.green05)
            )
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            button.text = "찌르기 (${selectedCount})"
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