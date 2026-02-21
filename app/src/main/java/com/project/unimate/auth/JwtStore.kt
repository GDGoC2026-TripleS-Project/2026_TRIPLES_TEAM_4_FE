package com.project.unimate.auth

// 역할: 액세스/리프레시 토큰·userId SharedPreferences 저장·로드

import android.content.Context

object JwtStore {
    private const val PREF = "unimate_auth"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"

    fun saveAccessToken(context: Context, token: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCESS_TOKEN, token.trim())
            .apply()
    }

    fun saveRefreshToken(context: Context, token: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_REFRESH_TOKEN, token.trim())
            .apply()
    }

    fun saveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_USER_ID, userId)
            .apply()
    }

    fun loadAccessToken(context: Context): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_ACCESS_TOKEN, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun loadRefreshToken(context: Context): String? {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_REFRESH_TOKEN, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun loadUserId(context: Context): Long? {
        val id = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getLong(KEY_USER_ID, -1L)
        return if (id > 0) id else null
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }

    // 기존 호출부 호환용
    fun save(context: Context, jwt: String) = saveAccessToken(context, jwt)
    fun load(context: Context): String? = loadAccessToken(context)
}
