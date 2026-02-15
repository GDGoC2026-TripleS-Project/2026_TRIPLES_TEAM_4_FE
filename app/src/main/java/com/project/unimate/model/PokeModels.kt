package com.project.unimate.model

import com.google.gson.annotations.SerializedName


data class PokeTargetsResponse(
    @SerializedName("teams") val teams: List<PokeTeam>
)

data class PokeTeam(
    @SerializedName("teamId") val teamId: Long,
    @SerializedName("teamName") val teamName: String,
    @SerializedName("members") val members: List<PokeMember>
)

data class PokeMember(
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String,
    // 명세서상의 필드명인 profileImageUrl로 수정
    @SerializedName("profileImageUrl") val profileImageUrl: String?
)

data class PokeRequest(
    @SerializedName("messageId") val messageId: Long,
    @SerializedName("targets") val targets: List<PokeTarget>
)

data class PokeTarget(
    @SerializedName("teamId") val teamId: Long,
    @SerializedName("userId") val userId: Long
)

data class PokeResponse(
    @SerializedName("sentCount") val sentCount: Int,
    @SerializedName("excludedSelfCount") val excludedSelfCount: Int,
    @SerializedName("invalidTargets") val invalidTargets: List<InvalidTarget>
)

data class InvalidTarget(
    @SerializedName("teamId") val teamId: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("reason") val reason: String
)

data class PokeMessageResponse(
    @SerializedName("messageId") val messageId: Long,
    @SerializedName("content") val content: String
)