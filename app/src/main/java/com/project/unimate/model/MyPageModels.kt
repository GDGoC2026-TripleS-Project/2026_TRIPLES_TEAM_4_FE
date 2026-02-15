package com.project.unimate.model

data class MyPageResponse(
    val profile: MyPageProfile,
    val activeTeams: List<MyPageTeam>,
    val completedTeams: List<MyPageTeam>
)

data class MyPageProfile(
    val userId: Long,
    val nickname: String,
    val email: String,
    val profileImageUrl: String?
)

data class MyPageTeam(
    val teamId: Long,
    val name: String,
    val description: String,
    val memberCount: Int,
    val startAt: String,
    val endAt: String,
    val isCompleted: Boolean,
    val color: String?,
    val colorHex: String?,
    val dday: String?
)