package com.project.unimate.network.dto

import com.google.gson.annotations.SerializedName

// === Request ===

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val email: String,
    val password: String,
    val nickname: String
)

data class SocialLoginRequest(
    val provider: String,
    val accessToken: String,
    val email: String? = null,
    val nickname: String? = null
)

data class EmailVerificationRequest(
    val email: String
)

data class EmailVerificationConfirmRequest(
    val email: String,
    val code: String
)

data class FindEmailRequest(
    val email: String,
    val code: String
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

// === Response ===

data class AuthResponse(
    val token: String?,
    val userId: Long?,
    val email: String?,
    val nickname: String?
)

data class AuthorizeUrlResponse(
    val authorizeUrl: String?,
    val state: String?
)

data class UserResponse(
    val id: Long?,
    val email: String?,
    val nickname: String?,
    val emailVerified: Boolean?,
    val provider: String?,
    val providerId: String?,
    val active: Boolean?,
    val profileImageUrl: String?,
    val name: String?,
    val birthDate: String?,
    val gender: String?,
    val phoneNumber: String?,
    val createdAt: String?,
    val updatedAt: String?,
    val universityId: Long?,
    val universityName: String?,
    val profileCompleted: Boolean?
)

data class ProfileUpsertRequest(
    val nickname: String,
    val universityId: Long,
    val profileImageUrl: String? = null
)

data class UniversityItemResponse(
    val id: Long,
    val name: String
)
