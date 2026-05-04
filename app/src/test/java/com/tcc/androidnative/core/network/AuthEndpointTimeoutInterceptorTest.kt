package com.tcc.androidnative.core.network

import java.util.concurrent.TimeUnit
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
import org.junit.Test

class AuthEndpointTimeoutInterceptorTest {
    @Test
    fun `should apply extended timeouts to auth endpoints`() {
        val interceptor = AuthEndpointTimeoutInterceptor()
        val request = Request.Builder().url("https://example.com/api/auth/refresh").build()
        val chain = TimeoutCaptureChain(request)

        interceptor.intercept(chain)

        assertEquals(60_000, chain.connectTimeoutMillis())
        assertEquals(150_000, chain.readTimeoutMillis())
        assertEquals(60_000, chain.writeTimeoutMillis())
    }

    @Test
    fun `should keep default timeouts for non-auth endpoints`() {
        val interceptor = AuthEndpointTimeoutInterceptor()
        val request = Request.Builder().url("https://example.com/api/clients").build()
        val chain = TimeoutCaptureChain(request)

        interceptor.intercept(chain)

        assertEquals(30_000, chain.connectTimeoutMillis())
        assertEquals(45_000, chain.readTimeoutMillis())
        assertEquals(30_000, chain.writeTimeoutMillis())
    }
}

private class TimeoutCaptureChain(private val initialRequest: Request) : Interceptor.Chain {
    private var connectTimeoutMs = 30_000
    private var readTimeoutMs = 45_000
    private var writeTimeoutMs = 30_000
    private val fakeCall = TimeoutTestCall(initialRequest)

    override fun request(): Request = initialRequest

    override fun proceed(request: Request): Response {
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

    override fun connectTimeoutMillis(): Int = connectTimeoutMs
    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
        connectTimeoutMs = unit.toMillis(timeout.toLong()).toInt()
        return this
    }

    override fun readTimeoutMillis(): Int = readTimeoutMs
    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
        readTimeoutMs = unit.toMillis(timeout.toLong()).toInt()
        return this
    }

    override fun writeTimeoutMillis(): Int = writeTimeoutMs
    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain {
        writeTimeoutMs = unit.toMillis(timeout.toLong()).toInt()
        return this
    }
}

private class TimeoutTestCall(private val request: Request) : Call {
    override fun request(): Request = request
    override fun execute(): Response = throw UnsupportedOperationException("Not needed")
    override fun enqueue(responseCallback: Callback) = throw UnsupportedOperationException("Not needed")
    override fun cancel() = Unit
    override fun isExecuted(): Boolean = false
    override fun isCanceled(): Boolean = false
    override fun timeout(): Timeout = Timeout.NONE
    override fun clone(): Call = TimeoutTestCall(request)
}
