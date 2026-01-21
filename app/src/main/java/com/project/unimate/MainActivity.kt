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
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val TAG = "UnimateFCM"
    private val client = OkHttpClient()

    private val BASE_URL = "https://seok-hwan1.duckdns.org"
    private val TEST_USER_ID = "1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 네 레이아웃 사용 (Hello World TextView 있음)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "앱 시작")

        requestNotificationPermissionIfNeeded()
        fetchFcmTokenAndRegister()

        // ✅ 알림 클릭/딥링크로 들어온 인텐트 처리
        handlePushIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    private fun handlePushIntent(intent: Intent?) {
        if (intent == null) return

        val screen = intent.getStringExtra("push_screen")
        val alarmId = intent.getStringExtra("push_alarm_id")

        val tv = findViewById<TextView>(R.id.tvLog)
        tv.text = "앱 시작(푸시 아님이면 이게 보임)"

        if (screen != null || alarmId != null) {
            val msg = "PushClick: screen=$screen alarmId=$alarmId"
            Log.d(TAG, msg)
            tv.text = msg

            // TODO: 여기서 네가 원하는 화면으로 이동(추후)
            // 예) if (screen == "alarm_detail" && alarmId != null) { ... }
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
            } else {
                Log.d(TAG, "알림 권한: 이미 허용됨")
            }
        } else {
            Log.d(TAG, "알림 권한: Android 13 미만은 런타임 권한 없음")
        }
    }

    private fun fetchFcmTokenAndRegister() {
        Log.d(TAG, "FCM 토큰 요청중...")

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "FCM_TOKEN=$token")
                registerTokenToServer(token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FCM 토큰 발급 실패", e)
            }
    }

    private fun registerTokenToServer(token: String) {
        val url = "$BASE_URL/api/v1/fcm/token"

        val json = """
            {
              "token": "$token",
              "deviceId": "emu-01",
              "platform": "ANDROID"
            }
        """.trimIndent()

        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-USER-ID", TEST_USER_ID)
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "토큰 등록 실패(네트워크)", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                Log.d(TAG, "토큰 등록 응답 code=$code body=$body")

                if (code in 200..299) {
                    Log.d(TAG, "✅ 토큰 등록 성공")
                } else {
                    Log.e(TAG, "❌ 토큰 등록 실패(서버 응답 비정상)")
                }
            }
        })
    }
}
