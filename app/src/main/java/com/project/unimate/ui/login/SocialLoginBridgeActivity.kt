package com.project.unimate.ui.login

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.project.unimate.auth.AuthApi
import com.project.unimate.auth.LoginViewModel
import com.project.unimate.network.ApiClient
import com.project.unimate.network.Env
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class SocialLoginBridgeActivity : ComponentActivity() {

    private val baseUrl = Env.BASE_URL
    private val authApi = AuthApi(baseUrl)
    private val vm: LoginViewModel by viewModels()

    private val provider: String by lazy { intent.getStringExtra("provider") ?: "KAKAO" }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true

        val req = when (provider.uppercase()) {
            "NAVER" -> authApi.naverAuthorizeUrlRequest()
            else -> authApi.kakaoAuthorizeUrlRequest()
        }

        ApiClient.http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                finishWithError(e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()
                if (code !in 200..299 || body.isNullOrBlank()) {
                    finishWithError("authorize-url 요청 실패 (code=$code)")
                    return
                }

                val json = JSONObject(body)
                val authorizeUrl = json.optString("authorizeUrl", "")
                val state = json.optString("state", "")

                if (authorizeUrl.isBlank()) {
                    finishWithError("authorizeUrl이 비어있습니다")
                    return
                }

                runOnUiThread {
                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            return handleUrl(request.url, state)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            return handleUrl(Uri.parse(url), state)
                        }
                    }
                    webView.loadUrl(authorizeUrl)
                }
            }
        })
    }

    private fun handleUrl(uri: Uri, stateFromAuthorize: String): Boolean {
        val url = uri.toString()

        if (provider.uppercase() == "KAKAO" && url.startsWith("$baseUrl/api/auth/kakao/callback")) {
            val code = uri.getQueryParameter("code") ?: return true
            vm.kakaoCallbackLogin(code) { ok, err ->
                runOnUiThread {
                    if (!ok && !err.isNullOrBlank()) {
                        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        finishWithError(err)
                        return@runOnUiThread
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            return true
        }

        if (provider.uppercase() == "NAVER" && url.startsWith("$baseUrl/api/auth/naver/callback")) {
            val code = uri.getQueryParameter("code") ?: return true
            val state = uri.getQueryParameter("state") ?: stateFromAuthorize
            vm.naverCallbackLogin(code, state) { ok, err ->
                runOnUiThread {
                    if (!ok && !err.isNullOrBlank()) {
                        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                        finishWithError(err)
                        return@runOnUiThread
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
            return true
        }

        return false
    }

    private fun finishWithError(message: String?) {
        val intent = Intent().putExtra("error", message ?: "로그인 실패")
        setResult(Activity.RESULT_CANCELED, intent)
        finish()
    }
}
