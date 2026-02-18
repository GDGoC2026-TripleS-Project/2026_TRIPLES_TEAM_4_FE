package com.project.unimate.ui.teamspace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.service.TeamService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TeamShareFragment : Fragment() {

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_team_share, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inviteCodeView = view.findViewById<TextView>(R.id.tvShareInviteCode)
        val countdownView  = view.findViewById<TextView>(R.id.tvShareCountdown)
        val reissueBtn     = view.findViewById<View>(R.id.btnShareReissue)

        val teamId  = arguments?.getString("teamId")
        val myRole  = arguments?.getString("myRole")
        val isLeader = myRole?.uppercase() == "LEADER"

        Log.d("TeamShare", "진입 — teamId=$teamId, myRole=$myRole, isLeader=$isLeader")

        // teamId 없으면 즉시 에러 UI
        if (teamId.isNullOrBlank()) {
            inviteCodeView.text = "---"
            countdownView.text = "팀 정보를 불러올 수 없습니다."
            Log.e("TeamShare", "teamId가 전달되지 않았습니다.")
        } else {
            inviteCodeView.text = "로딩 중..."
            countdownView.text = ""
            loadInviteCode(teamId, isLeader, inviteCodeView, countdownView, reissueBtn)
        }

        // 재발급 버튼: 팀장만 표시
        reissueBtn.visibility = if (isLeader) View.VISIBLE else View.GONE
        reissueBtn.setOnClickListener {
            val numericId = teamId?.toLongOrNull() ?: return@setOnClickListener
            reissueBtn.isEnabled = false
            stopCountdown()
            inviteCodeView.text = "재발급 중..."
            countdownView.text = ""
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val service = RetrofitClient.create<TeamService>(requireContext())
                    val resp = service.issueInviteCode(numericId)
                    Log.d("TeamShare", "재발급 응답: ${resp.code()}")
                    if (resp.isSuccessful) {
                        val code      = resp.body()?.inviteCode
                        val expiresAt = resp.body()?.expiresAt
                        Log.d("TeamShare", "재발급 성공: code=$code, expiresAt=$expiresAt")
                        if (!code.isNullOrBlank()) {
                            inviteCodeView.text = code
                            startCountdown(expiresAt, countdownView, reissueBtn)
                        } else {
                            inviteCodeView.text = "---"
                        }
                    } else {
                        val errorBody = resp.errorBody()?.string() ?: ""
                        Log.e("TeamShare", "재발급 실패: ${resp.code()} | $errorBody")
                        inviteCodeView.text = "재발급 실패"
                        Toast.makeText(requireContext(), "초대코드 재발급에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TeamShare", "재발급 예외: ${e.message}")
                    inviteCodeView.text = "---"
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                } finally {
                    reissueBtn.isEnabled = true
                }
            }
        }

        // 복사 로직
        fun copyInviteCode() {
            val code = inviteCodeView.text?.toString()?.trim().orEmpty()
            if (code.isBlank() || code == "로딩 중..." || code == "---"
                || code == "재발급 중..." || code == "재발급 실패") {
                Toast.makeText(requireContext(), "복사할 초대코드가 없습니다.", Toast.LENGTH_SHORT).show()
                return
            }
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("TeamInviteCode", code))
            Toast.makeText(requireContext(), "초대코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.teamShareBack).setOnClickListener { findNavController().popBackStack() }
        view.findViewById<View>(R.id.btnShareCopyCode).setOnClickListener { copyInviteCode() }
        view.findViewById<View>(R.id.ivShareCopyIcon).setOnClickListener { copyInviteCode() }
    }

    // ── 초대코드 로드 (역할 분기) ────────────────────────────────────────────

    private fun loadInviteCode(
        teamId: String,
        isLeader: Boolean,
        inviteCodeView: TextView,
        countdownView: TextView,
        reissueBtn: View
    ) {
        val numericId = teamId.toLongOrNull() ?: run {
            inviteCodeView.text = "---"
            countdownView.text = "팀 정보를 불러올 수 없습니다."
            Log.e("TeamShare", "teamId가 숫자가 아닙니다: $teamId")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val service = RetrofitClient.create<TeamService>(requireContext())

                if (isLeader) {
                    // 팀장: POST /api/teams/{teamId}/invite-code
                    val resp = service.issueInviteCode(numericId)
                    Log.d("TeamShare", "POST /invite-code 응답: ${resp.code()}")
                    when {
                        resp.isSuccessful -> {
                            val code      = resp.body()?.inviteCode
                            val expiresAt = resp.body()?.expiresAt
                            Log.d("TeamShare", "발급 성공: code=$code, expiresAt=$expiresAt")
                            if (!code.isNullOrBlank()) {
                                inviteCodeView.text = code
                                startCountdown(expiresAt, countdownView, reissueBtn)
                            } else {
                                inviteCodeView.text = "---"
                                countdownView.text = "초대코드를 불러올 수 없습니다."
                            }
                        }
                        resp.code() == 403 || resp.code() == 401 -> {
                            // 권한 오류 → GET으로 fallback
                            Log.w("TeamShare", "POST 권한 거부(${resp.code()}) → GET fallback")
                            tryGetInviteCode(numericId, service, inviteCodeView, countdownView, isLeader)
                        }
                        else -> {
                            val errorBody = resp.errorBody()?.string() ?: ""
                            Log.e("TeamShare", "POST /invite-code 실패: ${resp.code()} | $errorBody")
                            inviteCodeView.text = "---"
                            countdownView.text = "초대코드를 불러올 수 없습니다."
                        }
                    }
                } else {
                    // 팀원: GET /api/teams/{teamId}/invite
                    tryGetInviteCode(numericId, service, inviteCodeView, countdownView, isLeader)
                }

            } catch (e: Exception) {
                Log.e("TeamShare", "초대코드 로드 예외: ${e.message}")
                if (isAdded) {
                    inviteCodeView.text = "---"
                    countdownView.text = "초대코드를 불러올 수 없습니다."
                }
            }
        }
    }

    private suspend fun tryGetInviteCode(
        numericId: Long,
        service: TeamService,
        inviteCodeView: TextView,
        countdownView: TextView,
        isLeader: Boolean
    ) {
        val resp = service.getInviteCode(numericId)
        Log.d("TeamShare", "GET /invite 응답: ${resp.code()}")
        if (resp.isSuccessful) {
            val code      = resp.body()?.inviteCode
            val expiresAt = resp.body()?.expiresAt
            Log.d("TeamShare", "조회 성공: code=$code, expiresAt=$expiresAt")
            if (!code.isNullOrBlank()) {
                inviteCodeView.text = code
                val reissueBtn = view?.findViewById<View>(R.id.btnShareReissue)
                startCountdown(expiresAt, countdownView, reissueBtn)
            } else {
                inviteCodeView.text = "---"
                countdownView.text = "초대코드가 없습니다."
            }
        } else {
            val errorBody = resp.errorBody()?.string() ?: ""
            Log.e("TeamShare", "GET /invite 실패: ${resp.code()} | $errorBody")
            inviteCodeView.text = "---"
            countdownView.text = if (isLeader) {
                "초대코드가 없습니다. 재발급 버튼을 눌러주세요."
            } else {
                "초대코드가 만료되었거나 발급되지 않았습니다.\n팀장에게 재발급 요청하세요."
            }
        }
    }

    // ── 카운트다운 ───────────────────────────────────────────────────────────

    private fun startCountdown(expiresAt: String?, countdownView: TextView, reissueBtn: View?) {
        stopCountdown()
        val expiresMillis = expiresAt?.let { parseExpiresAtToMillis(it) } ?: return
        val remaining = expiresMillis - System.currentTimeMillis()
        if (remaining <= 0) {
            countdownView.text = "만료됨"
            return
        }
        countDownTimer = object : CountDownTimer(remaining, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                val minutes = (millisUntilFinished / 60000).toInt()
                val seconds = ((millisUntilFinished % 60000) / 1000).toInt()
                countdownView.text = "남은 시간: %02d:%02d".format(minutes, seconds)
            }
            override fun onFinish() {
                if (!isAdded) return
                countdownView.text = "만료됨"
            }
        }.start()
    }

    private fun stopCountdown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun parseExpiresAtToMillis(expiresAt: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(expiresAt) ?: continue
                return date.time
            } catch (_: Exception) {}
        }
        Log.w("TeamShare", "expiresAt 파싱 실패: $expiresAt")
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCountdown()
    }
}
