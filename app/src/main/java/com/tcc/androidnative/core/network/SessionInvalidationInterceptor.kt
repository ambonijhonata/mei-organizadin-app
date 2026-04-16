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
        if (shouldInvalidateSession(response.code, request.url.encodedPath)) {
            sessionManager.clearSession()
        }
        return response
    }

    internal fun shouldInvalidateSession(
        responseCode: Int,
        encodedPath: String? = null
    ): Boolean {
        if (responseCode != 401) return false
        if (encodedPath != null && isPaymentsPreloadPath(encodedPath)) return false
        return true
    }

    internal fun isPaymentsPreloadPath(encodedPath: String): Boolean {
        return encodedPath.matches(Regex("""/api/calendar/events/\d+/payments$"""))
    }
}
