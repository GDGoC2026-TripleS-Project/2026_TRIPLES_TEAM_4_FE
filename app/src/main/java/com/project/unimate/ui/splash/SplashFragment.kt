package com.project.unimate.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.databinding.FragmentSplashBinding

/**
 * 앱의 시작을 알리는 스플래시 프래그먼트입니다.
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        // 2초 뒤에 로그인 화면으로 이동하는 로직
        Handler(Looper.getMainLooper()).postDelayed({
            // 프래그먼트가 여전히 호스트 액티비티에 붙어 있는지 확인 후 이동
            if (isAdded) {
                moveToLogin()
            }
        }, 2000) // 2000ms = 2초
    }

    private fun moveToLogin() {
        // nav_graph.xml에 정의한 action_splash_to_login을 호출합니다.
        findNavController().navigate(R.id.action_splash_to_login)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}