package com.project.unimate.data.repository

// 역할: 현재 유저 프로필 이미지 리소스명 저장. 마이페이지·프로필 수정용

import android.content.Context

/** 프로필 이미지 저장/조회. */
object ProfileImageStore {
    private const val PREFS_NAME = "user_profile_image"
    private const val KEY_IMAGE_RES_NAME = "image_res_name"

    fun save(context: Context, imageResName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IMAGE_RES_NAME, imageResName)
            .apply()
    }

    fun get(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_IMAGE_RES_NAME, "") ?: ""
    }
}
