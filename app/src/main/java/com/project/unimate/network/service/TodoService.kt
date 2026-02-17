package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

interface TodoService {

    @GET("api/teams/{teamId}/todos")
    suspend fun getTeamTodos(
        @Path("teamId") teamId: Long,
        @Query("date") date: String
    ): Response<TeamTodosByDateResponse>

    @POST("api/teams/{teamId}/my-todos")
    suspend fun createMyTodo(
        @Path("teamId") teamId: Long,
        @Body request: TodoCreateRequest
    ): Response<Unit>

    @PATCH("api/teams/{teamId}/my-todos/{todoId}")
    suspend fun updateMyTodoCompleted(
        @Path("teamId") teamId: Long,
        @Path("todoId") todoId: Long,
        @Body request: TodoCompleteRequest
    ): Response<Unit>
}
