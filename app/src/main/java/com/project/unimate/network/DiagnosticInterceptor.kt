package com.project.unimate.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * 모든 API 요청/응답을 "UniLog" 태그로 Logcat에 출력합니다.
 *
 * 확인 항목:
 * - 요청 URL, Method
 * - Authorization 헤더 존재 여부 및 앞 30자 (토큰 null 체크)
 * - 응답 코드 + message
 * - 응답 body 앞 1000자 (JSON 래퍼 구조 확인)
 * - 실패 응답(non-2xx) errorBody 전문
 */
class DiagnosticInterceptor : Interceptor {

    companion object {
        private const val TAG = "UniLog"
        private const val BODY_PREVIEW_LEN = 1000
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val method = request.method

        val authHeader = request.header("Authorization")
        val authLog = when {
            authHeader == null -> "❌ Authorization 헤더 없음 (JWT 미설정)"
            authHeader.startsWith("Bearer ") -> "✅ Bearer ${authHeader.removePrefix("Bearer ").take(20)}..."
            else -> "⚠️ 형식이상: ${authHeader.take(30)}"
        }

        Log.d(TAG, "━━━ REQUEST [$method] ━━━")
        Log.d(TAG, "  URL  : $url")
        Log.d(TAG, "  Auth : $authLog")

        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ 네트워크 오류: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }

        val code = response.code
        val message = response.message
        Log.d(TAG, "━━━ RESPONSE [$code $message] ━━━")
        Log.d(TAG, "  URL  : $url")

        // body를 읽고 다시 복원 (body는 1회만 읽을 수 있음)
        val contentType = response.body?.contentType()
        val rawBody = response.body?.string() ?: ""

        val bodyPreview = rawBody.take(BODY_PREVIEW_LEN)
        if (response.isSuccessful) {
            Log.d(TAG, "  ✅ 성공 | body(앞${BODY_PREVIEW_LEN}자): $bodyPreview")
        } else {
            Log.e(TAG, "  ❌ 실패[$code] | errorBody: $rawBody")
        }

        // body를 소비했으므로 새 Response로 복원
        return response.newBuilder()
            .body(rawBody.toResponseBody(contentType))
            .build()
    }
}
