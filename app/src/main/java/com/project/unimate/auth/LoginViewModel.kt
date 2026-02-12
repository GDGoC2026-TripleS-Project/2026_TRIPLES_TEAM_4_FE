package com.project.unimate.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.project.unimate.network.ApiClient
import com.project.unimate.network.Env
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG = "UnimateLogin"
    private val baseUrl = Env.BASE_URL
    private val authApi = AuthApi(baseUrl)

    private data class AuthPayload(
        val token: String,
        val userId: Long?,
        val email: String?,
        val nickname: String?
    )

    fun onLoginSuccessSaveJwtAndRegisterFcm(jwt: String, userId: Long?) {
        JwtStore.save(getApplication(), jwt)
        if (userId != null && userId > 0) {
            JwtStore.saveUserId(getApplication(), userId)
        }
        FcmRegistrar.registerIfPossible(getApplication(), baseUrl)
    }

    fun emailLogin(email: String, password: String, onDone: (Boolean, String?) -> Unit) {
        val req = authApi.emailLoginRequest(email, password)
        ApiClient.http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                if (code in 200..299 && !body.isNullOrBlank()) {
                    val auth = parseAuthResponse(body)
                    if (auth != null && auth.token.isNotBlank()) {
                        onLoginSuccessSaveJwtAndRegisterFcm(auth.token, auth.userId)
                        Log.d(TAG, "email login ok userId=${auth.userId} email=${auth.email} nickname=${auth.nickname}")
                        onDone(true, null)
                        return
                    }
                }
                onDone(false, parseErrorMessage(code, body))
            }
        })
    }

    fun socialLogin(provider: String, accessToken: String, onDone: (Boolean, String?) -> Unit) {
        val req = authApi.socialLoginRequest(provider, accessToken)
        ApiClient.http.newCall(req).enqueue(callbackToJwt("social", onDone))
    }

    fun kakaoCallbackLogin(code: String, onDone: (Boolean, String?) -> Unit) {
        val req = authApi.kakaoCallbackRequest(code)
        ApiClient.http.newCall(req).enqueue(callbackToJwt("kakao-callback", onDone))
    }

    fun naverCallbackLogin(code: String, state: String, onDone: (Boolean, String?) -> Unit) {
        val req = authApi.naverCallbackRequest(code, state)
        ApiClient.http.newCall(req).enqueue(callbackToJwt("naver-callback", onDone))
    }

    private fun callbackToJwt(tag: String, onDone: (Boolean, String?) -> Unit): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                Log.d(TAG, "auth[$tag] resp code=$code body=$body")

                if (code in 200..299 && !body.isNullOrBlank()) {
                    val auth = parseAuthResponse(body)
                    if (auth != null && auth.token.isNotBlank()) {
                        onLoginSuccessSaveJwtAndRegisterFcm(auth.token, auth.userId)
                        Log.d(TAG, "social login ok userId=${auth.userId} email=${auth.email} nickname=${auth.nickname}")
                        onDone(true, null)
                        return
                    }
                }
                onDone(false, parseErrorMessage(code, body))
            }
        }
    }

    private fun parseAuthResponse(body: String): AuthPayload? {
        return try {
            val json = JSONObject(body)
            val token = json.optString("token", "")
            val userId = json.optLong("userId", -1L).takeIf { it > 0 }
            val email = json.optString("email", null)
            val nickname = json.optString("nickname", null)
            AuthPayload(token, userId, email, nickname)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseErrorMessage(code: Int, body: String?): String {
        if (body.isNullOrBlank()) return "요청 실패 (code=$code)"

        try {
            val json = JSONObject(body)

            // 1) validation errors
            val errorsObj = json.optJSONObject("errors")
            if (errorsObj != null && errorsObj.length() > 0) {
                val msgs = errorsObj.keys().asSequence()
                    .map { key -> errorsObj.optString(key) }
                    .filter { it.isNotBlank() }
                    .toList()
                if (msgs.isNotEmpty()) return msgs.joinToString("\n")
            }

            // 2) message field
            val message = json.optString("message", "")
            if (message.isNotBlank()) {
                return when (message) {
                    "KAKAO_TOKEN_EXCHANGE_FAILED" -> "카카오 인증에 실패했습니다. 다시 시도해주세요."
                    "NAVER_TOKEN_EXCHANGE_FAILED" -> "네이버 인증에 실패했습니다. 다시 시도해주세요."
                    else -> message
                }
            }

            // 3) code field
            val errCode = json.optString("code", "")
            if (errCode.isNotBlank()) return errCode
        } catch (e: Exception) {
            // ignore parse errors
        }

        return "요청 실패 (code=$code)"
    }

    fun fetchProfileCompleted(onDone: (Boolean?, String?) -> Unit) {
        val jwt = JwtStore.load(getApplication())
        if (jwt.isNullOrBlank()) {
            onDone(null, "로그인이 필요합니다")
            return
        }

        val req = Request.Builder()
            .url("$baseUrl/api/users/me")
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()

        ApiClient.http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(null, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                if (code in 200..299 && !body.isNullOrBlank()) {
                    try {
                        val json = JSONObject(body)
                        val completed = json.optBoolean("profileCompleted", false)
                        onDone(completed, null)
                        return
                    } catch (e: Exception) {
                        onDone(null, "프로필 상태 파싱 실패")
                        return
                    }
                }
                onDone(null, parseErrorMessage(code, body))
            }
        })
    }
}
