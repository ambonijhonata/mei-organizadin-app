package com.tcc.androidnative.core.network

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class RetryOnFailureInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isRetryableMethod(request.method)) {
            return chain.proceed(request)
        }

        var attempt = 0
        var lastError: IOException? = null
        while (attempt <= MAX_RETRIES) {
            try {
                return chain.proceed(request)
            } catch (error: IOException) {
                lastError = error
                val shouldRetry = attempt < MAX_RETRIES &&
                    error.isLikelyTransientNetworkFailure() &&
                    !chain.call().isCanceled()
                if (!shouldRetry) {
                    throw error
                }
                Thread.sleep(RETRY_BACKOFF_MS[attempt])
                attempt += 1
            }
        }

        throw lastError ?: IOException("Unexpected empty retry failure")
    }

    private fun isRetryableMethod(method: String): Boolean {
        return RETRYABLE_METHODS.contains(method.uppercase())
    }

    private companion object {
        val RETRYABLE_METHODS = setOf("GET", "HEAD", "OPTIONS")
        val RETRY_BACKOFF_MS = listOf(300L, 900L)
        const val MAX_RETRIES = 2
    }
}
