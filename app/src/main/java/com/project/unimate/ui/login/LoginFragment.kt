package com.project.unimate.ui.login

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.auth.JwtStore
import com.project.unimate.databinding.FragmentLoginBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 모든 로그인 버튼이 동일한 성공 처리 로직을 거치도록 설정
        binding.btnKakao.setOnClickListener {
            handleLoginSuccess("kakao_temp_token_12345")
        }

        binding.btnNaver.setOnClickListener {
            handleLoginSuccess("naver_temp_token_12345")
        }

        binding.btnGoogle.setOnClickListener {
            handleLoginSuccess("google_temp_token_12345")
        }
    }

    /**
     * 서버와의 통신 및 토큰 저장을 담당하는 함수
     */
    private fun handleLoginSuccess(socialToken: String) {
        // 코루틴을 사용하여 비동기 처리 시뮬레이션
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. (시뮬레이션) 서버에 socialToken을 보내고 결과를 기다림
                // 실제 연동 시: val response = apiService.login(socialToken)
                delay(500) // 서버 통신 중인 것처럼 0.5초 대기

                // 2. JwtStore를 활용하여 서버에서 받았다고 가정하는 토큰 저장
                val mockJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                JwtStore.save(requireContext(), mockJwtToken)

                // 3. 토큰 저장 성공 메시지 (디버깅용)
                Toast.makeText(context, "로그인 성공 및 토큰 저장 완료", Toast.LENGTH_SHORT).show()

                // 4. 모든 처리가 완료된 후 화면 이동
                moveToProfileCreate()

            } catch (e: Exception) {
                Toast.makeText(context, "로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveToProfileCreate() {
        findNavController().navigate(R.id.action_login_to_profileCreate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}