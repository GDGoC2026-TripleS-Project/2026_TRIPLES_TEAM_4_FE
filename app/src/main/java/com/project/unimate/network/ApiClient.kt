package com.project.unimate.network

// 역할: OkHttp 공용 클라이언트(인증·리프레시 적용). auth 패키지 등 비 Retrofit 호출용

import com.project.unimate.UnimateApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    val http: OkHttpClient by lazy {
        val appContext = UnimateApp.instance.applicationContext
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(appContext))
            .authenticator(TokenAuthenticator(appContext))
            .build()
    }
}
