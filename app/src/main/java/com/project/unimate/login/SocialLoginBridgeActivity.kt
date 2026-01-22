package com.project.unimate.login

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.project.unimate.auth.AuthApi
import com.project.unimate.auth.LoginViewModel
import com.project.unimate.network.ApiClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class SocialLoginBridgeActivity : ComponentActivity() {

    private val baseUrl = "https://seok-hwan1.duckdns.org"
    private val authApi = AuthApi(baseUrl)
    private val vm: LoginViewModel by viewModels()

    // intent extras
    private val provider: String by lazy { intent.getStringExtra("provider") ?: "KAKAO" }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)
        webView.settings.javaScriptEnabled = true

        // 1) authorize-url 호출해서 URL 얻고 WebView로 열기
        val req = when (provider.uppercase()) {
            "NAVER" -> authApi.naverAuthorizeUrlRequest()
            else -> authApi.kakaoAuthorizeUrlRequest()
        }

        ApiClient.http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                finish()
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()
                if (code !in 200..299 || body.isNullOrBlank()) {
                    finish()
                    return
                }

                val json = JSONObject(body)
                val authorizeUrl = json.optString("authorizeUrl", "")
                val state = json.optString("state", "")

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

        // 백엔드 콜백 URL로 이동하는 순간 가로채기
        if (provider.uppercase() == "KAKAO" && url.startsWith("$baseUrl/api/auth/kakao/callback")) {
            val code = uri.getQueryParameter("code") ?: return true
            vm.kakaoCallbackLogin(code) { ok, _ ->
                runOnUiThread { finish() }
            }
            return true
        }

        if (provider.uppercase() == "NAVER" && url.startsWith("$baseUrl/api/auth/naver/callback")) {
            val code = uri.getQueryParameter("code") ?: return true
            val state = uri.getQueryParameter("state") ?: stateFromAuthorize
            vm.naverCallbackLogin(code, state) { ok, _ ->
                runOnUiThread { finish() }
            }
            return true
        }

        return false
    }
}
