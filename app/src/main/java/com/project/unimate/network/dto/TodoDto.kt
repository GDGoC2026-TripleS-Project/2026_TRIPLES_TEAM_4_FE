package com.project.unimate.network.dto

// === Request ===

data class TodoCreateRequest(
    val date: String,
    val title: String
)

data class TodoCompleteRequest(
    val completed: Boolean? = true
)

// === Response ===

data class TeamTodosByDateResponse(
    val teamId: Long?,
    val date: String?,
    val items: List<TodoItemResponse>?
)

data class TodoItemResponse(
    val todoId: Long?,
    val userId: Long?,
    val nickname: String?,
    val profileImageUrl: String?,
    val displayColorHex: String?,
    val title: String?,
    val completed: Boolean?
)
