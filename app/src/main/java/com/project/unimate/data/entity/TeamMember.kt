package com.project.unimate.data.entity

/**
 * 팀원 엔티티 (팀 스페이스용).
 * @param id 팀원 ID
 * @param name 표시 이름 (한국어)
 * @param iconResName drawable 리소스 이름 (예: "ic_user")
 */
data class TeamMember(
    val id: String,
    val name: String,
    val iconResName: String = "ic_user"
)
