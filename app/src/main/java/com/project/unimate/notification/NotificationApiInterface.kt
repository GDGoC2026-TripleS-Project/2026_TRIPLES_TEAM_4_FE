package com.project.unimate.notification

/**
 * 서버 알림 목록 조회 인터페이스. 실제 네트워크 호출은 NotificationApi에서 수행.
 */
interface NotificationApiInterface {
    fun getNotifications(onDone: (List<NotificationServerItem>) -> Unit)
}

/** 서버 알림 응답 모델. isRead/action/actionDone 등은 서버 미제공 시 null */
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
    val processedAt: String?,
    val meetingPollId: Long?,
    val meetingNavigationTarget: String?
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
            processedAt = this.processedAt,
            meetingPollId = this.meetingPollId,
            meetingNavigationTarget = this.meetingNavigationTarget
        )
    }
}
