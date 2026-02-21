package com.project.unimate

// 역할: 푸시 알림 채널 생성·유지. IMPORTANCE_HIGH 미만이면 채널 재생성

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

object NotificationChannels {
    const val ALERT_CHANNEL_ID = "unimate_alert_v3"
    private const val TAG = "NotificationChannels"

    fun ensureAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(ALERT_CHANNEL_ID)
        if (existing != null && existing.importance >= NotificationManager.IMPORTANCE_HIGH) return
        if (existing != null) {
            // 사용자가 채널 중요도를 낮춘 경우 재생성해 HIGH 복구
            Log.w(TAG, "Alert channel importance is ${existing.importance}. Recreating as HIGH.")
            nm.deleteNotificationChannel(ALERT_CHANNEL_ID)
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "Unimate Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Unimate push alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
                setSound(soundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }
}
