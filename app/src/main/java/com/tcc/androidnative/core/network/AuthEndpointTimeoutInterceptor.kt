package com.tcc.androidnative.core.network

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthEndpointTimeoutInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        if (!isAuthFlowPath(path)) {
            return chain.proceed(chain.request())
        }

        val authAwareChain = chain
            .withConnectTimeout(AUTH_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .withReadTimeout(AUTH_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .withWriteTimeout(AUTH_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        return authAwareChain.proceed(authAwareChain.request())
    }

    private fun isAuthFlowPath(path: String): Boolean {
        return path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/refresh") ||
            path.startsWith("/api/auth/logout")
    }

    private companion object {
        const val AUTH_CONNECT_TIMEOUT_SECONDS = 60
        const val AUTH_READ_TIMEOUT_SECONDS = 150
        const val AUTH_WRITE_TIMEOUT_SECONDS = 60
    }
}
