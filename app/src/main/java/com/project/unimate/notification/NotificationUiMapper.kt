package com.project.unimate.notification

object NotificationUiMapper {

    data class UiSection(
        val title: String,
        val items: List<NotificationItem>
    )

    fun toSections(
        notifications: List<NotificationItem>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<UiSection> {
        val sorted = notifications.sortedByDescending { it.createdAtMillisOrMin() }

        val pastByDays = linkedMapOf<Int, MutableList<NotificationItem>>()

        for (item in sorted) {
            val dayKey = elapsedDaysLabelKey(item.createdAtMillis(), nowMillis)
            pastByDays.getOrPut(dayKey) { mutableListOf() }.add(item)
        }

        val result = mutableListOf<UiSection>()
        for ((daysAgo, items) in pastByDays.toSortedMap()) {
            val title = if (daysAgo == 0) "지금" else "${daysAgo}일 전"
            result.add(UiSection(title = title, items = items))
        }

        return result
    }

    private fun elapsedDaysLabelKey(createdAtMillis: Long, nowMillis: Long): Int {
        if (createdAtMillis <= 0L) return 0
        val elapsed = (nowMillis - createdAtMillis).coerceAtLeast(0L)
        val dayMs = 24L * 60L * 60L * 1000L
        return (elapsed / dayMs).toInt()
    }
}
