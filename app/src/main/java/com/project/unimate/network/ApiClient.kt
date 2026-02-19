package com.project.unimate.network

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
