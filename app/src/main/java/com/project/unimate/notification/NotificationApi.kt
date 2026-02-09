package com.project.unimate.notification

import android.content.Context
import com.project.unimate.auth.JwtStore
import com.project.unimate.network.ApiClient
import com.project.unimate.network.Env
import android.util.Log
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

class NotificationApi(
    private val baseUrl: String = Env.BASE_URL
) {
    private val tag = "NotificationApi"
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

    fun getNotifications(context: Context, onDone: (List<NotificationServerItem>) -> Unit) {
        val jwt = JwtStore.load(context) ?: ""
        if (jwt.isBlank()) {
            onDone(emptyList())
            return
        }

        val req = Request.Builder()
            .url("$baseUrl/api/notifications")
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()

        ApiClient.http.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                onDone(emptyList())
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string().orEmpty()
                val code = response.code
                response.close()
                if (code !in 200..299) {
                    Log.w(tag, "getNotifications failed. code=$code body=${body.take(200)}")
                    onDone(emptyList())
                    return
                }

                val list = parseNotificationList(body)
                onDone(list)
            }
        })
    }

    fun markRead(context: Context, notificationId: Long, onDone: (Boolean) -> Unit) {
        patchNoBody(context, "/api/notifications/$notificationId/read", onDone)
    }

    fun readAll(context: Context, onDone: (Boolean) -> Unit) {
        patchNoBody(context, "/api/notifications/read-all", onDone)
    }

    fun markActionDone(context: Context, notificationId: Long, onDone: (Boolean) -> Unit) {
        patchNoBody(context, "/api/notifications/$notificationId/action-done", onDone)
    }

    private fun patchNoBody(context: Context, path: String, onDone: (Boolean) -> Unit) {
        val jwt = JwtStore.load(context) ?: ""
        if (jwt.isBlank()) {
            onDone(false)
            return
        }

        val req = Request.Builder()
            .url("$baseUrl$path")
            .patch(ByteArray(0).toRequestBody(null, 0, 0))
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

    private fun parseServerItem(obj: JSONObject): NotificationServerItem {
        val isRead = if (obj.has("isRead")) obj.optBoolean("isRead", false) else obj.optBoolean("read", false)

        return NotificationServerItem(
            notificationId = obj.optLong("id", -1L).takeIf { it > 0 } ?: obj.optLong("notificationId", -1L),
            teamId = obj.optLong("teamId", 0L),
            teamName = obj.optString("teamName", "Unknown"),
            teamColorHex = obj.optString("teamColorHex", "#CCCCCC"),
            alarmType = obj.optString("alarmType", "알림"),
            messageTitle = obj.optString("messageTitle", ""),
            messageBody = obj.optString("messageBody", ""),
            createdAt = obj.optString("createdAt", ""),
            isRead = isRead,
            action = obj.optBoolean("action", false),
            actionDone = obj.optBoolean("actionDone", false),
            processedAt = obj.optString("processedAt", null)
        )
    }

    private fun parseNotificationList(body: String): List<NotificationServerItem> {
        return try {
            val arr = JSONArray(body)
            val out = ArrayList<NotificationServerItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                out.add(parseServerItem(obj))
            }
            out
        } catch (e: Exception) {
            Log.w(tag, "getNotifications parse failed. body=${body.take(200)}", e)
            emptyList()
        }
    }
}
