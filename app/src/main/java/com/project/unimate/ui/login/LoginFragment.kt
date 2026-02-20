package com.project.unimate.ui.login

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import com.project.unimate.auth.JwtStore
import com.project.unimate.auth.LoginViewModel
import com.project.unimate.data.repository.ServerSync
import com.project.unimate.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val vm: LoginViewModel by viewModels()
    private var googleSignInClient: GoogleSignInClient? = null

    private val socialLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleAuthSuccess()
                return@registerForActivityResult
            }

            val msg = result.data?.getStringExtra("error")
            if (!msg.isNullOrBlank()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

    private val googleLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (data == null) {
                Toast.makeText(context, "구글 로그인 응답이 없습니다", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(context, "구글 토큰을 가져오지 못했습니다", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                vm.socialLogin("GOOGLE", idToken) { ok, err ->
                    if (!isAdded) return@socialLogin
                    requireActivity().runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        if (ok) {
                            handleAuthSuccess()
                        } else {
                            Toast.makeText(context, err ?: "구글 로그인 실패", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ApiException) {
                val message = if (e.statusCode == 12501) {
                    "구글 로그인이 취소되었습니다"
                } else {
                    "구글 로그인 실패: ${e.statusCode}"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLoginBinding.bind(view)

        setupGoogleSignIn()
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
            startGoogleLogin()
        }
    }

    private fun setupGoogleSignIn() {
        val webClientId = firstNonBlank(
            getSafeString("google_web_client_id"),
            getSafeString("default_web_client_id")
        )

        if (webClientId.isNullOrBlank()) {
            googleSignInClient = null
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(webClientId)
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    private fun startGoogleLogin() {
        val client = googleSignInClient
        if (client == null) {
            Toast.makeText(context, "구글 로그인 설정이 누락되었습니다", Toast.LENGTH_SHORT).show()
            return
        }
        googleLoginLauncher.launch(client.signInIntent)
    }

    private fun getSafeString(name: String): String? {
        val stringId = resources.getIdentifier(name, "string", requireContext().packageName)
        if (stringId == 0) return null
        return try {
            getString(stringId).takeIf { it.isNotBlank() }
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private fun openSocialWeb(provider: String) {
        val intent = Intent(requireContext(), SocialLoginBridgeActivity::class.java)
        intent.putExtra("provider", provider)
        socialLoginLauncher.launch(intent)
    }

    private fun handleAuthSuccess() {
        // 로그인 성공 → 프로필 완료 여부 확인 후 이동
        vm.fetchProfileCompleted { completed, err ->
            if (!isAdded) return@fetchProfileCompleted
            // 콜백이 OkHttp 백그라운드 스레드에서 옴 → 반드시 Main에서 sync/이동
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread
                when (completed) {
                    true -> runSyncThen { moveToHome() }
                    false -> runSyncThen { moveToProfileCreate() }
                    null -> {
                        if (!err.isNullOrBlank()) {
                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun autoSkipIfLoggedIn() {
        val jwt = JwtStore.load(requireContext())
        if (jwt.isNullOrBlank()) return

        vm.fetchProfileCompleted { completed, _ ->
            if (!isAdded) return@fetchProfileCompleted
            requireActivity().runOnUiThread {
                if (isAdded && completed == true) runSyncThen { moveToHome() }
            }
        }
    }

    /** 로그인 직후 서버에서 유저정보·팀·일정 전부 불러온 뒤 화면 전환 */
    private fun runSyncThen(onDone: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            ServerSync.syncFromServer(requireContext().applicationContext)
            if (isAdded) {
                requireActivity().runOnUiThread { onDone() }
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
