package com.project.unimate

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val TAG = "UnimateFCM"
    private val client = OkHttpClient()

    private val BASE_URL = "https://seok-hwan1.duckdns.org"

    // ✅ 데모용: 이메일 로그인(실제 앱에선 로그인 화면에서 받기)
    private val DEMO_EMAIL = "skhu202214139@gmail.com"
    private val DEMO_PASSWORD = "sh0308141***" // 백엔드 패턴 맞춰야 함

    private var cachedFcmToken: String? = null
    private var cachedJwt: String? = null

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "앱 시작")

        requestNotificationPermissionIfNeeded()

        // 1) 앱 시작 시 FCM 토큰만 받아둠 (등록은 로그인 후)
        fetchFcmTokenOnly()

        // 2) (데모) 앱 시작하자마자 로그인 시도 → 성공하면 JWT 저장 후 토큰 등록
        loginThenRegisterIfPossible()

        // (선택) 푸시 클릭 인텐트 처리 필요하면 유지
        handlePushIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    // ===== (선택) 푸시 클릭 처리 =====
    private fun handlePushIntent(intent: Intent?) {
        if (intent == null) return

        val screen = intent.getStringExtra("push_screen")
        val alarmId = intent.getStringExtra("push_alarm_id")

        if (!screen.isNullOrBlank() || !alarmId.isNullOrBlank()) {
            Log.d(TAG, "PushClick: screen=$screen alarmId=$alarmId")
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

    /**
     * ✅ A안 핵심: 앱 시작 시에는 "FCM 토큰 발급만"
     * 서버 등록은 JWT 확보 후에만 한다.
     */
    private fun fetchFcmTokenOnly() {
        Log.d(TAG, "FCM 토큰 요청중...")

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                cachedFcmToken = token
                logFcmTokenForSwagger(token) // ✅ 여기서 FULL 토큰 찍힘

                // 혹시 JWT가 이미 확보된 상태라면 바로 등록
                tryRegisterTokenIfPossible()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FCM 토큰 발급 실패", e)
            }
    }

    /**
     * ✅ BuildConfig에 의존하지 않고 "진짜 debug 가능 앱인지" 런타임으로 판정
     * - 디버그 가능이면 FULL 토큰을 스웨거에 복사 가능
     * - 릴리즈면 앞 12글자만
     */
    private fun logFcmTokenForSwagger(token: String) {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        Log.d(TAG, "APP_DEBUGGABLE=$debuggable")

        if (debuggable) {
            Log.d(TAG, "FCM_TOKEN_FULL=$token")
        } else {
            Log.d(TAG, "FCM_TOKEN=${token.take(12)}...")
        }
    }

    /**
     * ✅ 데모용 로그인 함수
     * 실제 앱에선 로그인 화면에서 성공 시 받은 token을 cachedJwt에 넣고 tryRegisterTokenIfPossible() 호출.
     */
    private fun loginThenRegisterIfPossible() {
        val url = "$BASE_URL/api/auth/login"

        val json = JSONObject()
            .put("email", DEMO_EMAIL)
            .put("password", DEMO_PASSWORD)
            .toString()

        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "로그인 실패(네트워크)", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                Log.d(TAG, "로그인 응답 code=$code body=$body")

                if (code in 200..299 && !body.isNullOrBlank()) {
                    val token = runCatching { JSONObject(body).optString("token", null) }.getOrNull()

                    if (!token.isNullOrBlank()) {
                        cachedJwt = token
                        Log.d(TAG, "✅ JWT 저장 완료")
                        tryRegisterTokenIfPossible()
                    } else {
                        Log.e(TAG, "❌ 로그인 성공처럼 보이지만 token 파싱 실패")
                    }
                } else {
                    Log.e(TAG, "❌ 로그인 실패(서버 응답 비정상)")
                }
            }
        })
    }

    /**
     * ✅ JWT + FCM_TOKEN 둘 다 있을 때만 서버 등록 호출
     */
    private fun tryRegisterTokenIfPossible() {
        val jwt = cachedJwt
        val fcm = cachedFcmToken

        if (jwt.isNullOrBlank()) {
            Log.d(TAG, "JWT 없음 → 토큰 등록 스킵(A안 정상)")
            return
        }
        if (fcm.isNullOrBlank()) {
            Log.d(TAG, "FCM 토큰 없음 → 토큰 등록 스킵")
            return
        }

        registerTokenToServer(jwt, fcm)
    }

    /**
     * ✅ 백엔드 스펙:
     * POST /api/v1/fcm/token/me
     * Authorization: Bearer <JWT>
     * body: { "token": "..." }
     */
    private fun registerTokenToServer(jwt: String, fcmToken: String) {
        val url = "$BASE_URL/api/v1/fcm/token/me"

        val json = JSONObject()
            .put("token", fcmToken)
            .toString()

        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $jwt")
            .post(json.toRequestBody(JSON_MEDIA_TYPE))
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
