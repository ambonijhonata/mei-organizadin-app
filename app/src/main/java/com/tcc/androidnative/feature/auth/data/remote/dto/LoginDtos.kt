package com.tcc.androidnative.feature.auth.data.remote.dto

data class LoginRequestDto(
    val idToken: String,
    val authorizationCode: String
)

data class LoginResponseDto(
    val userId: Long,
    val email: String,
    val name: String
)

