package com.project.unimate.data.entity

/**
 * 팀플(팀 스페이스) 엔티티.
 * @param id 팀 ID
 * @param name 팀 이름 (표시용)
 * @param colorHex 팀 지정색 (예: "#F495E0")
 * @param imageResName drawable 리소스 이름 (예: "cherish_image")
 * @param isCompleted 완료 여부 (마이페이지 "완료된 팀플" 구분용)
 */
data class Team(
    val id: String,
    val name: String,
    val colorHex: String,
    val imageResName: String,
    val isCompleted: Boolean = false,
    val memberCount: Int = 0,
    val deadlineDays: Int? = null // 마감 D-N, null이면 표시 안 함
)
