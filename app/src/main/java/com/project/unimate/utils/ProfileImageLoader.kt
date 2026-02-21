package com.project.unimate.utils

// 역할: 프로필 이미지 공통 로드. imageRef 규칙(URL/file:/drawable명), version으로 URL 캐시 무효화

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.project.unimate.R
import java.io.File

/**
 * imageRef: null/blank→기본, http(s)→Glide, file:→내부파일, 그 외→drawable명.
 * version: 0이면 캐시 사용, >0이면 캐시 스킵(프로필 변경 직후 갱신용).
 */
object ProfileImageLoader {

    fun load(imageView: ImageView, imageRef: String?, context: Context, version: Long = 0L) {
        when {
            imageRef.isNullOrBlank() -> setDefault(imageView)

            imageRef.startsWith("http://") || imageRef.startsWith("https://") -> {
                val url = if (version > 0L) "$imageRef?v=$version" else imageRef
                Glide.with(context)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .diskCacheStrategy(
                        if (version > 0L) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC
                    )
                    .into(imageView)
            }

            imageRef.startsWith("file:") -> {
                val file = File(context.filesDir, imageRef.removePrefix("file:"))
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        imageView.setImageBitmap(bmp)
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    } else {
                        setDefault(imageView)
                    }
                } else {
                    setDefault(imageView)
                }
            }

            else -> {
                val resId = context.resources.getIdentifier(imageRef, "drawable", context.packageName)
                if (resId != 0) {
                    imageView.setImageResource(resId)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    setDefault(imageView)
                }
            }
        }
    }

    private fun setDefault(imageView: ImageView) {
        imageView.setImageResource(R.drawable.ic_user)
        imageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }
}
