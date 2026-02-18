package com.project.unimate.ui.poke

import android.os.Parcelable // import 필요
import kotlinx.parcelize.Parcelize // import 필요

sealed class PokeData : Parcelable { // 부모에도 Parcelable 추가
    @Parcelize
    data class Header(
        val title: String,
        val teamColor: String,
        var isAllSelected: Boolean = false
    ) : PokeData()

    @Parcelize
    data class Member(
        val id: Int,
        val name: String,
        val teamName: String,
        val teamColor: String,
        var isSelected: Boolean = false
    ) : PokeData()

    /** 팀원이 나 혼자인 팀에서 팀원 목록 자리에 표시하는 문구용 (선택 불가) */
    @Parcelize
    data class NoMembersMessage(
        val teamName: String,
        val teamColor: String
    ) : PokeData()
}