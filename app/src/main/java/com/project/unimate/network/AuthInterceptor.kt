package com.project.unimate.network

import android.content.Context
import com.project.unimate.auth.JwtStore
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val appContext: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val jwt = JwtStore.load(appContext)

        val reqBuilder = original.newBuilder()
            .header("Content-Type", "application/json")

        if (!jwt.isNullOrBlank()) {
            reqBuilder.header("Authorization", "Bearer $jwt")
        }

        return chain.proceed(reqBuilder.build())
    }
}
