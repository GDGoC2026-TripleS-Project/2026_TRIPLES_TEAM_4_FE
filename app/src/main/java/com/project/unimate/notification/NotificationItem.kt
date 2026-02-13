package com.project.unimate.notification

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

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
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            val date = fmt.parse(createdAt)
            date?.time ?: 0L
        } catch (e: ParseException) {
            0L
        } catch (e: IllegalArgumentException) {
            0L
        }
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
