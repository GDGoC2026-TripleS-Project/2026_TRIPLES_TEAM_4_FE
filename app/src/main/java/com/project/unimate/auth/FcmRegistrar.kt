package com.project.unimate.auth

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object FcmRegistrar {

    private const val TAG = "UnimateFCM"
    private val client = OkHttpClient()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * ✅ JWT + FCM_TOKEN 있을 때만 token/me 호출
     * - baseUrl: MainActivity에서 주입
     * - deviceId: 앱 설치 UUID (SharedPreferences)
     * - platform: ANDROID
     */
    fun registerIfPossible(context: Context, baseUrl: String) {
        val jwt = JwtStore.load(context)
        if (jwt.isNullOrBlank()) {
            Log.d(TAG, "FCM register skip: JWT empty")
            return
        }

        val deviceId = DeviceIdProvider.getOrCreate(context)
        val platform = "ANDROID"

        // ✅✅✅ 요구사항: deviceId 로그 출력 (여기서 항상 확인 가능)
        Log.d(TAG, "DEVICE_ID=$deviceId")

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcm ->
                logTokenForDebug(context, fcm)

                val url = "${baseUrl.trimEnd('/')}/api/v1/fcm/token/me"
                val json = JSONObject()
                    .put("token", fcm)
                    .put("deviceId", deviceId)
                    .put("platform", platform)
                    .toString()

                val req = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer $jwt")
                    .post(json.toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                client.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "FCM token register failed(network)", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val code = response.code
                        val body = response.body?.string()
                        response.close()
                        Log.d(TAG, "FCM token register resp code=$code body=${body ?: ""}")
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FCM token fetch failed", e)
            }
    }

    private fun logTokenForDebug(context: Context, token: String) {
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (debuggable) {
            Log.d(TAG, "FCM_TOKEN_FULL=$token")
        } else {
            Log.d(TAG, "FCM_TOKEN=${token.take(12)}...")
        }
    }
}
