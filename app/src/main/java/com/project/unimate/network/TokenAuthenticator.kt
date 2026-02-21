package com.project.unimate.network

// 역할: 401 시 리프레시 토큰으로 액세스 갱신 후 재요청. /api/auth/refresh 호출은 재인증 제외, 2회 이상 연쇄 시 중단

import android.content.Context
import com.project.unimate.auth.JwtStore
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject

class TokenAuthenticator(
    private val appContext: Context
) : Authenticator {

    private val lock = Any()
    private val refreshClient: OkHttpClient = OkHttpClient.Builder().build()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override fun authenticate(route: Route?, response: Response): Request? {
        val req = response.request
        if (req.url.encodedPath.contains("/api/auth/refresh")) return null
        if (responseCount(response) >= 2) return null

        val authHeader = req.header("Authorization") ?: return null
        if (!authHeader.startsWith("Bearer ")) return null

        synchronized(lock) {
            val latestAccess = JwtStore.loadAccessToken(appContext)
            val requestAccess = authHeader.removePrefix("Bearer ").trim()

            if (!latestAccess.isNullOrBlank() && latestAccess != requestAccess) {
                return req.newBuilder()
                    .header("Authorization", "Bearer $latestAccess")
                    .build()
            }

            val refreshToken = JwtStore.loadRefreshToken(appContext) ?: return null
            val refreshed = refreshAccessToken(refreshToken) ?: return null

            JwtStore.saveAccessToken(appContext, refreshed.accessToken)
            refreshed.refreshToken?.let { JwtStore.saveRefreshToken(appContext, it) }

            return req.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun refreshAccessToken(refreshToken: String): RefreshResult? {
        return try {
            val body = JSONObject()
                .put("refreshToken", refreshToken)
                .toString()
                .toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("${Env.BASE_URL}/api/auth/refresh")
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            refreshClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return null

                val json = JSONObject(raw)
                val nested = json.optJSONObject("data")

                val accessToken = firstNonBlank(
                    json.optString("accessToken"),
                    json.optString("token"),
                    json.optString("jwt"),
                    nested?.optString("accessToken"),
                    nested?.optString("token"),
                    nested?.optString("jwt")
                ) ?: return null

                val newRefresh = firstNonBlank(
                    json.optString("refreshToken"),
                    json.optString("newRefreshToken"),
                    nested?.optString("refreshToken"),
                    nested?.optString("newRefreshToken")
                )

                RefreshResult(accessToken = accessToken, refreshToken = newRefresh)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()
    }

    private data class RefreshResult(
        val accessToken: String,
        val refreshToken: String?
    )
}
