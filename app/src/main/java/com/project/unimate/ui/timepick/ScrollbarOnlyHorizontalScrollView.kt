package com.project.unimate.ui.timepick

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView

/**
 * 가로 스크롤은 스크롤바 영역(하단) 터치로만 가능.
 * 타임픽 그리드 영역 터치로는 스크롤되지 않고 그리드 선택만 된다.
 */
class ScrollbarOnlyHorizontalScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val scrollbarTouchHeightPx: Int
        get() = (44 * resources.displayMetrics.density).toInt().coerceAtLeast(24)

    private fun isInScrollbarArea(ev: MotionEvent): Boolean {
        return ev.y >= height - scrollbarTouchHeightPx
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!isInScrollbarArea(ev)) return false
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!isInScrollbarArea(ev)) return false
        return super.onTouchEvent(ev)
    }
}
