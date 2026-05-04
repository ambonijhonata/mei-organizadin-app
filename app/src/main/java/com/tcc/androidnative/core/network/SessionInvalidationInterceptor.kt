package com.tcc.androidnative.core.network

import com.tcc.androidnative.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionInvalidationInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val responseBodyPreview = runCatching { response.peekBody(4096).string() }.getOrDefault("")
        if (shouldInvalidateSession(response.code, request.url.encodedPath, responseBodyPreview)) {
            sessionManager.clearSession()
        }
        return response
    }

    internal fun shouldInvalidateSession(
        responseCode: Int,
        encodedPath: String? = null,
        responseBody: String? = null
    ): Boolean {
        if (responseCode != 401) return false
        if (encodedPath != null && isPaymentsPreloadPath(encodedPath)) return false
        if (encodedPath != null && encodedPath.startsWith("/api/auth/refresh")) {
            val body = responseBody.orEmpty()
            return body.contains("REFRESH_TOKEN_INVALID") ||
                body.contains("REFRESH_TOKEN_REVOKED") ||
                body.contains("REFRESH_TOKEN_REUSED") ||
                body.contains("REFRESH_TOKEN_EXPIRED")
        }
        return false
    }

    internal fun isPaymentsPreloadPath(encodedPath: String): Boolean {
        return encodedPath.matches(Regex("""/api/calendar/events/\d+/payments$"""))
    }
}
