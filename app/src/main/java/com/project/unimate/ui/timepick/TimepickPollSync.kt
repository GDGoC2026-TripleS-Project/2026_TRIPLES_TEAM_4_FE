package com.project.unimate.ui.timepick

// 역할: 모이기 poll 서버 동기화. SchedulePollService getDetail → TimepickStateHolder 반영

import android.content.Context
import com.project.unimate.auth.JwtStore
import com.project.unimate.network.RetrofitClient
import com.project.unimate.network.dto.SchedulePollDetailResponse
import com.project.unimate.network.service.SchedulePollService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object TimepickPollSync {

    suspend fun refreshFromServer(context: Context, pollId: Long): Boolean {
        return try {
            val service = RetrofitClient.create<SchedulePollService>(context)
            val response = service.getDetail(pollId)
            if (!response.isSuccessful) return false
            val detail = response.body() ?: return false
            applyDetail(context, pollId, detail)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun applyDetail(
        context: Context,
        fallbackPollId: Long,
        detail: SchedulePollDetailResponse
    ) {
        val pollId = detail.pollId ?: fallbackPollId
        val startHour = parseHour(detail.startTime) ?: TimepickStateHolder.globalStartHour
        val endHour = parseHour(detail.endTime) ?: TimepickStateHolder.globalEndHour
        val dateTexts = detail.dates.orEmpty()
        val dateMap = parseDates(dateTexts)
        val displayDates = dateMap.values.toList().sorted()

        if (displayDates.isNotEmpty()) {
            TimepickStateHolder.displayDates = displayDates
            TimepickStateHolder.selectedDates.clear()
            TimepickStateHolder.selectedDates.addAll(displayDates)
            TimepickStateHolder.dateTimeRanges.clear()
            displayDates.forEach { day ->
                TimepickStateHolder.dateTimeRanges[day] = startHour to endHour
            }
        }
        TimepickStateHolder.globalStartHour = startHour
        TimepickStateHolder.globalEndHour = endHour
        TimepickStateHolder.pollId = pollId
        TimepickStateHolder.pollStatus = detail.status
        detail.teamId?.let { teamId ->
            if (teamId > 0) {
                TimepickStateHolder.teamId = teamId.toString()
            }
        }

        val slotToCell = mutableMapOf<Int, Pair<Int, Int>>()
        detail.slotDefinitions.orEmpty().forEach { slot ->
            val slotId = slot.slotId ?: return@forEach
            val dateText = slot.date ?: return@forEach
            val dayMillis = dateMap[dateText] ?: return@forEach
            val dayIndex = TimepickStateHolder.displayDates.indexOf(dayMillis)
            if (dayIndex < 0) return@forEach
            val slotHour = parseHour(slot.startTime) ?: return@forEach
            val slotMinute = parseMinute(slot.startTime) ?: 0
            val slotIndex = ((slotHour - startHour) * 2 + if (slotMinute >= 30) 1 else 0)
            if (slotIndex < 0) return@forEach
            slotToCell[slotId] = dayIndex to slotIndex
        }

        val members = detail.members.orEmpty()
            .mapNotNull { m ->
                val id = m.memberId ?: return@mapNotNull null
                val name = m.name?.trim().orEmpty().ifBlank { "사용자 $id" }
                TimepickStateHolder.PollMember(id, name)
            }
        TimepickStateHolder.pollMembers = members

        val selections = mutableMapOf<Long, Set<Pair<Int, Int>>>()
        detail.votesByMember.orEmpty().forEach { vote ->
            val memberId = vote.memberId ?: return@forEach
            val cells = vote.slots.orEmpty().mapNotNull { slotToCell[it] }.toSet()
            selections[memberId] = cells
        }
        TimepickStateHolder.pollSelectionsByMember = selections

        val intersectionCells = detail.intersectionSlots.orEmpty()
            .mapNotNull { slotToCell[it] }
            .toSet()
        TimepickStateHolder.pollIntersectionCells = intersectionCells

        val fixedSlotId = detail.fixedSlotId ?: detail.autoFixedSlotId
        TimepickStateHolder.confirmedIntersection = if (fixedSlotId != null) {
            slotToCell[fixedSlotId]?.let { setOf(it) } ?: intersectionCells
        } else {
            intersectionCells
        }

        val myUserId = JwtStore.loadUserId(context)
        val mySlots = if (myUserId != null && myUserId > 0) {
            selections[myUserId]
        } else {
            detail.mySlots.orEmpty().mapNotNull { slotToCell[it] }.toSet()
        }.orEmpty()
        TimepickStateHolder.selectTimeSelected.clear()
        mySlots.forEach { cell ->
            TimepickStateHolder.selectTimeSelected[cell] = true
        }
    }

    private fun parseDates(dateTexts: List<String>): LinkedHashMap<String, Long> {
        val result = LinkedHashMap<String, Long>()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            isLenient = false
        }
        dateTexts.forEach { text ->
            try {
                val date = fmt.parse(text) ?: return@forEach
                val cal = Calendar.getInstance().apply { time = date }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                result[text] = cal.timeInMillis
            } catch (_: Exception) {
            }
        }
        return result
    }

    private fun parseHour(text: String?): Int? {
        val t = text?.trim().orEmpty()
        if (!t.contains(":")) return null
        return t.substringBefore(":").toIntOrNull()
    }

    private fun parseMinute(text: String?): Int? {
        val t = text?.trim().orEmpty()
        if (!t.contains(":")) return null
        return t.substringAfter(":").take(2).toIntOrNull()
    }
}
