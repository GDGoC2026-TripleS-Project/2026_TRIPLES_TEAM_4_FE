package com.project.unimate.ui.timepick

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.project.unimate.R

/**
 * 타임픽 왼쪽 시간 라벨만 그리는 뷰. 스크롤 시에도 고정되도록 별도 배치한다.
 * TimepickGridView와 동일한 headerHeight/cellHeight 계산으로 라벨 위치를 맞춘다.
 */
class TimepickTimeLabelStrip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var timeLabels: List<String> = emptyList()
        set(value) { field = value; invalidate() }
    var startHour: Int = 8
        set(value) { field = value; invalidate() }
    var endHour: Int = 24
        set(value) { field = value; invalidate() }

    private val hourCount: Int get() = (endHour - startHour).coerceAtLeast(0)
    private val slotsPerHour: Int = 2
    private val slotRowCount: Int get() = hourCount * slotsPerHour

    private val textPaintTime = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * resources.displayMetrics.scaledDensity
        color = ContextCompat.getColor(context, R.color.gray06)
    }

    private var headerHeight = 0f
    private var headerBottomMargin = 0f
    private var timeLabelBottomPadding = 0f
    private var cellHeight = 0f
    private var hourBlockHeight = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val density = resources.displayMetrics.density
        headerHeight = 64f * density
        headerBottomMargin = 12f * density
        timeLabelBottomPadding = 40f * density
        val availableGridH = (h - headerHeight - headerBottomMargin - timeLabelBottomPadding).coerceAtLeast(0f)
        cellHeight = if (slotRowCount > 0) availableGridH / slotRowCount else 0f
        hourBlockHeight = cellHeight * slotsPerHour
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gridTop = headerHeight + headerBottomMargin
        val fm = textPaintTime.fontMetrics
        val labelVerticalCenterOffset = (fm.ascent + fm.descent) / 2f
        val labelShiftUp = 10f * resources.displayMetrics.density
        for (hour in 0..hourCount) {
            val topOfHour = gridTop + hour * hourBlockHeight
            val label = timeLabels.getOrNull(hour) ?: continue
            val labelY = topOfHour + hourBlockHeight / 2f - labelVerticalCenterOffset - labelShiftUp
            canvas.drawText(label, 8f, labelY, textPaintTime)
        }
    }
}
