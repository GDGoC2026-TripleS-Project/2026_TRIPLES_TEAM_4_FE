package com.project.unimate.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthApi(
    private val baseUrl: String
) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun emailLoginRequest(email: String, password: String): Request {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
            .toRequestBody(JSON)

        return Request.Builder()
            .url("$baseUrl/api/auth/login")
            .post(body)
            .build()
    }

    fun kakaoAuthorizeUrlRequest(): Request =
        Request.Builder()
            .url("$baseUrl/api/auth/kakao/authorize-url")
            .get()
            .build()

    fun naverAuthorizeUrlRequest(): Request =
        Request.Builder()
            .url("$baseUrl/api/auth/naver/authorize-url")
            .get()
            .build()

    fun kakaoCallbackRequest(code: String): Request =
        Request.Builder()
            .url("$baseUrl/api/auth/kakao/callback?code=$code")
            .get()
            .build()

    fun naverCallbackRequest(code: String, state: String): Request =
        Request.Builder()
            .url("$baseUrl/api/auth/naver/callback?code=$code&state=$state")
            .get()
            .build()
}
