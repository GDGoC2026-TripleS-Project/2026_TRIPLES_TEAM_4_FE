package com.project.unimate.notification

import android.content.Context
import com.project.unimate.auth.JwtStore
import com.project.unimate.network.ApiClient
import com.project.unimate.network.Env
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class NotificationApi(
    private val baseUrl: String = Env.BASE_URL
) {
    fun completeNotification(context: Context, notificationId: Long, onDone: (Boolean) -> Unit) {
        val jwt = JwtStore.load(context) ?: ""
        if (jwt.isBlank()) {
            onDone(false)
            return
        }

        val req = Request.Builder()
            .url("$baseUrl/api/notifications/$notificationId/complete")
            .post(ByteArray(0).toRequestBody(null, 0, 0))
            .addHeader("Authorization", "Bearer $jwt")
            .build()

        ApiClient.http.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onDone(false)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val ok = response.code in 200..299
                response.close()
                onDone(ok)
            }
        })
    }
}
