package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MyScheduleService {

    @GET("api/teams/{teamId}/my-schedules")
    suspend fun getMarkedDates(
        @Path("teamId") teamId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<MyScheduleMarkingResponse>

    @GET("api/teams/{teamId}/my-schedules/day")
    suspend fun getDaySchedules(
        @Path("teamId") teamId: Long,
        @Query("date") date: String
    ): Response<List<MyScheduleResponse>>

    @GET("api/teams/{teamId}/my-schedules/now")
    suspend fun getNowStatus(
        @Path("teamId") teamId: Long
    ): Response<MyScheduleNowResponse>

    @POST("api/teams/{teamId}/my-schedules")
    suspend fun create(
        @Path("teamId") teamId: Long,
        @Body request: MyScheduleCreateRequest
    ): Response<MyScheduleResponse>

    @PUT("api/teams/{teamId}/my-schedules/{scheduleId}")
    suspend fun update(
        @Path("teamId") teamId: Long,
        @Path("scheduleId") scheduleId: Long,
        @Body request: MyScheduleUpdateRequest
    ): Response<MyScheduleResponse>

    @DELETE("api/teams/{teamId}/my-schedules/{scheduleId}")
    suspend fun delete(
        @Path("teamId") teamId: Long,
        @Path("scheduleId") scheduleId: Long
    ): Response<Unit>
}
