package com.project.unimate.network

import com.project.unimate.model.CalendarDayResponse
import com.project.unimate.model.CalendarMonthResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CalendarService {
    @GET("api/calendar/month")
    fun getMonthlyDayCounts(
        @Query("month") month: String,
        @Query("teamIds") teamIds: List<Int>?,
        @Query("includeMyPersonal") includeMyPersonal: Boolean = true
    ): Call<CalendarMonthResponse>

    @GET("api/calendar/day")
    fun getDaySchedules(
        @Query("date") date: String,
        @Query("teamIds") teamIds: List<Int>? = null,
        @Query("includeMyPersonal") includeMyPersonal: Boolean = true
    ): Call<CalendarDayResponse>

}