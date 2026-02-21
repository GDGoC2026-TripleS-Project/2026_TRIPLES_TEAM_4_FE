package com.project.unimate.notification

/**
 * 서버 알림 목록 조회. 구현체에서 API 호출 후 NotificationStore.mergeWithServer()로 병합·저장.
 */
interface NotificationRepository {
    fun fetchNotifications(onDone: (List<NotificationServerItem>) -> Unit)
}
