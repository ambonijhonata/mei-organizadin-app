package com.tcc.androidnative.feature.auth.data.remote

import com.tcc.androidnative.feature.auth.data.remote.dto.LoginRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.LoginResponseDto
import com.tcc.androidnative.feature.auth.data.remote.dto.LogoutRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.RefreshRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.RefreshResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("/api/auth/login")
    suspend fun login(@Body body: LoginRequestDto): LoginResponseDto

    @POST("/api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): RefreshResponseDto

    @POST("/api/auth/logout")
    suspend fun logout(@Body body: LogoutRequestDto)
}
