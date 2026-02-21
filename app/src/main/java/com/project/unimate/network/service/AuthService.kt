package com.project.unimate.network.service

import com.project.unimate.network.dto.*
import retrofit2.Response
import retrofit2.http.*

// 역할: 로그인·회원가입·OAuth·이메일 인증·비밀번호 API

interface AuthService {

    // 로그인·회원가입

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("api/auth/social/login")
    suspend fun socialLogin(@Body request: SocialLoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Map<String, String>>

    @DELETE("api/auth/account")
    suspend fun deleteAccount(): Response<Map<String, String>>

    // OAuth

    @GET("api/auth/kakao/authorize-url")
    suspend fun kakaoAuthorizeUrl(): Response<AuthorizeUrlResponse>

    @GET("api/auth/naver/authorize-url")
    suspend fun naverAuthorizeUrl(): Response<AuthorizeUrlResponse>

    @GET("api/auth/kakao/callback")
    suspend fun kakaoCallback(@Query("code") code: String): Response<AuthResponse>

    @GET("api/auth/naver/callback")
    suspend fun naverCallback(
        @Query("code") code: String,
        @Query("state") state: String
    ): Response<AuthResponse>

    // --- 이메일 인증 ---

    @POST("api/auth/email/verification/send")
    suspend fun sendVerificationCode(@Body request: EmailVerificationRequest): Response<Map<String, String>>

    @POST("api/auth/email/verification/confirm")
    suspend fun confirmVerificationCode(@Body request: EmailVerificationConfirmRequest): Response<Map<String, String>>

    @POST("api/auth/email/find")
    suspend fun findEmail(@Body request: FindEmailRequest): Response<Map<String, String>>

    // 닉네임

    @GET("api/auth/nickname/check")
    suspend fun checkNickname(@Query("nickname") nickname: String): Response<Map<String, String>>

    // 비밀번호

    @POST("api/auth/password/change")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Map<String, String>>

    @POST("api/auth/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Map<String, String>>
}
