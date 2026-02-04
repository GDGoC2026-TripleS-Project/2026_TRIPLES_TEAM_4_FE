package com.project.unimate.notification

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object NotificationStore {
    private const val PREF = "unimate_notifications"
    private const val KEY = "items"

    fun upsert(context: Context, item: NotificationItem) {
        val list = loadAll(context).toMutableList()
        val idx = list.indexOfFirst { it.notificationId == item.notificationId }
        if (idx >= 0) {
            list[idx] = item
        } else {
            list.add(item)
        }
        saveAll(context, list)
    }

    fun loadAll(context: Context): List<NotificationItem> {
        val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?: return emptyList()

        val arr = try {
            JSONArray(raw)
        } catch (e: Exception) {
            return emptyList()
        }

        val items = ArrayList<NotificationItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val item = fromJson(obj) ?: continue
            items.add(item)
        }
        return items
    }

    fun mergeWithServer(
        local: List<NotificationItem>,
        server: List<NotificationServerItem>
    ): List<NotificationItem> {
        val map = LinkedHashMap<Long, NotificationItem>()
        for (item in local) {
            map[item.notificationId] = item
        }
        for (s in server) {
            val l = map[s.notificationId]
            map[s.notificationId] = if (l == null) {
                s.toNotificationItem(isCompleted = s.isCompleted ?: false)
            } else {
                mergeItem(l, s)
            }
        }
        return map.values.sortedByDescending { it.createdAtMillisOrMin() }
    }

    private fun mergeItem(local: NotificationItem, server: NotificationServerItem): NotificationItem {
        val completed = server.isCompleted ?: local.isCompleted
        return local.copy(
            teamName = server.teamName,
            teamColorHex = server.teamColorHex,
            alarmType = server.alarmType,
            messageTitle = server.messageTitle,
            messageBody = server.messageBody,
            createdAt = server.createdAt,
            isCompleted = completed
        )
    }

    private fun saveAll(context: Context, items: List<NotificationItem>) {
        val arr = JSONArray()
        for (item in items) {
            arr.put(toJson(item))
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .apply()
    }

    private fun toJson(item: NotificationItem): JSONObject {
        return JSONObject()
            .put("notificationId", item.notificationId)
            .put("teamId", item.teamId)
            .put("teamName", item.teamName)
            .put("teamColorHex", item.teamColorHex)
            .put("alarmType", item.alarmType)
            .put("messageTitle", item.messageTitle)
            .put("messageBody", item.messageBody)
            .put("createdAt", item.createdAt)
            .put("isCompleted", item.isCompleted)
    }

    private fun fromJson(obj: JSONObject): NotificationItem? {
        val notificationId = obj.optLong("notificationId", -1L)
        if (notificationId <= 0) return null

        return NotificationItem(
            notificationId = notificationId,
            teamId = obj.optLong("teamId", 0L),
            teamName = obj.optString("teamName", "Unknown").ifBlank { "Unknown" },
            teamColorHex = obj.optString("teamColorHex", "#CCCCCC").ifBlank { "#CCCCCC" },
            alarmType = obj.optString("alarmType", "알림"),
            messageTitle = obj.optString("messageTitle", ""),
            messageBody = obj.optString("messageBody", ""),
            createdAt = obj.optString("createdAt", ""),
            isCompleted = obj.optBoolean("isCompleted", false)
        )
    }
}
