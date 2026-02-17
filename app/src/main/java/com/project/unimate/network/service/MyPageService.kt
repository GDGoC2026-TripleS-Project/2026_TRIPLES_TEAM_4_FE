package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MyPageService {

    @GET("api/mypage")
    suspend fun getSummary(): Response<MyPageSummaryResponse>
}
