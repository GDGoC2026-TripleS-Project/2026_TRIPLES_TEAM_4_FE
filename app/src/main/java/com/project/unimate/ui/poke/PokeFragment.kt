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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R

class PokeFragment : Fragment() {

    private lateinit var pokeAdapter: PokeAdapter
    private val dataList = mutableListOf<PokeData>() // 전체 데이터 (헤더 + 멤버)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poke, container, false)

        // 1. 더미 데이터 생성 (팀원 목록)
        setupDummyData()

        val rvPokeList = view.findViewById<RecyclerView>(R.id.rvPokeList)
        val btnPokeAction = view.findViewById<Button>(R.id.btnPokeAction)

        // 초기 버튼 상태 설정 (비활성화)
        updateButtonState(btnPokeAction)

        // 2. 어댑터 연결
        pokeAdapter = PokeAdapter(dataList) {
            // 리스트에서 체크박스를 누를 때마다 버튼 상태 업데이트
            updateButtonState(btnPokeAction)
        }

        rvPokeList.layoutManager = LinearLayoutManager(context)
        rvPokeList.adapter = pokeAdapter

        // 3. [핵심] 찌르기 버튼 클릭 시 -> 데이터 전달 및 화면 이동
        btnPokeAction.setOnClickListener {
            // (1) 전체 데이터 중 'Member' 타입이면서 'isSelected'가 true인 것만 골라냄
            val selectedMembers = dataList
                .filterIsInstance<PokeData.Member>()
                .filter { it.isSelected }

            if (selectedMembers.isNotEmpty()) {
                // (2) ArrayList로 변환 (Bundle에 넣기 위함)
                val arrayList = ArrayList(selectedMembers)

                // (3) Bundle 생성 및 데이터 담기
                // "selected_members"라는 키값은 받는 쪽(DetailFragment)과 똑같아야 합니다!
                val bundle = Bundle().apply {
                    putParcelableArrayList("selected_members", arrayList)
                }

                // (4) 다음 화면으로 이동 (네비게이션 액션 ID 확인 필수)
                try {
                    findNavController().navigate(R.id.action_pokeFragment_to_pokeDetailFragment, bundle)
                } catch (e: Exception) {
                    Log.e("PokeFragment", "Navigation Error: ${e.message}")
                    Toast.makeText(context, "페이지 이동 오류: NavGraph를 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return view
    }

    private fun setupDummyData() {
        dataList.clear() // 중복 방지 초기화

        // 색상 정의 (colors.xml의 색상과 유사하게 설정)
        val cherryColor = "#FF4081" // 체리시 (분홍)
        val megaColor = "#FFC107"   // 메가커피 (노랑)
        val monimoColor = "#3366FF" // 모니모 (파랑)

        // --- Team 1: 체리시 ---
        dataList.add(PokeData.Header("체리시", cherryColor))
        dataList.add(PokeData.Member(1, "김철수", "체리시", cherryColor))
        dataList.add(PokeData.Member(2, "이영희", "체리시", cherryColor))
        dataList.add(PokeData.Member(3, "박민수", "체리시", cherryColor))
        dataList.add(PokeData.Member(4, "최지우", "체리시", cherryColor))

        // --- Team 2: 메가커피릿 ---
        dataList.add(PokeData.Header("메가커피릿", megaColor))
        dataList.add(PokeData.Member(5, "정수빈", "메가커피릿", megaColor))
        dataList.add(PokeData.Member(6, "한소희", "메가커피릿", megaColor))
        dataList.add(PokeData.Member(7, "강동원", "메가커피릿", megaColor))

        // --- Team 3: 모니모 ---
        dataList.add(PokeData.Header("모니모", monimoColor))
        dataList.add(PokeData.Member(8, "아이유", "모니모", monimoColor))
        dataList.add(PokeData.Member(9, "카리나", "모니모", monimoColor))
        dataList.add(PokeData.Member(10, "윈터", "모니모", monimoColor))
    }

    private fun updateButtonState(button: Button) {
        // 선택된 멤버 수 계산
        val selectedCount = dataList.count { it is PokeData.Member && it.isSelected }

        if (selectedCount > 0) {
            // [활성화 상태] - 메인 그린 색상
            button.isEnabled = true
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.main_green)
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