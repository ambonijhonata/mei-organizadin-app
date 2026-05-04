package com.tcc.androidnative.core.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RetryOnFailureInterceptorTest {
    private val interceptor = RetryOnFailureInterceptor()

    @Test
    fun `should retry transient failure for GET`() {
        val request = Request.Builder()
            .url("https://example.com/api/calendar/events")
            .get()
            .build()
        val chain = FakeChain(
            request = request,
            proceedResults = listOf(
                { throw IOException("timeout") },
                { responseOf(request, 200) }
            )
        )

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(2, chain.proceedCalls)
    }

    @Test
    fun `should not retry POST`() {
        val request = Request.Builder()
            .url("https://example.com/api/clients")
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()
        val chain = FakeChain(
            request = request,
            proceedResults = listOf(
                { throw IOException("timeout") }
            )
        )

        assertThrows(IOException::class.java) {
            interceptor.intercept(chain)
        }
        assertEquals(1, chain.proceedCalls)
    }

    private fun responseOf(request: Request, code: Int): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body("{}".toResponseBody())
            .build()
    }
}

private class FakeChain(
    private val request: Request,
    private val proceedResults: List<() -> Response>
) : Interceptor.Chain {
    var proceedCalls: Int = 0
        private set

    private val fakeCall = FakeCall(request)

    override fun request(): Request = request

    override fun proceed(request: Request): Response {
        val index = proceedCalls
        proceedCalls += 1
        return proceedResults[index].invoke()
    }

    override fun call(): Call = fakeCall

    override fun connection(): Connection? = null

    override fun connectTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()

    override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun readTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(45).toInt()

    override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this

    override fun writeTimeoutMillis(): Int = TimeUnit.SECONDS.toMillis(30).toInt()

    override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
}

private class FakeCall(
    private val request: Request
) : Call {
    override fun request(): Request = request

    override fun execute(): Response {
        throw UnsupportedOperationException("Not needed in this test")
    }

    override fun enqueue(responseCallback: Callback) {
        throw UnsupportedOperationException("Not needed in this test")
    }

    override fun cancel() = Unit

    override fun isExecuted(): Boolean = false

    override fun isCanceled(): Boolean = false

    override fun timeout(): Timeout = Timeout.NONE

    override fun clone(): Call = FakeCall(request)
}
