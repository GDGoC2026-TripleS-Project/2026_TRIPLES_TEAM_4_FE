package com.project.unimate.ui.splash // 나현 님의 패키지명 확인 필수!

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.project.unimate.MainActivity
import com.project.unimate.R
import com.project.unimate.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // 2초 뒤에 실행
        Handler(Looper.getMainLooper()).postDelayed({
            // MainActivity로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // 현재 스플래시 액티비티를 종료하여 뒤로가기 시 다시 나오지 않게 함
            finish()
        }, 2000)
    }
}