package com.project.unimate.model

import com.google.gson.annotations.SerializedName

data class JoinTeamRequest(
    @SerializedName("inviteCode") val inviteCode: String
)


data class JoinTeamResponse(
    @SerializedName("team") val team: TeamInfo,
    @SerializedName("myRole") val myRole: String,
    @SerializedName("memberCount") val memberCount: Int,
    @SerializedName("members") val members: List<TeamMember>,
    @SerializedName("myDisplayColor") val myDisplayColor: String,
    @SerializedName("myDisplayColorHex") val myDisplayColorHex: String
)


data class TeamMember(
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImageUrl") val profileImageUrl: String?,
    @SerializedName("universityName") val universityName: String?,
    @SerializedName("role") val role: String,
    @SerializedName("joinedAt") val joinedAt: String,
    @SerializedName("displayColor") val displayColor: String,
    @SerializedName("displayColorHex") val displayColorHex: String
)