package com.project.unimate.network.dto

import com.google.gson.annotations.SerializedName

// === Request ===

data class PokeRequest(
    val messageId: Long,
    val targets: List<PokeTarget>
)

data class PokeTarget(
    val teamId: Long,
    val userId: Long
)

// === Response ===

data class PokeResponse(
    val sentCount: Int?,
    val excludedSelfCount: Int?,
    val invalidTargets: List<InvalidTarget>?
)

data class InvalidTarget(
    val teamId: Long?,
    val userId: Long?,
    val reason: String?
)

data class PokeTargetsResponse(
    @SerializedName(value = "teams", alternate = ["data", "content"])
    val teams: List<PokeTeamSection>?
)

data class PokeTeamSection(
    @SerializedName(value = "teamId", alternate = ["id"])
    val teamId: Long?,
    @SerializedName(value = "teamName", alternate = ["name"])
    val teamName: String?,
    @SerializedName(value = "members", alternate = ["users", "targets"])
    val members: List<PokeMember>?
)

data class PokeMember(
    @SerializedName(value = "userId", alternate = ["id", "memberId"])
    val userId: Long?,
    @SerializedName(value = "nickname", alternate = ["name", "userName"])
    val nickname: String?,
    val profileImageUrl: String?
)

data class PokeMessageResponse(
    val messageId: Long?,
    val content: String?
)
