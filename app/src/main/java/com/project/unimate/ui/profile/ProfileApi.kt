package com.project.unimate.ui.profile

import android.content.Context
import com.project.unimate.auth.JwtStore
import com.project.unimate.network.ApiClient
import com.project.unimate.network.Env
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ProfileApi(
    private val baseUrl: String = Env.BASE_URL
) {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun searchUniversities(context: Context, query: String, limit: Int = 10, onDone: (List<UniversityItem>, String?) -> Unit) {
        val q = query.trim()
        if (q.isEmpty()) {
            onDone(emptyList(), null)
            return
        }

        val jwt = JwtStore.load(context)
        if (jwt.isNullOrBlank()) {
            onDone(emptyList(), "로그인이 필요합니다")
            return
        }

        val url = "$baseUrl/api/universities/search?q=${encode(q)}&limit=$limit"
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $jwt")
            .build()

        ApiClient.http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(emptyList(), e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                if (code !in 200..299 || body.isNullOrBlank()) {
                    onDone(emptyList(), "학교 검색 실패 (code=$code)")
                    return
                }

                val list = parseUniversityList(body)
                onDone(list, null)
            }
        })
    }

    fun upsertProfile(
        context: Context,
        nickname: String,
        universityId: Long,
        profileImageUrl: String?,
        onDone: (Boolean, String?) -> Unit
    ) {
        val jwt = JwtStore.load(context)
        if (jwt.isNullOrBlank()) {
            onDone(false, "로그인이 필요합니다")
            return
        }

        val bodyObj = JSONObject()
            .put("nickname", nickname)
            .put("universityId", universityId)
        if (!profileImageUrl.isNullOrBlank()) {
            bodyObj.put("profileImageUrl", profileImageUrl)
        }

        val req = Request.Builder()
            .url("$baseUrl/api/users/me")
            .put(bodyObj.toString().toRequestBody(JSON))
            .addHeader("Authorization", "Bearer $jwt")
            .build()

        ApiClient.http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onDone(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val body = response.body?.string()
                response.close()

                if (code in 200..299) {
                    onDone(true, null)
                    return
                }

                onDone(false, parseErrorMessage(code, body))
            }
        })
    }

    fun uploadProfileImage(
        context: Context,
        uri: android.net.Uri,
        onDone: (String?, String?) -> Unit
    ) {
        val jwt = JwtStore.load(context)
        if (jwt.isNullOrBlank()) {
            onDone(null, "로그인이 필요합니다")
            return
        }

        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/*"
        val fileName = queryDisplayName(contentResolver, uri) ?: "profile.jpg"

        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }

        if (bytes == null || bytes.isEmpty()) {
            onDone(null, "이미지를 읽을 수 없습니다")
            return
        }

        val fileBody = bytes.toRequestBody(mimeType.toMediaType())
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()

        val req = Request.Builder()
            .url("$baseUrl/api/users/me/profile-image")
            .post(multipart)
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
                        val url = json.optString("imageUrl", "")
                        if (url.isNotBlank()) {
                            onDone(url, null)
                            return
                        }
                    } catch (e: Exception) {
                        // fall through
                    }
                }
                onDone(null, parseErrorMessage(code, body))
            }
        })
    }

    private fun parseUniversityList(body: String): List<UniversityItem> {
        val arr = try {
            JSONArray(body)
        } catch (e: Exception) {
            return emptyList()
        }

        val out = ArrayList<UniversityItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optLong("id", -1L)
            val name = obj.optString("name", "")
            if (id > 0 && name.isNotBlank()) {
                out.add(UniversityItem(id, name))
            }
        }
        return out
    }

    private fun parseErrorMessage(code: Int, body: String?): String {
        if (body.isNullOrBlank()) return "요청 실패 (code=$code)"

        try {
            val json = JSONObject(body)

            val errorsObj = json.optJSONObject("errors")
            if (errorsObj != null && errorsObj.length() > 0) {
                val msgs = errorsObj.keys().asSequence()
                    .map { key -> errorsObj.optString(key) }
                    .filter { it.isNotBlank() }
                    .toList()
                if (msgs.isNotEmpty()) return msgs.joinToString("\n")
            }

            val message = json.optString("message", "")
            if (message.isNotBlank()) return message

            val errCode = json.optString("code", "")
            if (errCode.isNotBlank()) return errCode
        } catch (e: Exception) {
            // ignore parse errors
        }

        return "요청 실패 (code=$code)"
    }

    private fun queryDisplayName(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri
    ): String? {
        val cursor = resolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIdx >= 0 && it.moveToFirst()) {
                return it.getString(nameIdx)
            }
        }
        return null
    }

    private fun encode(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8")
    }
}
