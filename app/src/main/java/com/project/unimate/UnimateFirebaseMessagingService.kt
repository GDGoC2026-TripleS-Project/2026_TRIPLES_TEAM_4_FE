package com.project.unimate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class UnimateFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data

        // data-only를 우선(라우팅에 필요)
        val title = data["title"] ?: message.notification?.title ?: "Unimate"
        val body = data["body"] ?: message.notification?.body ?: ""
        val screen = data["screen"] ?: "home"
        val alarmId = data["alarmId"]

        Log.d(TAG, "onMessageReceived title=$title body=$body screen=$screen alarmId=$alarmId")

        showNotification(title, body, screen, alarmId)
    }

    private fun showNotification(title: String, body: String, screen: String, alarmId: String?) {
        ensureChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_SCREEN, screen)
            putExtra(EXTRA_PUSH_ALARM_ID, alarmId)
        }

        // ✅ requestCode 고정 금지
        val requestCode = (System.currentTimeMillis() % 100000).toInt()

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(requestCode, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Unimate", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        private const val TAG = "UnimateFCM"
        private const val CHANNEL_ID = "unimate_default"

        const val EXTRA_PUSH_SCREEN = "push_screen"
        const val EXTRA_PUSH_ALARM_ID = "push_alarm_id"
    }
}
