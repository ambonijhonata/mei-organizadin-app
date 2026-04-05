package com.tcc.androidnative.feature.auth.data.remote

import com.tcc.androidnative.feature.auth.data.remote.dto.LoginRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.LoginResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto
}

