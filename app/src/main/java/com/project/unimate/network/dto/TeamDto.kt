package com.project.unimate.network.dto

import com.google.gson.annotations.SerializedName

// === Request ===

data class TeamCreateRequest(
    val name: String,
    val description: String? = null,
    val color: String,
    val startAt: String,
    val endAt: String
)

data class TeamUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val startAt: String? = null,
    val endAt: String? = null
)

data class TeamJoinRequest(
    val inviteCode: String
)

// === Response ===

data class TeamResponse(
    val id: Long?,
    val name: String?,
    val description: String?,
    val color: String?,
    val colorHex: String?,
    val ownerUserId: Long?,
    val startAt: String?,
    val endAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val completed: Boolean?,
    val isCompleted: Boolean?
)

data class TeamSummaryResponse(
    val id: Long?,
    val name: String?,
    val description: String?,
    val color: String?,
    val colorHex: String?,
    val myRole: String?,
    val memberCount: Long?,
    val startAt: String?,
    val endAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val completed: Boolean?,
    val isCompleted: Boolean?
)

data class TeamDetailResponse(
    val team: TeamResponse?,
    val members: List<TeamMemberResponse>?,
    val myRole: String?,
    val memberCount: Int?,
    val myDisplayColor: String?,
    val myDisplayColorHex: String?
)

data class TeamMemberResponse(
    val userId: Long?,
    val nickname: String?,
    val profileImageUrl: String?,
    val universityName: String?,
    val role: String?,
    val joinedAt: String?,
    val displayColor: String?,
    val displayColorHex: String?
)

data class TeamInviteCodeResponse(
    val teamId: Long?,
    val inviteCode: String?,
    val expiresAt: String?
)

data class TeamJoinResponse(
    val team: TeamResponse?,
    val myRole: String?,
    val memberCount: Int?,
    val members: List<TeamMemberResponse>?,
    val myDisplayColor: String?,
    val myDisplayColorHex: String?
)

data class TeamColorResponse(
    val name: String?,
    val hex: String?
)
