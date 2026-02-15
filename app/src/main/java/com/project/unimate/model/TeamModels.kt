package com.project.unimate.model

import com.google.gson.annotations.SerializedName

/**
 * 팀 관련 통합 데이터 모델
 */

// 1. 팀 목록 및 요약 정보
data class TeamSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val colorHex: String?,
    val myRole: String,
    val memberCount: Int,
    val startAt: String?,
    val endAt: String?,
    val isCompleted: Boolean
)

// 2. 팀 생성 관련
data class CreateTeamRequest(
    val name: String,
    val description: String,
    val color: String,
    val startAt: String,
    val endAt: String
)

data class TeamCreateResponse(
    @SerializedName("team") val team: CreatedTeamDetail?,
    @SerializedName("myRole") val myRole: String?
)

data class CreatedTeamDetail(
    @SerializedName("id") val id: Long
)

// 3. 팀 상세 정보 조회 관련
data class TeamDetailResponse(
    val team: TeamInfo,
    val members: List<TeamMember>,
    val myRole: String,
    val memberCount: Int,
    val myDisplayColor: String?,
    val myDisplayColorHex: String?
) {
    val isLeader: Boolean
        get() = myRole == "LEADER"
}

data class TeamInfo(
    val id: Long,
    val name: String,
    val description: String?,
    val color: String?,
    val colorHex: String?,
    val startAt: String?,
    val endAt: String?,
    val isCompleted: Boolean
)

// 4. 팀 수정 및 초대 코드
data class UpdateTeamRequest(
    val name: String?,
    val description: String?,
    val startAt: String?,
    val endAt: String?
)

data class InviteCodeResponse(
    @SerializedName("teamId") val teamId: Long,
    @SerializedName("inviteCode") val inviteCode: String,
    @SerializedName("expiresAt") val expiresAt: String
)

