package com.project.unimate.network

import com.project.unimate.model.HomeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface HomeService {
    @GET("api/home")
    fun getHomeSummary(
        @Query("date") date: String,
        @Query("includeMyPersonal") includeMyPersonal: Boolean = true
    ): Call<HomeResponse>
}