package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface MemberStatusService {

    @GET("api/teams/{teamId}/members/busy-now")
    suspend fun getBusyNow(
        @Path("teamId") teamId: Long
    ): Response<TeamMemberBusyNowResponse>

    @GET("api/teams/{teamId}/members/idle")
    suspend fun getIdleMembers(
        @Path("teamId") teamId: Long,
        @Query("date") date: String
    ): Response<List<TeamIdleMemberResponse>>
}
