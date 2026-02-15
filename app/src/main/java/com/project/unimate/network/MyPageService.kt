package com.project.unimate.network

import com.project.unimate.model.MyPageResponse
import retrofit2.Call
import retrofit2.http.GET

interface MyPageService {
    @GET("api/mypage")
    fun getMyPageSummary(): Call<MyPageResponse>
}