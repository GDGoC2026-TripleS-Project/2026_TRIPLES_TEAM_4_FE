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
}
