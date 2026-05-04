package com.tcc.androidnative.core.network

import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthTokenInterceptorTest {
    @Test
    fun `should attach bearer token for protected endpoints`() {
        val sessionManager = TokenSessionManager(validSession())
        val interceptor = AuthTokenInterceptor(sessionManager)
        val request = Request.Builder().url("https://example.com/api/clients").build()
        val chain = CaptureAuthHeaderChain(request)

        interceptor.intercept(chain)

        assertEquals("Bearer access-token", chain.lastAuthorizationHeader)
    }

    @Test
    fun `should not attach bearer token for auth endpoints`() {
        val sessionManager = TokenSessionManager(validSession())
        val interceptor = AuthTokenInterceptor(sessionManager)
        val request = Request.Builder().url("https://example.com/api/auth/refresh").build()
        val chain = CaptureAuthHeaderChain(request)

        interceptor.intercept(chain)

        assertNull(chain.lastAuthorizationHeader)
    }

    private fun validSession(): UserSession {
        return UserSession(
            userId = 1L,
            email = "user@test.com",
            name = "User",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            accessTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 3600L,
            refreshTokenExpiresAtEpochSeconds = (System.currentTimeMillis() / 1000L) + 7200L
        )
    }
}

private class CaptureAuthHeaderChain(private val initialRequest: Request) : Interceptor.Chain {
    var lastAuthorizationHeader: String? = null
        private set
    private val fakeCall = AuthTokenTestCall(initialRequest)

    override fun request(): Request = initialRequest

    override fun proceed(request: Request): Response {
        lastAuthorizationHeader = request.header("Authorization")
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody())
            .build()
    }

    override fun call(): Call = fakeCall
    override fun connection(): Connection? = null
    override fun connectTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()
    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    override fun readTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()
    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    override fun writeTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()
    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}

private class AuthTokenTestCall(private val request: Request) : Call {
    override fun request(): Request = request
    override fun execute(): Response = throw UnsupportedOperationException("Not needed")
    override fun enqueue(responseCallback: Callback) = throw UnsupportedOperationException("Not needed")
    override fun cancel() = Unit
    override fun isExecuted(): Boolean = false
    override fun isCanceled(): Boolean = false
    override fun timeout(): Timeout = Timeout.NONE
    override fun clone(): Call = AuthTokenTestCall(request)
}

private class TokenSessionManager(initial: UserSession?) : SessionManager {
    private val state = MutableStateFlow(initial)
    override val sessionState: StateFlow<UserSession?> = state

    override fun saveSession(session: UserSession) {
        state.value = session
    }

    override fun clearSession() {
        state.value = null
    }

    override fun currentSession(): UserSession? = state.value

    override fun getIdToken(): String? = state.value?.accessToken
}
