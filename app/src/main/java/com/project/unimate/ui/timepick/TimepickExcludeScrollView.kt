package com.project.unimate.ui.timepick

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView

/**
 * 타임픽 공간(excludeView) 안에서는 세로 스크롤을 막고, 해당 영역 터치 시 드래그만 가능하게 함.
 */
class TimepickExcludeScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    var excludeView: View? = null

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val ex = excludeView ?: return super.onInterceptTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val loc = IntArray(2)
                ex.getLocationOnScreen(loc)
                val x = ev.rawX
                val y = ev.rawY
                if (x >= loc[0] && x < loc[0] + ex.width && y >= loc[1] && y < loc[1] + ex.height) {
                    return false
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
