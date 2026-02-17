package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface
CalendarService {

    @GET("api/calendar/month")
    suspend fun getMonth(
        @Query("month") month: String,
        @Query("teamIds") teamIds: List<Long>? = null,
        @Query("includeMyPersonal") includeMyPersonal: Boolean = true
    ): Response<CalendarMonthResponse>

    @GET("api/calendar/day")
    suspend fun getDay(
        @Query("date") date: String,
        @Query("teamIds") teamIds: List<Long>? = null,
        @Query("includeMyPersonal") includeMyPersonal: Boolean = true
    ): Response<CalendarDayResponse>
}
