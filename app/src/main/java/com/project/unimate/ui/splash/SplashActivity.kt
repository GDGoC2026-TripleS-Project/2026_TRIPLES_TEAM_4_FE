package com.project.unimate.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.project.unimate.R
import com.project.unimate.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 2초 뒤에 로그인 화면으로 이동하는 로직
        Handler(Looper.getMainLooper()).postDelayed({
            // 팀 컨벤션에 따라 LoginActivity로 이동하도록 설정
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // 스플래시 화면을 종료하여 사용자가 뒤로가기를 눌러도 다시 보이지 않게 함
            finish()
        }, 2000)
    }
}