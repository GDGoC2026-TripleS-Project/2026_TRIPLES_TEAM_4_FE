package com.project.unimate.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ApiClient {
    val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
