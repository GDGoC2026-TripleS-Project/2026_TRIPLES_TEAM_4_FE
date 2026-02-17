package com.project.unimate.network.dto

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
    val teams: List<PokeTeamSection>?
)

data class PokeTeamSection(
    val teamId: Long?,
    val teamName: String?,
    val members: List<PokeMember>?
)

data class PokeMember(
    val userId: Long?,
    val nickname: String?,
    val profileImageUrl: String?
)

data class PokeMessageResponse(
    val messageId: Long?,
    val content: String?
)
