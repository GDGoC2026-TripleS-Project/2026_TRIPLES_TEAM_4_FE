package com.project.unimate.data.repository

import android.content.Context

/** 마이페이지·프로필 수정에 쓰는 유저 프로필 이미지 경로 저장. 앱 재시작 후에도 유지. */
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
