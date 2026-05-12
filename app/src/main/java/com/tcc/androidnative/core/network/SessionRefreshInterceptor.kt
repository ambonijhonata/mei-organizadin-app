package com.tcc.androidnative.core.network

import com.tcc.androidnative.core.session.RefreshOutcome
import com.tcc.androidnative.core.session.RefreshTrigger
import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.SessionRefreshCoordinator
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class SessionRefreshInterceptor @Inject constructor(
    private val sessionRefreshCoordinator: SessionRefreshCoordinator,
    private val sessionManager: SessionManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isAuthFlowPath(request.url.encodedPath) || request.header(RETRY_HEADER) == RETRY_MARKER) {
            return chain.proceed(request)
        }

        val response = chain.proceed(request)
        if (response.code != HTTP_UNAUTHORIZED) {
            return response
        }

        val currentAccessToken = sessionManager.getAccessToken()
        val requestAccessToken = extractBearerToken(request.header("Authorization"))
        if (!requestAccessToken.isNullOrBlank() &&
            !currentAccessToken.isNullOrBlank() &&
            requestAccessToken != currentAccessToken
        ) {
            logInfo("session_refresh_retry_latest_token_without_refresh path=${request.url.encodedPath}")
            return retryOnceWithCurrentToken(
                chain = chain,
                request = request,
                response = response,
                accessToken = currentAccessToken
            )
        }

        val refreshOutcome = runBlocking {
            sessionRefreshCoordinator.refreshIfNeeded(
                force = true,
                trigger = RefreshTrigger.REACTIVE_401
            )
        }

        return when (refreshOutcome) {
            is RefreshOutcome.Success -> {
                logInfo("session_refresh_retry_after_outcome kind=${refreshOutcome.kind}")
                retryOnceWithCurrentToken(
                    chain = chain,
                    request = request,
                    response = response,
                    accessToken = sessionManager.getAccessToken()
                )
            }
            is RefreshOutcome.TerminalFailure -> {
                logWarn("session_refresh_terminal_failure path=${request.url.encodedPath} code=${refreshOutcome.code.value}")
                response
            }
            is RefreshOutcome.RecoverableFailure -> {
                logWarn(
                    "session_refresh_recoverable_failure path=${request.url.encodedPath} " +
                        "reason=${refreshOutcome.message ?: "recoverable_failure"}"
                )
                response
            }
            RefreshOutcome.NoSession -> response
            RefreshOutcome.NotNeeded -> response
        }
    }

    private fun retryOnceWithCurrentToken(
        chain: Interceptor.Chain,
        request: okhttp3.Request,
        response: Response,
        accessToken: String?
    ): Response {
        response.close()
        val retriedRequest = request.newBuilder()
            .header(RETRY_HEADER, RETRY_MARKER)
            .apply {
                if (!accessToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $accessToken")
                }
            }
            .build()
        return chain.proceed(retriedRequest)
    }

    private fun extractBearerToken(authorizationHeader: String?): String? {
        if (authorizationHeader.isNullOrBlank()) return null
        if (!authorizationHeader.startsWith("Bearer ")) return null
        return authorizationHeader.removePrefix("Bearer ").trim()
    }

    private fun isAuthFlowPath(path: String): Boolean {
        return path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/refresh") ||
            path.startsWith("/api/auth/logout")
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logWarn(message: String) {
        runCatching { Log.w(TAG, message) }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
        const val RETRY_HEADER = "X-Session-Retry"
        const val RETRY_MARKER = "1"
        const val TAG = "SessionRefreshInterceptor"
    }
}
