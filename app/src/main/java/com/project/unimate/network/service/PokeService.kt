package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface PokeService {

    @POST("api/pokes")
    suspend fun sendPokes(@Body request: PokeRequest): Response<PokeResponse>

    @GET("api/pokes/targets")
    suspend fun getTargets(): Response<PokeTargetsResponse>

    @GET("api/pokes/messages")
    suspend fun getMessages(): Response<List<PokeMessageResponse>>
}
