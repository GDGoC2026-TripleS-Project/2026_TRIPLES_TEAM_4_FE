package com.project.unimate.notification

/**
 * TODO: 서버 알림 목록 API 연동 시 구현.
 * - fetchNotifications()에서 서버 리스트를 가져오고
 * - NotificationStore.mergeWithServer()로 병합 후 저장
 */
interface NotificationRepository {
    fun fetchNotifications(onDone: (List<NotificationServerItem>) -> Unit)
}
