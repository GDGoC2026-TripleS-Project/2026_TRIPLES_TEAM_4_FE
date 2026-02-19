package com.project.unimate.network.service

import retrofit2.Response
import retrofit2.http.*

interface SystemService {

    @GET("api/system/health")
    suspend fun health(): Response<Map<String, Any>>
}
