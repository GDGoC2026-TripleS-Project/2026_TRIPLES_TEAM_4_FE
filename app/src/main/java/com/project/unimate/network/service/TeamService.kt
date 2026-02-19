package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TeamService {

    @GET("api/teams")
    suspend fun getMyTeams(): Response<List<TeamSummaryResponse>>

    @POST("api/teams")
    suspend fun createTeam(@Body request: TeamCreateRequest): Response<TeamResponse>

    @GET("api/teams/{teamId}")
    suspend fun getTeamDetail(@Path("teamId") teamId: Long): Response<TeamDetailResponse>

    @PUT("api/teams/{teamId}")
    suspend fun updateTeam(
        @Path("teamId") teamId: Long,
        @Body request: TeamUpdateRequest
    ): Response<TeamResponse>

    @DELETE("api/teams/{teamId}")
    suspend fun deleteTeam(@Path("teamId") teamId: Long): Response<Unit>

    @DELETE("api/teams/{teamId}/leave")
    suspend fun leaveTeam(@Path("teamId") teamId: Long): Response<Unit>

    @GET("api/teams/{teamId}/members")
    suspend fun getMembers(@Path("teamId") teamId: Long): Response<List<TeamMemberResponse>>

    @POST("api/teams/{teamId}/invite-code")
    suspend fun issueInviteCode(@Path("teamId") teamId: Long): Response<TeamInviteCodeResponse>

    @GET("api/teams/{teamId}/invite")
    suspend fun getInviteCode(@Path("teamId") teamId: Long): Response<TeamInviteCodeResponse>

    @POST("api/teams/join")
    suspend fun joinTeam(@Body request: TeamJoinRequest): Response<TeamJoinResponse>

    @GET("api/teams/colors/available")
    suspend fun getAvailableColors(): Response<List<TeamColorResponse>>
}
