package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TeamScheduleService {

    @GET("api/teams/{teamId}/team-schedules")
    suspend fun getByRange(
        @Path("teamId") teamId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<List<TeamScheduleResponse>>

    @GET("api/teams/{teamId}/team-schedules/day")
    suspend fun getByDay(
        @Path("teamId") teamId: Long,
        @Query("date") date: String
    ): Response<List<TeamScheduleResponse>>

    @POST("api/teams/{teamId}/team-schedules")
    suspend fun create(
        @Path("teamId") teamId: Long,
        @Body request: TeamScheduleCreateRequest
    ): Response<TeamScheduleResponse>

    @PUT("api/teams/{teamId}/team-schedules/{scheduleId}")
    suspend fun update(
        @Path("teamId") teamId: Long,
        @Path("scheduleId") scheduleId: Long,
        @Body request: TeamScheduleUpdateRequest
    ): Response<TeamScheduleResponse>

    @DELETE("api/teams/{teamId}/team-schedules/{scheduleId}")
    suspend fun delete(
        @Path("teamId") teamId: Long,
        @Path("scheduleId") scheduleId: Long
    ): Response<Unit>
}
