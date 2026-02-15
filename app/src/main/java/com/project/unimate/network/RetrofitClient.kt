package com.project.unimate.network

import android.content.Context
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://unimate-alb-274308250.ap-northeast-2.elb.amazonaws.com/"

    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): Retrofit {
        if (retrofit == null) {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request()
                val spf = context.getSharedPreferences("unimate_auth", Context.MODE_PRIVATE)
                val token = spf.getString("jwt", null)

                val newRequest = request.newBuilder().apply {
                    if (token != null) addHeader("Authorization", "Bearer $token")
                }.build()

                Log.d("API_DEBUG", ">>> 요청: ${newRequest.method} ${newRequest.url}")
                val buffer = Buffer()
                newRequest.body?.writeTo(buffer)
                Log.d("API_DEBUG", ">>> 데이터: ${buffer.readUtf8()}")

                val response = chain.proceed(newRequest)

                if (!response.isSuccessful) {
                    val errorBody = response.peekBody(Long.MAX_VALUE).string()
                    Log.e("API_DEBUG", "<<< 실패 코드: ${response.code}")
                    Log.e("API_DEBUG", "<<< 실패 원인: $errorBody")
                } else {
                    Log.d("API_DEBUG", "<<< 성공 코드: ${response.code}")
                }

                response
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}