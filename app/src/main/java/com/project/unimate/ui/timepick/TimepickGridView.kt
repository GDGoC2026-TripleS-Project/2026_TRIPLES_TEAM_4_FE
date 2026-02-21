package com.project.unimate.ui.timepick

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.project.unimate.R

/** 타임픽 그리드(날짜/요일 헤더, 시간 라벨, 셀). 드래그 선택 시 main_green. 읽기 전용이면 선택 불가 */
class TimepickGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var dayCount: Int = 7
        set(value) { field = value; invalidate() }
    var startHour: Int = 8
        set(value) { field = value; invalidate() }
    var endHour: Int = 24
        set(value) { field = value; invalidate() }

    /** 한 시간당 30분 슬롯 2개. 선택/데이터 모두 (day, slotIndex) 사용. */
    private val hourCount: Int get() = (endHour - startHour).coerceAtLeast(0)
    private val slotsPerHour: Int get() = 2
    private val slotRowCount: Int get() = hourCount * slotsPerHour

    /** (dayIndex, slotIndex) -> selected. slotIndex는 0..slotRowCount-1 (30분 단위). */
    val selectedCells = mutableSetOf<Pair<Int, Int>>()

    /** 읽기 전용 모드에서 (dayIndex, slotIndex) -> 인원 수. */
    var cellCounts: Map<Pair<Int, Int>, Int> = emptyMap()
        set(value) { field = value; invalidate() }

    /** 날짜별 선택 가능 슬롯 범위. null이면 전체. 각 (startSlot, endSlot) 0..slotRowCount. */
    var dayTimeRanges: List<Pair<Int, Int>>? = null
        set(value) { field = value; invalidate() }

    var isReadOnly: Boolean = false
    var onSelectionChanged: (() -> Unit)? = null
    /** 읽기 전용일 때 셀 클릭 시 (dayIndex, slotIndex) 전달 */
    var onCellClick: ((day: Int, slot: Int) -> Unit)? = null

    /** "2/14" to "일" 등. size = dayCount */
    var dateHeaders: List<Pair<String, String>> = emptyList()
        set(value) { field = value; invalidate() }

    /** "오전 8시" 등. size = hourCount */
    var timeLabels: List<String> = emptyList()
        set(value) { field = value; invalidate() }

    /** false면 왼쪽 시간 라벨을 그리지 않고 그리드만 그림 (고정 시간 컬럼과 분리할 때 사용) */
    var drawTimeLabels: Boolean = true
        set(value) { field = value; invalidate(); requestLayout() }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val levelColors: IntArray by lazy {
        intArrayOf(
            ContextCompat.getColor(context, R.color.timepick_level_1),
            ContextCompat.getColor(context, R.color.timepick_level_2),
            ContextCompat.getColor(context, R.color.timepick_level_3),
            ContextCompat.getColor(context, R.color.timepick_level_4),
            ContextCompat.getColor(context, R.color.timepick_darkest)
        )
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.gray04)
        strokeWidth = 1.5f
    }
    private val gridBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.gray02)
    }
    private val gridRoundRadius = 12f * resources.displayMetrics.density
    private val textPaintDate = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * resources.displayMetrics.scaledDensity
        color = ContextCompat.getColor(context, R.color.gray06)
    }
    private val textPaintWeekday = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 16f * resources.displayMetrics.scaledDensity
        isFakeBoldText = true
        color = ContextCompat.getColor(context, R.color.gray09)
    }
    private val textPaintTime = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * resources.displayMetrics.scaledDensity
        color = ContextCompat.getColor(context, R.color.gray06)
    }
    private val gridBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF000000.toInt()
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val cellRect = RectF()
    private var cellWidth = 0f
    private var cellHeight = 0f
    private var headerHeight = 0f
    private var leftLabelWidth = 0f
    private var dragging = false
    private var dragSelecting = true
    private var lastDay = -1
    private var lastSlot = -1

    private fun isSlotInRange(day: Int, slot: Int): Boolean {
        val ranges = dayTimeRanges ?: return true
        val pair = ranges.getOrNull(day) ?: return true
        return slot in pair.first until pair.second
    }

    /** 요일과 그리드 사이 여백 + 마지막 시간(종료시간) 라벨/셀이 잘리지 않도록 하단 여유 */
    private val headerBottomMargin: Float get() = 12f * resources.displayMetrics.density
    private val timeLabelBottomPadding: Float get() = 40f * resources.displayMetrics.density

    /** 7일일 때 가용 너비를 꽉 채워 양옆 마진이 같아지도록. drawTimeLabels=false면 그리드 옆 56dp 스트립을 빼고 계산 */
    private fun computeColumnWidth(referenceWidth: Int): Float {
        val lw = if (drawTimeLabels) 56f * resources.displayMetrics.density else 0f
        val available = (referenceWidth - lw).coerceAtLeast(0f)
        return (available / 7f).coerceAtLeast(0f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        leftLabelWidth = if (drawTimeLabels) 56f * density else 0f
        headerHeight = 64f * density
        val marginHorizontal = 16f * resources.displayMetrics.density * 2f
        val stripWidthPx = (56f * density).toInt()
        val refWidth = when (View.MeasureSpec.getMode(widthMeasureSpec)) {
            View.MeasureSpec.UNSPECIFIED -> {
                val screenW = resources.displayMetrics.widthPixels
                val base = (screenW - marginHorizontal).toInt().coerceAtLeast(0)
                if (drawTimeLabels) base else (base - stripWidthPx).coerceAtLeast(0)
            }
            else -> View.MeasureSpec.getSize(widthMeasureSpec)
        }
        val columnWidthPx = computeColumnWidth(refWidth)
        val desiredW = (leftLabelWidth + columnWidthPx * dayCount).toInt().coerceAtLeast(0)
        val resolvedW = resolveSize(desiredW, widthMeasureSpec)
        val minSlotH = 20f * density
        val gridHeight = slotRowCount * minSlotH
        val desiredH = (headerHeight + headerBottomMargin + gridHeight + timeLabelBottomPadding).toInt().coerceAtLeast(200)
        val hSpec = View.MeasureSpec.makeMeasureSpec(
            resolveSize(desiredH, heightMeasureSpec),
            View.MeasureSpec.EXACTLY
        )
        setMeasuredDimension(resolvedW, resolveSize(desiredH, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        leftLabelWidth = if (drawTimeLabels) 56f * resources.displayMetrics.density else 0f
        headerHeight = 64f * resources.displayMetrics.density
        val gridW = (w - leftLabelWidth).coerceAtLeast(0f)
        val minSlotH = 20f * resources.displayMetrics.density
        val maxGridH = slotRowCount * minSlotH
        val availableGridH = (h - headerHeight - headerBottomMargin - timeLabelBottomPadding).coerceAtLeast(0f)
        val gridH = minOf(maxGridH, availableGridH)
        cellWidth = if (dayCount > 0) gridW / dayCount else 0f
        cellHeight = if (slotRowCount > 0) gridH / slotRowCount else 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val gridLeft = leftLabelWidth
        val gridTop = headerHeight + headerBottomMargin
        val hourBlockHeight = cellHeight * slotsPerHour
        val gridRight = gridLeft + dayCount * cellWidth
        val gridBottom = gridTop + slotRowCount * cellHeight

        val density = resources.displayMetrics.density
        val dateBaseline = 20f * density + (textPaintDate.fontMetrics.descent - textPaintDate.fontMetrics.ascent) * 0.5f
        val weekdayBaseline = 38f * density + (textPaintWeekday.fontMetrics.descent - textPaintWeekday.fontMetrics.ascent) * 0.5f
        for (day in 0 until dayCount) {
            val left = gridLeft + day * cellWidth
            val datePair = dateHeaders.getOrNull(day)
            if (datePair != null) {
                val cx = left + cellWidth / 2f
                canvas.drawText(datePair.first, cx, dateBaseline, textPaintDate.apply { textAlign = Paint.Align.CENTER })
                canvas.drawText(datePair.second, cx, weekdayBaseline, textPaintWeekday.apply { textAlign = Paint.Align.CENTER })
            }
        }
        // TimepickTimeLabelStrip과 동일한 시간 라벨 공식 사용
        if (drawTimeLabels) {
            val fm = textPaintTime.fontMetrics
            val labelVerticalCenterOffset = (fm.ascent + fm.descent) / 2f
            val labelShiftUp = 10f * resources.displayMetrics.density
            val availableGridHForLabels = (height - headerHeight - headerBottomMargin - timeLabelBottomPadding).coerceAtLeast(0f)
            val hourBlockHeightForLabels = if (slotRowCount > 0) (availableGridHForLabels / slotRowCount) * slotsPerHour else 0f
            for (hour in 0..hourCount) {
                val topOfHour = gridTop + hour * hourBlockHeightForLabels
                val label = timeLabels.getOrNull(hour)
                if (label != null) {
                    val labelY = topOfHour + hourBlockHeightForLabels / 2f - labelVerticalCenterOffset - labelShiftUp
                    canvas.drawText(label, 8f, labelY, textPaintTime)
                }
            }
        }

        val gridRect = RectF(gridLeft, gridTop, gridRight, gridBottom)
        canvas.drawRoundRect(gridRect, gridRoundRadius, gridRoundRadius, gridBgPaint)
        val clipPath = Path().apply { addRoundRect(gridRect, gridRoundRadius, gridRoundRadius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(clipPath)
        val maxCount = cellCounts.values.maxOrNull() ?: 1
        for (day in 0 until dayCount) {
            for (slot in 0 until slotRowCount) {
                if (!isSlotInRange(day, slot)) continue
                val left = gridLeft + day * cellWidth
                val top = gridTop + slot * cellHeight
                cellRect.set(left, top, left + cellWidth, top + cellHeight)
                if (isReadOnly && cellCounts.isNotEmpty()) {
                    val count = cellCounts[day to slot] ?: 0
                    if (count > 0) {
                        val level = if (maxCount <= 1) 0 else ((count - 1).toFloat() / (maxCount - 1) * (levelColors.size - 1)).toInt().coerceIn(0, levelColors.size - 1)
                        cellPaint.color = levelColors[level]
                        canvas.drawRect(cellRect, cellPaint)
                    }
                } else if (selectedCells.contains(day to slot)) {
                    cellPaint.color = ContextCompat.getColor(context, R.color.main_green)
                    canvas.drawRect(cellRect, cellPaint)
                }
            }
        }
        for (day in 0 until dayCount) {
            val range = dayTimeRanges?.getOrNull(day) ?: (0 to slotRowCount)
            for (slot in range.first until range.second) {
                val left = gridLeft + day * cellWidth
                val top = gridTop + slot * cellHeight
                cellRect.set(left, top, left + cellWidth, top + cellHeight)
                canvas.drawRect(cellRect, gridPaint)
            }
        }
        canvas.restore()
        val halfStroke = gridBorderPaint.strokeWidth / 2f
        val borderRect = RectF(gridLeft + halfStroke, gridTop + halfStroke, gridRight - halfStroke, gridBottom - halfStroke)
        canvas.drawRoundRect(borderRect, gridRoundRadius - halfStroke, gridRoundRadius - halfStroke, gridBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val gridLeft = leftLabelWidth
        val gridTop = headerHeight + headerBottomMargin
        if (x < gridLeft || y < gridTop) return false
        val day = ((x - gridLeft) / cellWidth).toInt().coerceIn(0, dayCount - 1)
        val slotRow = ((y - gridTop) / cellHeight).toInt().coerceIn(0, slotRowCount - 1)
        if (!isSlotInRange(day, slotRow)) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isReadOnly) {
                    onCellClick?.invoke(day, slotRow)
                    return true
                }
                dragging = true
                lastDay = day
                lastSlot = slotRow
                dragSelecting = !selectedCells.contains(day to slotRow)
                setCell(day, slotRow, dragSelecting)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && (day != lastDay || slotRow != lastSlot)) {
                    lastDay = day
                    lastSlot = slotRow
                    setCell(day, slotRow, dragSelecting)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setCell(day: Int, slot: Int, selected: Boolean) {
        val key = day to slot
        if (selected) selectedCells.add(key) else selectedCells.remove(key)
        invalidate()
        onSelectionChanged?.invoke()
    }

    fun setSelected(cells: Set<Pair<Int, Int>>) {
        selectedCells.clear()
        selectedCells.addAll(cells)
        invalidate()
    }
}
