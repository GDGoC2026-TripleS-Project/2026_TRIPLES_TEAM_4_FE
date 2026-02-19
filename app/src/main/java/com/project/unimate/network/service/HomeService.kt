package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface HomeService {

    @GET("api/home")
    suspend fun getHomeSummary(
        @Query("date") date: String? = null,
        @Query("teamIds") teamIds: List<Long>? = null,
        @Query("includeMyPersonal") includeMyPersonal: Boolean? = true
    ): Response<HomeSummaryResponse>
}
