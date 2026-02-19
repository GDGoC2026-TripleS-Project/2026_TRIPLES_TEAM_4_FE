package com.project.unimate.network.dto

// === Request ===

data class SchedulePollCreateRequest(
    val teamId: Long,
    val dates: List<String>,
    val startTime: String,
    val endTime: String,
    val timezone: String? = null,
    val slotMinutes: Int? = null,
    val title: String? = null,
    val memo: String? = null,
    val alarm: String? = null
)

data class SchedulePollMetaUpdateRequest(
    val title: String? = null,
    val memo: String? = null,
    val alarm: String? = null
)

data class SchedulePollVoteUpsertRequest(
    val slots: List<Int>
)

data class SchedulePollFixRequest(
    val fixedSlotId: Int
)

// === Response ===

data class SchedulePollCreateResponse(
    val pollId: Long?
)

data class SchedulePollDetailResponse(
    val pollId: Long?,
    val teamId: Long?,
    val timezone: String?,
    val slotMinutes: Int?,
    val startTime: String?,
    val endTime: String?,
    val dates: List<String>?,
    val title: String?,
    val memo: String?,
    val alarm: String?,
    val status: String?,
    val locked: Boolean?,
    val autoFixedSlotId: Int?,
    val fixedSlotId: Int?,
    val totalCount: Long?,
    val votedCount: Long?,
    val members: List<MemberDto>?,
    val votesByMember: List<MemberVoteDto>?,
    val intersectionSlots: List<Int>?,
    val mySlots: List<Int>?,
    val slotDefinitions: List<SlotDefinitionResponse>?
)

data class MemberDto(
    val memberId: Long?,
    val name: String?
)

data class MemberVoteDto(
    val memberId: Long?,
    val slots: List<Int>?
)

data class SlotDefinitionResponse(
    val slotId: Int?,
    val date: String?,
    val startTime: String?,
    val endTime: String?
)

data class TeamFixedSchedulePollSummaryResponse(
    val pollId: Long?,
    val title: String?,
    val status: String?,
    val fixedSlotId: Int?,
    val dates: List<String>?
)
