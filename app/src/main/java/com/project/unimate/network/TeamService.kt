package com.project.unimate.network

import com.project.unimate.model.CalendarDayResponse
import com.project.unimate.model.CreateScheduleRequest
import com.project.unimate.model.CreateTeamRequest
import com.project.unimate.model.JoinTeamRequest
import com.project.unimate.model.JoinTeamResponse
import com.project.unimate.model.ScheduleDetail
import com.project.unimate.model.TeamCreateResponse
import com.project.unimate.model.TeamDetailResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface TeamService {
    @POST("api/teams")
    fun createTeam(@Body request: CreateTeamRequest): Call<TeamCreateResponse>

    @POST("api/teams/join")
    fun joinTeam(
        @Body request: JoinTeamRequest
    ): Call<JoinTeamResponse>

    @retrofit2.http.GET("api/teams")
    fun getMyTeams(): retrofit2.Call<List<com.project.unimate.model.TeamSummary>>

    @retrofit2.http.GET("api/teams/colors/available")
    fun getAvailableColors(): retrofit2.Call<List<com.project.unimate.model.AvailableColor>>

    @retrofit2.http.DELETE("api/teams/{teamId}")
    fun deleteTeam(
        @retrofit2.http.Path("teamId") teamId: Long
    ): retrofit2.Call<Unit>

    @retrofit2.http.GET("api/teams/{teamId}")
    fun getTeamDetail(
        @retrofit2.http.Path("teamId") teamId: Long
    ): retrofit2.Call<TeamDetailResponse>

    @retrofit2.http.GET("api/teams/{teamId}/team-schedules/day")
    fun getTeamSchedulesDaily(
        @retrofit2.http.Path("teamId") teamId: Long,
        @retrofit2.http.Query("date") date: String
    ): retrofit2.Call<List<ScheduleDetail>>

    @retrofit2.http.GET("api/teams/{teamId}/my-schedules/day")
    fun getMySchedulesDaily(
        @retrofit2.http.Path("teamId") teamId: Long,
        @retrofit2.http.Query("date") date: String
    ): retrofit2.Call<List<ScheduleDetail>>

    @retrofit2.http.GET("api/teams/{teamId}/my-schedules")
    fun getMySchedulesMonthly(
        @retrofit2.http.Path("teamId") teamId: Long,
        @retrofit2.http.Query("from") from: String,
        @retrofit2.http.Query("to") to: String
    ): retrofit2.Call<List<ScheduleDetail>>

    @retrofit2.http.POST("api/teams/{teamId}/team-schedules")
    fun createTeamSchedule(
        @retrofit2.http.Path("teamId") teamId: Long,
        @retrofit2.http.Body request: CreateScheduleRequest
    ): retrofit2.Call<ScheduleDetail>

    @retrofit2.http.GET("api/teams/{teamId}/team-schedules")
    fun getTeamSchedulesMonthly(
        @retrofit2.http.Path("teamId") teamId: Long,
        @retrofit2.http.Query("from") from: String,
        @retrofit2.http.Query("to") to: String
    ): retrofit2.Call<List<ScheduleDetail>>


    @retrofit2.http.POST("api/teams/{teamId}/invite-code")
    fun issueInviteCode(
        @retrofit2.http.Path("teamId") teamId: Long
    ): retrofit2.Call<com.project.unimate.model.InviteCodeResponse>

    @retrofit2.http.PUT("api/teams/{teamId}")
    fun updateTeam(
        @retrofit2.http.Path("teamId") teamId: Long,
        @retrofit2.http.Body request: com.project.unimate.model.UpdateTeamRequest
    ): retrofit2.Call<retrofit2.Response<Unit>>


    @retrofit2.http.GET("api/teams/{teamId}/members")
    fun getTeamMembers(
        @retrofit2.http.Path("teamId") teamId: Long
    ): retrofit2.Call<List<com.project.unimate.model.TeamMember>>




    @retrofit2.http.DELETE("api/teams/{teamId}/leave")
    fun leaveTeam(
        @retrofit2.http.Path("teamId") teamId: Long
    ): retrofit2.Call<Unit>








}