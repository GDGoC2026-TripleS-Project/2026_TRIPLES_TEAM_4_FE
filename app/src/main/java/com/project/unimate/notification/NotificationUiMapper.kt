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

        val nowSection = mutableListOf<NotificationItem>()
        val pastByDays = linkedMapOf<Int, MutableList<NotificationItem>>()

        for (item in sorted) {
            if (isPinnedNow(item)) {
                nowSection.add(item)
            } else {
                val dayKey = elapsedDaysLabelKey(item.createdAtMillis(), nowMillis)
                pastByDays.getOrPut(dayKey) { mutableListOf() }.add(item)
            }
        }

        val result = mutableListOf<UiSection>()
        if (nowSection.isNotEmpty()) {
            result.add(UiSection(title = "지금", items = nowSection))
        }

        for ((daysAgo, items) in pastByDays.toSortedMap()) {
            result.add(UiSection(title = "${daysAgo}일 전", items = items))
        }

        return result
    }

    private fun isPinnedNow(item: NotificationItem): Boolean {
        return if (item.action) {
            !item.actionDone
        } else {
            !item.isRead
        }
    }

    private fun elapsedDaysLabelKey(createdAtMillis: Long, nowMillis: Long): Int {
        if (createdAtMillis <= 0L) return 1
        val elapsed = (nowMillis - createdAtMillis).coerceAtLeast(0L)
        val dayMs = 24L * 60L * 60L * 1000L
        return (elapsed / dayMs).toInt() + 1
    }
}

