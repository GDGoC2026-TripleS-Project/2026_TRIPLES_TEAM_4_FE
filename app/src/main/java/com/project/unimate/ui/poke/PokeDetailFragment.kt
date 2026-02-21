package com.project.unimate.ui.poke

// 역할: 찌르기 메시지 선택·전송. PokeService send. 선택 인원·메시지 검증

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

    private var selectedMembers: List<PokeData.Member> = emptyList()
    private var selectedMessageId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedMembers = arguments?.getParcelableArrayList<PokeData.Member>("selected_members")
            ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_poke_detail, container, false)

        val rvSelectedUsers = view.findViewById<RecyclerView>(R.id.rvSelectedUsers)
        val tvSelectedCount = view.findViewById<TextView>(R.id.tvSelectedCount)
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val layoutMessageDropdown = view.findViewById<ConstraintLayout>(R.id.layoutMessageDropdown)
        val tvSelectedMessage = view.findViewById<TextView>(R.id.tvSelectedMessage)
        val ivDropdownArrow = view.findViewById<ImageView>(R.id.ivDropdownArrow)
        val rvMessageList = view.findViewById<RecyclerView>(R.id.rvMessageList)
        val btnSendPoke = view.findViewById<Button>(R.id.btnSendPoke)

        // 초기: 버튼 비활성화
        btnSendPoke.isEnabled = false

        // 선택된 팀원 목록
        tvSelectedCount.text = "${selectedMembers.size}"
        rvSelectedUsers.layoutManager = LinearLayoutManager(context)
        rvSelectedUsers.adapter = SelectedUserAdapter(selectedMembers)

        // 드롭다운 열기/닫기
        rvMessageList.layoutManager = LinearLayoutManager(context)
        layoutMessageDropdown.setOnClickListener {
            if (rvMessageList.visibility == View.VISIBLE) {
                rvMessageList.visibility = View.GONE
                ivDropdownArrow.animate().rotation(0f).start()
            } else {
                rvMessageList.visibility = View.VISIBLE
                ivDropdownArrow.animate().rotation(180f).start()
            }
        }

        btnBack.setOnClickListener { findNavController().popBackStack() }

        // 찌르기 문구 서버에서 로드
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<PokeService>(requireContext())
                val response = service.getMessages()
                if (response.isSuccessful && isAdded) {
                    val apiMessages = response.body() ?: emptyList()
                    if (apiMessages.isNotEmpty()) {
                        rvMessageList.adapter = MessageAdapter(apiMessages.mapNotNull { it.content }) { selectedContent ->
                            selectedMessageId = apiMessages.find { it.content == selectedContent }?.messageId
                            tvSelectedMessage.text = selectedContent
                            tvSelectedMessage.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.black)
                            )
                            rvMessageList.visibility = View.GONE
                            ivDropdownArrow.animate().rotation(0f).start()
                            btnSendPoke.isEnabled = (selectedMessageId != null)
                        }
                    } else {
                        tvSelectedMessage.text = "문구 없음"
                    }
                }
            } catch (_: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "찌르기 문구를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 찌르기 전송
        btnSendPoke.setOnClickListener {
            val msgId = selectedMessageId
            if (msgId == null) {
                Toast.makeText(requireContext(), "찌르기 문구를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedMembers.isEmpty()) {
                Toast.makeText(requireContext(), "찌를 팀원을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSendPoke.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                // userId <= 0 인 멤버는 더미(로컬 전용) — API 호출 없이 성공 처리
                val serverMembers = selectedMembers.filter { it.userId > 0 }
                val dummyCount = selectedMembers.count { it.userId <= 0 }

                try {
                    if (serverMembers.isEmpty()) {
                        // 더미만 선택된 경우: 바로 성공 처리
                        if (!isAdded) return@launch
                        Toast.makeText(requireContext(), "총 ${dummyCount}명에게 찌르기를 보냈습니다.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                        return@launch
                    }

                    val service = RetrofitClient.create<PokeService>(requireContext())
                    val request = PokeRequest(
                        messageId = msgId,
                        targets = serverMembers.map { PokeTarget(teamId = it.teamId, userId = it.userId) }
                    )
                    val response = service.sendPokes(request)
                    if (!isAdded) return@launch

                    if (response.isSuccessful) {
                        val body = response.body()
                        val sentCount = (body?.sentCount ?: 0) + dummyCount
                        val excludedSelf = body?.excludedSelfCount ?: 0
                        val invalid = body?.invalidTargets ?: emptyList()

                        val msg = buildString {
                            append("총 ${sentCount}명에게 찌르기를 보냈습니다.")
                            if (excludedSelf > 0) append("\n본인 제외 ${excludedSelf}명")
                            if (invalid.isNotEmpty()) {
                                append("\n전송 실패: ")
                                append(invalid.joinToString(", ") { iv ->
                                    "userId=${iv.userId}(${iv.reason ?: "알 수 없음"})"
                                })
                            }
                        }
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        findNavController().popBackStack()
                    } else {
                        val errorMsg = when (response.code()) {
                            400 -> "잘못된 요청입니다."
                            403 -> "같은 팀원이 아닌 대상이 포함되어 있습니다."
                            404 -> "찌르기 문구를 찾을 수 없습니다."
                            else -> "찌르기 전송에 실패했습니다. (${response.code()})"
                        }
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        btnSendPoke.isEnabled = true
                    }
                } catch (e: Exception) {
                    if (!isAdded) return@launch
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    btnSendPoke.isEnabled = true
                }
            }
        }

        return view
    }
}
