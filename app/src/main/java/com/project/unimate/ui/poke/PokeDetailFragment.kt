package com.project.unimate.ui.poke

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.unimate.R
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.PokeRequest
import com.project.unimate.network.dto.PokeTarget
import com.project.unimate.network.service.PokeService
import kotlinx.coroutines.launch

class PokeDetailFragment : Fragment() {

    private var selectedMembers: ArrayList<PokeData.Member>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 이전 화면에서 보낸 데이터 받기
        arguments?.let {
            selectedMembers = it.getParcelableArrayList("selected_members")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poke_detail, container, false)

        // 뷰 연결
        val rvSelectedUsers = view.findViewById<RecyclerView>(R.id.rvSelectedUsers)
        val tvSelectedCount = view.findViewById<TextView>(R.id.tvSelectedCount)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)

        val layoutMessageDropdown = view.findViewById<ConstraintLayout>(R.id.layoutMessageDropdown)
        val tvSelectedMessage = view.findViewById<TextView>(R.id.tvSelectedMessage)
        val ivDropdownArrow = view.findViewById<ImageView>(R.id.ivDropdownArrow)
        val rvMessageList = view.findViewById<RecyclerView>(R.id.rvMessageList)

        // [중요] 버튼 연결
        val btnSendPoke = view.findViewById<Button>(R.id.btnSendPoke)

        // 1. 초기 상태: 버튼 비활성화 (XML의 shape_button_join 덕분에 회색으로 시작)
        btnSendPoke.isEnabled = false

        // 2. 선택된 유저 목록 설정
        selectedMembers?.let { members ->
            tvSelectedCount.text = "${members.size}"
            rvSelectedUsers.layoutManager = LinearLayoutManager(context)
            rvSelectedUsers.adapter = SelectedUserAdapter(members)
        }

        // 3. 메시지 리스트 데이터 (더미, API 성공 시 교체)
        val messages = mutableListOf(
            "자료를 기다리고 있는 팀원의 간절한 눈빛이 느껴져요 👀",
            "혹시 바쁜 일정에 마감일을 잊으신 건 아니죠? ⏰",
            "팀원이 전한 메시지가 답변을 기다리고 있어요 💌",
            "지금 바로 회의 가능한 시간을 콕 찍어주실래요? 👉",
            "놓치면 안 될 중요한 팀 공지가 도착해 있어요 📢"
        )
        // 메시지 ID 맵 (API용)
        val messageIdMap = mutableMapOf<String, Long>()

        // 4. 메시지 선택 시 동작 (여기서 버튼 색상이 바뀝니다!)
        val messageAdapter = MessageAdapter(messages) { selectedMsg ->
            // 드롭다운 텍스트 변경
            tvSelectedMessage.text = selectedMsg
            tvSelectedMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

            // 리스트 닫기 & 화살표 원위치
            rvMessageList.visibility = View.GONE
            ivDropdownArrow.animate().rotation(0f).start()

            // ★ [핵심 기능] 버튼 활성화
            // 이 코드가 실행되면 isEnabled가 true가 되면서
            // XML에 설정한 selector가 작동하여 배경이 '연두색'으로 변합니다.
            btnSendPoke.isEnabled = true
        }

        rvMessageList.layoutManager = LinearLayoutManager(context)
        rvMessageList.adapter = messageAdapter

        // 5. 드롭다운 박스 클릭 (열기/닫기)
        layoutMessageDropdown.setOnClickListener {
            if (rvMessageList.visibility == View.VISIBLE) {
                // 닫기
                rvMessageList.visibility = View.GONE
                ivDropdownArrow.animate().rotation(0f).start()
            } else {
                // 열기
                rvMessageList.visibility = View.VISIBLE
                ivDropdownArrow.animate().rotation(180f).start()
            }
        }

        // 6. 뒤로가기
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // API에서 찌르기 문구 로드
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<PokeService>(requireContext())
                val response = service.getMessages()
                if (response.isSuccessful) {
                    val apiMessages = response.body()
                    if (!apiMessages.isNullOrEmpty()) {
                        messages.clear()
                        messageIdMap.clear()
                        apiMessages.forEach { msg ->
                            val content = msg.content ?: return@forEach
                            messages.add(content)
                            msg.messageId?.let { messageIdMap[content] = it }
                        }
                        rvMessageList.adapter = MessageAdapter(messages) { selectedMsg ->
                            tvSelectedMessage.text = selectedMsg
                            tvSelectedMessage.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            rvMessageList.visibility = View.GONE
                            ivDropdownArrow.animate().rotation(0f).start()
                            btnSendPoke.isEnabled = true
                        }
                    }
                }
            } catch (_: Exception) { }
        }

        // 7. 찌르기 버튼 클릭 (API 호출 + 토스트)
        btnSendPoke.setOnClickListener {
            if (!selectedMembers.isNullOrEmpty()) {
                val firstUser = selectedMembers!![0].name
                val extraCount = selectedMembers!!.size - 1
                val toastMsg = if (extraCount > 0) {
                    "${firstUser}님 외 ${extraCount}명에게 찌르기를 보냈어요 👋"
                } else {
                    "${firstUser}님에게 찌르기를 보냈어요 👋"
                }

                // API 호출
                val selectedMsg = tvSelectedMessage.text.toString()
                val msgId = messageIdMap[selectedMsg] ?: 1L
                btnSendPoke.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val service = RetrofitClient.create<PokeService>(requireContext())
                        service.sendPokes(PokeRequest(
                            messageId = msgId,
                            targets = selectedMembers!!.map { PokeTarget(teamId = 0, userId = it.id.toLong()) }
                        ))
                    } catch (_: Exception) { }
                    Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }

        return view
    }
}