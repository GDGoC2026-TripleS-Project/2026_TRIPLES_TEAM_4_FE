package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface FcmService {

    @POST("api/v1/fcm/token/me")
    suspend fun registerMyToken(@Body request: FcmTokenRegisterRequest): Response<Unit>

    @POST("api/v1/fcm/test/me")
    suspend fun testMe(@Body request: FcmSendRequest): Response<FcmTestResponse>
}
