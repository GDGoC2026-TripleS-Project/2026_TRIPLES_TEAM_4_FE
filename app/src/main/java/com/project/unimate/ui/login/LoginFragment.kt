package com.project.unimate.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.auth.JwtStore
import com.project.unimate.auth.LoginViewModel
import com.project.unimate.databinding.FragmentLoginBinding

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val vm: LoginViewModel by viewModels()

    private val socialLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 로그인 성공 → 프로필 완료 여부 확인 후 이동
                vm.fetchProfileCompleted { completed, err ->
                    requireActivity().runOnUiThread {
                        when (completed) {
                            true -> moveToHome()
                            false -> moveToProfileCreate()
                            null -> {
                                if (!err.isNullOrBlank()) {
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                return@registerForActivityResult
            }

            val msg = result.data?.getStringExtra("error")
            if (!msg.isNullOrBlank()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupClickListeners()
        autoSkipIfLoggedIn()
    }

    private fun setupClickListeners() {
        binding.btnKakao.setOnClickListener {
            openSocialWeb("KAKAO")
        }

        binding.btnNaver.setOnClickListener {
            openSocialWeb("NAVER")
        }

        binding.btnGoogle.setOnClickListener {
            Toast.makeText(context, "구글 로그인은 아직 지원되지 않습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSocialWeb(provider: String) {
        val intent = Intent(requireContext(), SocialLoginBridgeActivity::class.java)
        intent.putExtra("provider", provider)
        socialLoginLauncher.launch(intent)
    }

    private fun autoSkipIfLoggedIn() {
        val jwt = JwtStore.load(requireContext())
        if (jwt.isNullOrBlank()) return

        vm.fetchProfileCompleted { completed, _ ->
            if (!isAdded) return@fetchProfileCompleted
            requireActivity().runOnUiThread {
                if (completed == true) {
                    moveToHome()
                }
            }
        }
    }

    private fun moveToProfileCreate() {
        findNavController().navigate(R.id.action_login_to_profileCreate)
    }

    private fun moveToHome() {
        findNavController().navigate(
            R.id.homeFragment,
            null,
            androidx.navigation.navOptions {
                popUpTo(R.id.nav_graph) { inclusive = true }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
