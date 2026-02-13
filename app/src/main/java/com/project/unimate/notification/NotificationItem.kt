package com.project.unimate.notification

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class NotificationItem(
    val notificationId: Long,
    val teamId: Long,
    val teamName: String,
    val teamColorHex: String,
    val alarmType: String,
    val messageTitle: String,
    val messageBody: String,
    val createdAt: String,
    val isRead: Boolean,
    val action: Boolean,
    val actionDone: Boolean,
    val processedAt: String?
) {
    fun createdAtMillis(): Long {
        val text = createdAt.trim()
        if (text.isBlank()) return 0L

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS"
        )

        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (!pattern.contains("XXX")) {
                        timeZone = TimeZone.getDefault()
                    }
                }
                val date = fmt.parse(text)
                if (date != null) return date.time
            } catch (_: ParseException) {
            } catch (_: IllegalArgumentException) {
            }
        }

        return 0L
    }

    fun createdAtMillisOrMin(): Long {
        val t = createdAtMillis()
        return if (t <= 0L) Long.MIN_VALUE else t
    }

    companion object {
        fun fromFcmData(data: Map<String, String>): NotificationItem? {
            val notificationId = data["notificationId"]?.toLongOrNull() ?: return null
            val teamId = data["teamId"]?.toLongOrNull() ?: 0L
            val rawTeamName = data["teamName"] ?: ""
            val teamName = rawTeamName.ifBlank { "Unknown" }
            val rawTeamColor = data["teamColorHex"] ?: ""
            val teamColorHex = rawTeamColor.ifBlank { "#CCCCCC" }
            val alarmType = data["alarmType"] ?: "알림"
            val messageTitle = data["messageTitle"] ?: ""
            val messageBody = data["messageBody"] ?: ""
            val createdAt = data["createdAt"] ?: return null

            return NotificationItem(
                notificationId = notificationId,
                teamId = teamId,
                teamName = teamName,
                teamColorHex = teamColorHex,
                alarmType = alarmType,
                messageTitle = messageTitle,
                messageBody = messageBody,
                createdAt = createdAt,
                isRead = false,
                action = true,
                actionDone = false,
                processedAt = null
            )
        }
    }
}
