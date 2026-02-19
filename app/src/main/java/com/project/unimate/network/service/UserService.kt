package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface UserService {

    @GET("api/users/me")
    suspend fun getMyInfo(): Response<UserResponse>

    @PUT("api/users/me")
    suspend fun upsertProfile(@Body request: ProfileUpsertRequest): Response<UserResponse>

    @Multipart
    @POST("api/users/me/profile-image")
    suspend fun uploadProfileImage(@Part file: MultipartBody.Part): Response<Map<String, String>>

    @GET("api/universities/search")
    suspend fun searchUniversities(
        @Query("q") query: String,
        @Query("limit") limit: Int = 10
    ): Response<List<UniversityItemResponse>>
}
