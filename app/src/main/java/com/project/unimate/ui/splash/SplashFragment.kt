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
 * 스플래시. JWT 있으면 서버 동기화 후 홈, 없으면 2초 후 로그인. 동기화 실패해도 홈 이동(갇힘 방지).
 */
class SplashFragment : Fragment(R.layout.fragment_splash) {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSplashBinding.bind(view)

        val jwt = JwtStore.load(requireContext())
        if (!jwt.isNullOrBlank()) {
            // 로그인 상태: 서버 sync 1회 후 홈. 실패해도 홈 이동(스플래시 갇힘 방지)
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    ServerSync.syncFromServer(requireContext().applicationContext)
                } catch (_: Exception) { /* 실패 시 로컬 캐시로 홈 표시 */ }
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
