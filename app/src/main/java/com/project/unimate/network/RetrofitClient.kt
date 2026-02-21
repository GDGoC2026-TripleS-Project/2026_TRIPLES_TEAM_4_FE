package com.project.unimate.network

// 역할: Retrofit 싱글톤 빌드. AuthInterceptor·TokenAuthenticator·Gson(TeamsListResponse 디시리얼라이저) 적용

import android.content.Context
import com.google.gson.GsonBuilder
import com.project.unimate.network.dto.TeamsListResponse
import com.project.unimate.network.dto.TeamsListResponseDeserializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object
RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context.applicationContext).also { retrofit = it }
        }
    }

    private fun buildRetrofit(appContext: Context): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(appContext))
            .authenticator(TokenAuthenticator(appContext))
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(TeamsListResponse::class.java, TeamsListResponseDeserializer())
            .create()
        return Retrofit.Builder()
            .baseUrl(Env.BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    inline fun <reified T> create(context: Context): T {
        return getInstance(context).create(T::class.java)
    }
}
