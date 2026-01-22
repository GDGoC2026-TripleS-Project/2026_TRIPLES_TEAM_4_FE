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
import com.project.unimate.auth.FcmRegistrar

class UnimateFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken: ${token.take(12)}...")
        // ✅ JWT가 있으면 즉시 서버에 재등록 (토큰은 자주 바뀔 수 있음)
        FcmRegistrar.registerIfPossible(this, BASE_URL)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)) // ✅ 길어도 보이게
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        private const val BASE_URL = "https://seok-hwan1.duckdns.org"

        const val EXTRA_PUSH_SCREEN = "push_screen"
        const val EXTRA_PUSH_ALARM_ID = "push_alarm_id"
    }
}
