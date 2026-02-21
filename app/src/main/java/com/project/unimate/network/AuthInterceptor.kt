package com.project.unimate.network

// 역할: 모든 API 요청에 JWT를 Authorization Bearer 헤더로 첨부. Content-Type 기본값 application/json

import android.content.Context
import com.project.unimate.auth.JwtStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val appContext: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val jwt = JwtStore.loadAccessToken(appContext)

        val reqBuilder = original.newBuilder()
        if (original.header("Content-Type") == null) {
            reqBuilder.header("Content-Type", "application/json")
        }

        if (!jwt.isNullOrBlank()) {
            reqBuilder.header("Authorization", "Bearer $jwt")
        }

        return chain.proceed(reqBuilder.build())
    }
}
