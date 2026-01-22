package com.project.unimate.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.project.unimate.network.ApiClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val TAG = "UnimateLogin"
    private val baseUrl = "https://seok-hwan1.duckdns.org"
    private val authApi = AuthApi(baseUrl)

    fun onLoginSuccessSaveJwtAndRegisterFcm(jwt: String) {
        JwtStore.save(getApplication(), jwt)
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
                    val jwt = JSONObject(body).optString("token", "")
                    if (jwt.isNotBlank()) {
                        onLoginSuccessSaveJwtAndRegisterFcm(jwt)
                        onDone(true, null)
                        return
                    }
                }
                onDone(false, "login failed code=$code body=$body")
            }
        })
    }

    fun kakaoCallbackLogin(code: String, onDone: (Boolean, String?) -> Unit) {
        val req = authApi.kakaoCallbackRequest(code)
        ApiClient.http.newCall(req).enqueue(callbackToJwt(onDone))
    }

    fun naverCallbackLogin(code: String, state: String, onDone: (Boolean, String?) -> Unit) {
        val req = authApi.naverCallbackRequest(code, state)
        ApiClient.http.newCall(req).enqueue(callbackToJwt(onDone))
    }

    private fun callbackToJwt(onDone: (Boolean, String?) -> Unit): Callback {
        return object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                Log.d(TAG, "social callback resp code=$code body=$body")

                if (code in 200..299 && !body.isNullOrBlank()) {
                    val jwt = JSONObject(body).optString("token", "")
                    if (jwt.isNotBlank()) {
                        onLoginSuccessSaveJwtAndRegisterFcm(jwt)
                        onDone(true, null)
                        return
                    }
                }
                onDone(false, "social login failed code=$code body=$body")
            }
        }
    }
}
