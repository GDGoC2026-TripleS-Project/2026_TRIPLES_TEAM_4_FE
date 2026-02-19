package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface NotificationService {

    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<NotificationItemResponse>>

    @POST("api/notifications/{notificationId}/complete")
    suspend fun complete(
        @Path("notificationId") notificationId: Long
    ): Response<CompletionResponse>

    @PATCH("api/notifications/{notificationId}/read")
    suspend fun markRead(
        @Path("notificationId") notificationId: Long
    ): Response<NotificationItemResponse>

    @PATCH("api/notifications/{notificationId}/action-done")
    suspend fun actionDone(
        @Path("notificationId") notificationId: Long
    ): Response<NotificationItemResponse>

    @PATCH("api/notifications/read-all")
    suspend fun readAll(): Response<Map<String, Int>>

    @POST("api/notifications/dday-test/run")
    suspend fun runDdayTest(): Response<Map<String, String>>
}
