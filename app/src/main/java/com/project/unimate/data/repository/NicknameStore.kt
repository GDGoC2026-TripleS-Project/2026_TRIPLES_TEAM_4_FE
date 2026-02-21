package com.project.unimate.data.repository

// 역할: 서버에서 불러온 닉네임 SharedPreferences 저장. sync 후 복구용

import android.content.Context

/** 닉네임 저장/조회. */
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
