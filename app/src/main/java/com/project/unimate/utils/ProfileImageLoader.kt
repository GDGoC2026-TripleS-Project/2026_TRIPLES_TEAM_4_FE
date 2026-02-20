package com.project.unimate.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.project.unimate.R
import java.io.File

/**
 * 내 프로필 / 팀원 프로필 이미지 공통 로더.
 *
 * imageRef 규칙:
 *  - null / blank           → 기본 이미지(ic_user)
 *  - "http" / "https" 시작  → Glide URL 로드 (circleCrop, placeholder ic_user)
 *  - "file:" 시작           → internal storage 파일 → BitmapFactory (CENTER_CROP)
 *  - 그 외                  → drawable 리소스 이름으로 해석
 *
 * @param version URL 캐시 무효화용 타임스탬프 (0이면 무시). 프로필 변경 직후 version 전달 시 캐시 스킵.
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
