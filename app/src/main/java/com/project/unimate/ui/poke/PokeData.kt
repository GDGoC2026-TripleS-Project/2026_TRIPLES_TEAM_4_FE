package com.project.unimate.ui.poke

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class PokeData : Parcelable {
    @Parcelize
    data class Header(
        val teamId: Long,
        val title: String,
        val teamColor: String,
        var isAllSelected: Boolean = false
    ) : PokeData()

    @Parcelize
    data class Member(
        val userId: Long,
        val teamId: Long,
        val teamName: String,
        val teamColor: String,
        val name: String,
        var isSelected: Boolean = false
    ) : PokeData()

    /** 팀원이 나 혼자인 팀에서 팀원 목록 자리에 표시하는 문구용 (선택 불가) */
    @Parcelize
    data class NoMembersMessage(
        val teamName: String,
        val teamColor: String
    ) : PokeData()
}
