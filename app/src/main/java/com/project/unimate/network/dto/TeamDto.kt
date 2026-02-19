package com.project.unimate.network.dto

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

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

/** GET /api/teams 응답: 배열 [] 또는 { "content"/"data"/"teams": [] } 둘 다 파싱 */
data class TeamsListResponse(
    val content: List<TeamSummaryResponse>? = null,
    val data: List<TeamSummaryResponse>? = null,
    val teams: List<TeamSummaryResponse>? = null
) {
    fun listOrEmpty(): List<TeamSummaryResponse> = content ?: data ?: teams ?: emptyList()
}

private val listType = object : TypeToken<List<TeamSummaryResponse>>() {}.type

/** 배열/객체 둘 다 파싱하는 디시리얼라이저 */
class TeamsListResponseDeserializer : JsonDeserializer<TeamsListResponse> {
    private val gson = Gson()
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TeamsListResponse {
        return when {
            json.isJsonArray -> TeamsListResponse(content = gson.fromJson(json, listType))
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val content = obj.get("content")?.takeIf { it.isJsonArray }?.let { gson.fromJson<List<TeamSummaryResponse>>(it, listType) }
                val data = obj.get("data")?.takeIf { it.isJsonArray }?.let { gson.fromJson<List<TeamSummaryResponse>>(it, listType) }
                val teams = obj.get("teams")?.takeIf { it.isJsonArray }?.let { gson.fromJson<List<TeamSummaryResponse>>(it, listType) }
                TeamsListResponse(content = content, data = data, teams = teams)
            }
            else -> TeamsListResponse()
        }
    }
}

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
