package com.project.unimate.network

import com.project.unimate.model.PokeMessageResponse
import com.project.unimate.model.PokeRequest
import com.project.unimate.model.PokeResponse
import com.project.unimate.model.PokeTargetsResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PokeService {

    @GET("api/pokes/targets")
    fun getPokeTargets(): Call<PokeTargetsResponse>


    /** 찌르기 문구 목록 조회 */
    @GET("api/pokes/messages")
    fun getPokeMessages(): Call<List<PokeMessageResponse>>

    /** 찌르기 전송 */
    @POST("api/pokes")
    fun sendPoke(@Body request: PokeRequest): Call<PokeResponse>
}