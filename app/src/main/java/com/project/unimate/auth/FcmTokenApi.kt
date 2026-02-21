package com.project.unimate.auth

// 역할: FCM token/me API용 Request 빌더(jwt, deviceId, platform)

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class FcmTokenApi(private val baseUrl: String) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun registerMyToken(jwt: String, fcmToken: String, deviceId: String, platform: String): Request {
        val body = JSONObject()
            .put("token", fcmToken)
            .put("deviceId", deviceId)
            .put("platform", platform)
            .toString()
            .toRequestBody(JSON)

        return Request.Builder()
            .url("$baseUrl/api/v1/fcm/token/me")
            .addHeader("Authorization", "Bearer $jwt")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
    }
}
