package com.project.unimate.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.unimate.databinding.ActivityLoginBinding // 바인딩 클래스 확인

class LoginActivity : AppCompatActivity() {

    // 1. 뷰바인딩 초기화
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. 뷰바인딩 설정
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 전체 화면 설정 (Edge-to-Edge)
        enableEdgeToEdge()

        // 3. 시스템 바 패딩 설정 (R.id.main 에러 방지용)
        // XML 최상단 레이아웃에 android:id="@+id/main" 이 있어야 합니다.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 4. 버튼 클릭 리스너 연결
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // 카카오 로그인
        binding.btnKakao.setOnClickListener {
            startSocialLogin("KAKAO")
        }

        // 네이버 로그인
        binding.btnNaver.setOnClickListener {
            startSocialLogin("NAVER")
        }

        // 구글 로그인
        binding.btnGoogle.setOnClickListener {
            startSocialLogin("GOOGLE")
        }
    }

    /**
     * 소셜 로그인 브릿지 액티비티를 실행하는 공통 함수
     */
    private fun startSocialLogin(provider: String) {
        // 프래그먼트가 아니므로 requireContext() 대신 this를 사용합니다.
        val intent = Intent(this, SocialLoginBridgeActivity::class.java).apply {
            putExtra("provider", provider)
        }
        startActivity(intent)
    }
}