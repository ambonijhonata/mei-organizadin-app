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
        val response = chain.proceed(chain.request())
        if (shouldInvalidateSession(response.code)) {
            sessionManager.clearSession()
        }
        return response
    }

    internal fun shouldInvalidateSession(responseCode: Int): Boolean = responseCode == 401
}
