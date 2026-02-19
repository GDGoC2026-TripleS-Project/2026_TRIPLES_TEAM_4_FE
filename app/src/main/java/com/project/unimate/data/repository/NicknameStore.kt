package com.project.unimate.data.repository

import android.content.Context

/** 서버에서 불러온 닉네임 저장. 재설치 후 sync 전까지 복구용. */
object NicknameStore {
    private const val PREFS_NAME = "user_nickname"
    private const val KEY_NICKNAME = "nickname"

    fun save(context: Context, nickname: String) {
        if (nickname.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NICKNAME, nickname.trim())
            .apply()
    }

    fun get(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NICKNAME, "")?.trim() ?: ""
    }
}
