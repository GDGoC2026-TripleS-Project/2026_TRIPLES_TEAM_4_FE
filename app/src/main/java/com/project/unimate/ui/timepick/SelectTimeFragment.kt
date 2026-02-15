package com.project.unimate.ui.timepick

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.project.unimate.R
import java.util.Calendar

class SelectTimeFragment : Fragment() {

    private fun hourToLabel(hour: Int): String = when (hour) {
        0 -> "오전 12시"
        in 1..11 -> "오전 ${hour}시"
        12 -> "오후 12시"
        else -> "오후 ${hour - 12}시"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_select_time, container, false)
        val back = root.findViewById<ImageButton>(R.id.selectTimeBack)
        val grid = root.findViewById<TimepickGridView>(R.id.selectTimeGrid)
        val strip = root.findViewById<TimepickTimeLabelStrip>(R.id.selectTimeLabelStrip)
        val scrollView = root.findViewById<HorizontalScrollView>(R.id.selectTimeScroll)
        val contentRow = root.findViewById<LinearLayout>(R.id.selectTimeContentRow)
        val scrollContent = root.findViewById<View>(R.id.selectTimeScrollContent)
        val gridWrapper = root.findViewById<View>(R.id.selectTimeGridWrapper)
        val done = root.findViewById<android.widget.Button>(R.id.selectTimeDone)

        val dates = TimepickStateHolder.displayDates
        val startHour = TimepickStateHolder.globalStartHour
        val endHour = TimepickStateHolder.globalEndHour
        if (dates.isEmpty()) {
            findNavController().navigateUp()
            return root
        }

        val dayCount = dates.size
        val slotCount = (endHour - startHour).coerceAtLeast(0) * 2
        val timeLabelsList = (startHour..endHour).map { hourToLabel(it) }

        grid.drawTimeLabels = false
        grid.dayCount = dayCount
        grid.startHour = startHour
        grid.endHour = endHour
        grid.dateHeaders = dates.map { dayMillis ->
            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }
            val m = cal.get(Calendar.MONTH) + 1
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
            val weekday = weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
            "$m/$d" to weekday
        }
        grid.timeLabels = timeLabelsList
        grid.dayTimeRanges = dates.map { dayMillis ->
            val (s, e) = TimepickStateHolder.dateTimeRanges[dayMillis] ?: (startHour to endHour)
            val startSlot = ((s - startHour).coerceAtLeast(0) * 2).coerceIn(0, slotCount)
            val endSlot = ((e - startHour).coerceAtMost(endHour - startHour) * 2).coerceIn(0, slotCount)
            startSlot to endSlot
        }
        grid.isReadOnly = false
        TimepickStateHolder.selectTimeSelected.forEach { (pair, selected) ->
            if (selected) grid.selectedCells.add(pair)
        }
        grid.onSelectionChanged = {
            TimepickStateHolder.selectTimeSelected.clear()
            grid.selectedCells.forEach { TimepickStateHolder.selectTimeSelected[it] = true }
        }

        strip.timeLabels = timeLabelsList
        strip.startHour = startHour
        strip.endHour = endHour

        // 스트립 높이를 그리드와 동일하게 맞춤 (시간 라벨 정렬)
        grid.viewTreeObserver.addOnGlobalLayoutListener {
            if (grid.height > 0 && strip.layoutParams.height != grid.height) {
                strip.layoutParams = strip.layoutParams.apply { height = grid.height }
                strip.requestLayout()
            }
        }

        // 7일 미만: 왼쪽 마진 줄이기(왼쪽에 붙지 않게). 좌 16dp / 우 28dp 로 블록은 중앙감 유지
        val contentWrapper = root.findViewById<LinearLayout>(R.id.selectTimeContentWrapper)
        val density = resources.displayMetrics.density
        val padLeftPx = (16 * density).toInt()
        val padRightPx = if (dayCount < 7) (28 * density).toInt() else padLeftPx
        contentWrapper.setPadding(padLeftPx, contentWrapper.paddingTop, padRightPx, contentWrapper.paddingBottom)
        contentWrapper.gravity = android.view.Gravity.CENTER_HORIZONTAL
        val contentRowLp = contentRow.layoutParams as LinearLayout.LayoutParams
        contentRowLp.width = if (dayCount < 7) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT
        contentRow.layoutParams = contentRowLp

        val scrollLp = scrollView.layoutParams as LinearLayout.LayoutParams
        if (dayCount < 7) {
            scrollLp.width = LinearLayout.LayoutParams.WRAP_CONTENT
            scrollLp.weight = 0f
            (scrollContent.layoutParams as FrameLayout.LayoutParams).width = FrameLayout.LayoutParams.WRAP_CONTENT
        } else {
            scrollLp.width = 0
            scrollLp.weight = 1f
            (scrollContent.layoutParams as FrameLayout.LayoutParams).width = FrameLayout.LayoutParams.MATCH_PARENT
        }
        scrollView.layoutParams = scrollLp

        gridWrapper.layoutParams = (gridWrapper.layoutParams as FrameLayout.LayoutParams).apply {
            width = if (dayCount <= 7) FrameLayout.LayoutParams.MATCH_PARENT else FrameLayout.LayoutParams.WRAP_CONTENT
        }

        val scrollRoot = root.findViewById<com.project.unimate.ui.timepick.TimepickExcludeScrollView>(R.id.selectTimeScrollRoot)
        scrollRoot.excludeView = contentWrapper

        back.setOnClickListener { findNavController().popBackStack() }
        done.setOnClickListener {
            if (grid.selectedCells.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.timepick_select_at_least_one), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            TimepickStateHolder.selectTimeSelected.clear()
            grid.selectedCells.forEach { TimepickStateHolder.selectTimeSelected[it] = true }
            findNavController().navigate(R.id.timepickStatusFragment)
        }

        return root
    }
}
