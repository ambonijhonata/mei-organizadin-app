package com.tcc.androidnative.feature.auth.data.remote.dto

data class LoginRequestDto(
    val idToken: String,
    val authorizationCode: String,
    val deviceId: String? = null,
    val appVersion: String? = null
)

data class LoginResponseDto(
    val userId: Long,
    val email: String,
    val name: String,
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)

data class RefreshRequestDto(
    val refreshToken: String,
    val deviceId: String? = null,
    val appVersion: String? = null
)

data class RefreshResponseDto(
    val accessToken: String,
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)

data class LogoutRequestDto(
    val refreshToken: String
)
