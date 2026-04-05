package com.tcc.androidnative.core.config

import com.tcc.androidnative.BuildConfig

object ApiConfig {
    const val DEFAULT_BASE_URL: String = "http://10.0.2.2:8080"
    const val GOOGLE_CALENDAR_READONLY_SCOPE: String = "https://www.googleapis.com/auth/calendar.readonly"
    val BASE_URL: String = BuildConfig.API_BASE_URL.ifBlank { DEFAULT_BASE_URL }

    fun retrofitBaseUrl(): String {
        return if (BASE_URL.endsWith("/")) BASE_URL else "$BASE_URL/"
    }
}
