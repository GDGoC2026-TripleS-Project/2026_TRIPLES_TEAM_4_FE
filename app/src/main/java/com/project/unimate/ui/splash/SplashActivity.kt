<<<<<<< HEAD
package com.project.unimate.ui.splash // 나현 님의 패키지명 확인 필수!

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.project.unimate.MainActivity
import com.project.unimate.R
import com.project.unimate.ui.login.LoginActivity
=======
//깃허브에 폴더 구조를 올리기 위해 임시로 만들어둔 파일입니다.
//개발 과정에 따라 파일을 삭제하거나 파일명을 변경해도 됩니다.
// 파일명 수정 시 연결된 xml 파일명도 수정 필요

package com.project.unimate.ui.splash

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.project.unimate.R
>>>>>>> main

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
<<<<<<< HEAD

        // 2초 뒤에 실행
        Handler(Looper.getMainLooper()).postDelayed({
            // MainActivity로 이동
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

            // 현재 스플래시 액티비티를 종료하여 뒤로가기 시 다시 나오지 않게 함
            finish()
        }, 2000)
=======
>>>>>>> main
    }
}