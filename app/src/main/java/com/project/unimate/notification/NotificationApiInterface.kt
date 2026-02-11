package com.project.unimate.notification

/**
 * TODO: 서버 알림 목록 API 정의용 인터페이스.
 * - 실제 네트워크 구현은 하지 않음.
 */
interface NotificationApiInterface {
    fun getNotifications(onDone: (List<NotificationServerItem>) -> Unit)
}

/**
 * 서버 알림 응답용 모델 (isCompleted는 서버가 제공하지 않으면 null)
 */
data class NotificationServerItem(
    val notificationId: Long,
    val teamId: Long,
    val teamName: String,
    val teamColorHex: String,
    val alarmType: String,
    val messageTitle: String,
    val messageBody: String,
    val createdAt: String,
    val isRead: Boolean?,
    val action: Boolean?,
    val actionDone: Boolean?,
    val processedAt: String?
) {
    fun toNotificationItem(): NotificationItem {
        return NotificationItem(
            notificationId = notificationId,
            teamId = teamId,
            teamName = teamName,
            teamColorHex = teamColorHex,
            alarmType = alarmType,
            messageTitle = messageTitle,
            messageBody = messageBody,
            createdAt = createdAt,
            isRead = this.isRead ?: false,
            action = this.action ?: false,
            actionDone = this.actionDone ?: false,
            processedAt = this.processedAt
        )
    }
}
