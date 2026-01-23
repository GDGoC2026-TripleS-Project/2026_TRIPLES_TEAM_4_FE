package com.project.unimate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.project.unimate.auth.FcmRegistrar
import com.project.unimate.auth.JwtStore

class MainActivity : ComponentActivity() {

    private val TAG = "UnimateFCM"
    private val BASE_URL = "https://seok-hwan1.duckdns.org"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()
        handlePushIntent(intent)

        // =============================
        // ✅ (테스트용) 스웨거 JWT 임시 주입
        // - 한번만 넣고 앱 재실행해서 확인
        // - 테스트 끝나면 이 블록 통째로 삭제
        // =============================
        val TEST_JWT = "" // <-- Swagger에서 복사한 JWT 붙여넣기
        if (TEST_JWT.isNotBlank()) {
            JwtStore.save(this, TEST_JWT)
            Log.d(TAG, "✅ TEST_JWT injected into JwtStore")
        }

        // ✅ 이미 로그인(JWT 저장)된 상태면 앱 시작하자마자 FCM 등록(갱신)
        val jwt = JwtStore.load(this)
        Log.d(TAG, "JWT exists? ${!jwt.isNullOrBlank()}")

        // ✅ BASE_URL을 넘기는 형태 유지
        FcmRegistrar.registerIfPossible(this, BASE_URL)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        val tv = findViewById<TextView>(R.id.tvLog)
        if (intent == null) return

        val screen = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_SCREEN)
        val alarmId = intent.getStringExtra(UnimateFirebaseMessagingService.EXTRA_PUSH_ALARM_ID)

        if (!screen.isNullOrBlank() || !alarmId.isNullOrBlank()) {
            val msg = "PushClick: screen=$screen alarmId=$alarmId"
            Log.d(TAG, msg)
            tv.text = msg
        } else {
            tv.text = "Hello World!"
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}
