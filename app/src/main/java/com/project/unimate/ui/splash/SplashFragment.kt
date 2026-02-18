package com.project.unimate.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.auth.JwtStore
import com.project.unimate.data.repository.ServerSync
import com.project.unimate.databinding.FragmentSplashBinding
import kotlinx.coroutines.launch

/**
 * 앱의 시작을 알리는 스플래시.
 * JWT 있으면 서버 동기화 후 홈으로, 없으면 2초 뒤 로그인으로.
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        val jwt = JwtStore.load(requireContext())
        if (!jwt.isNullOrBlank()) {
            // 이미 로그인됨 → 서버에서 유저/팀/일정 전부 불러온 뒤 홈으로
            viewLifecycleOwner.lifecycleScope.launch {
                ServerSync.syncFromServer(requireContext().applicationContext)
                if (isAdded) {
                    findNavController().navigate(R.id.action_splash_to_home)
                }
            }
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) moveToLogin()
            }, 2000)
        }
    }

    private fun moveToLogin() {
        findNavController().navigate(R.id.action_splash_to_login)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
