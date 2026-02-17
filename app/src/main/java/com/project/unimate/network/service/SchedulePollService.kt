package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface SchedulePollService {

    @POST("api/schedule-polls")
    suspend fun create(@Body request: SchedulePollCreateRequest): Response<SchedulePollCreateResponse>

    @GET("api/schedule-polls/{pollId}")
    suspend fun getDetail(@Path("pollId") pollId: Long): Response<SchedulePollDetailResponse>

    @PUT("api/schedule-polls/{pollId}")
    suspend fun updateMeta(
        @Path("pollId") pollId: Long,
        @Body request: SchedulePollMetaUpdateRequest
    ): Response<Unit>

    @DELETE("api/schedule-polls/{pollId}")
    suspend fun delete(@Path("pollId") pollId: Long): Response<Unit>

    @PUT("api/schedule-polls/{pollId}/votes/me")
    suspend fun upsertMyVote(
        @Path("pollId") pollId: Long,
        @Body request: SchedulePollVoteUpsertRequest
    ): Response<Unit>

    @PATCH("api/schedule-polls/{pollId}/fix")
    suspend fun fix(
        @Path("pollId") pollId: Long,
        @Body request: SchedulePollFixRequest
    ): Response<Unit>

    @GET("api/teams/{teamId}/schedule-polls/fixed")
    suspend fun getFixedRange(
        @Path("teamId") teamId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): Response<List<TeamFixedSchedulePollSummaryResponse>>

    @GET("api/teams/{teamId}/schedule-polls/fixed/day")
    suspend fun getFixedDay(
        @Path("teamId") teamId: Long,
        @Query("date") date: String
    ): Response<List<TeamFixedSchedulePollSummaryResponse>>
}
