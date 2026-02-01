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
import com.project.unimate.network.Env

class MainActivity : ComponentActivity() {

    // ToDo : 네비게이션바 로직 추가 필요(백파트 코드 건들이지 않기)

    private val TAG = "UnimateFCM"
    private val BASE_URL = Env.BASE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNotificationPermissionIfNeeded()
        handlePushIntent(intent)

        // ✅ 여기 " " 안에 Swagger에서 받은 JWT를 그대로 붙여넣기 (Bearer 붙이지 말 것)
        val TEST_JWT = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJrYWthb180Njk4MjY5NTk4QHVuaW1hdGUubG9jYWwiLCJpYXQiOjE3Njk2MDQ1MjksImV4cCI6MTc2OTYwODEyOX0.Nu1IJqOEFUnsFo8hs3BQuPkhGIKj8chHms4OlzrTDWM"  // ex) "eyJhbGciOiJIUzI1NiJ9...."

        if (TEST_JWT.isNotBlank()) {
            val token = TEST_JWT.trim().removePrefix("Bearer ").trim()
            JwtStore.save(this, token)

            val after = JwtStore.load(this)
            Log.d(TAG, "✅ TEST_JWT injected. afterLoad len=${after?.length ?: 0}, head=${after?.take(12)}")
        } else {
            Log.d(TAG, "TEST_JWT is blank - skip inject (use stored token or login flow)")
        }

        val jwt = JwtStore.load(this)
        Log.d(TAG, "JWT exists? ${!jwt.isNullOrBlank()} len=${jwt?.length ?: 0}")

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
