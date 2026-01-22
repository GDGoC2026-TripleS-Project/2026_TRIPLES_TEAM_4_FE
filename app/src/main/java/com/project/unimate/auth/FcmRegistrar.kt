package com.project.unimate.auth

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.project.unimate.network.ApiClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

object FcmRegistrar {
    private const val TAG = "UnimateFCM"
    private const val PLATFORM = "ANDROID"

    fun registerIfPossible(context: Context, baseUrl: String) {
        val jwt = JwtStore.load(context)
        if (jwt.isNullOrBlank()) {
            Log.d(TAG, "FCM register skip: JWT empty")
            return
        }

        val deviceId = DeviceIdProvider.getOrCreate(context)

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { fcm ->
                logTokenForDebug(context, fcm)

                val req = FcmTokenApi(baseUrl).registerMyToken(
                    jwt = jwt,
                    fcmToken = fcm,
                    deviceId = deviceId,
                    platform = PLATFORM
                )

                ApiClient.http.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "FCM token register fail(network)", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val code = response.code
                        val body = response.body?.string()
                        response.close()
                        Log.d(TAG, "FCM token register resp code=$code body=$body")
                    }
                })
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FCM token fetch fail", e)
            }
    }

    private fun logTokenForDebug(context: Context, token: String) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) Log.d(TAG, "FCM_TOKEN_FULL=$token")
        else Log.d(TAG, "FCM_TOKEN=${token.take(12)}...")
    }
}
