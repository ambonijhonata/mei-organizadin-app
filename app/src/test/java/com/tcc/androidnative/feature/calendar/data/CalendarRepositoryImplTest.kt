package com.tcc.androidnative.feature.calendar.data

import com.tcc.androidnative.feature.calendar.data.remote.CalendarApi
import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarEventDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.IntegrationStatusDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.SpringPageDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.SyncResponseDto
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class CalendarRepositoryImplTest {
    @Test
    fun `sync should return success outcome when api sync succeeds`() = runBlocking {
        val repository = CalendarRepositoryImpl(
            FakeCalendarApi(
                syncResult = SyncResponseDto(created = 2, updated = 1, deleted = 0)
            )
        )

        val result = repository.sync()

        assertTrue(result is CalendarSyncOutcome.Success)
        val success = result as CalendarSyncOutcome.Success
        assertEquals(2, success.result.created)
        assertEquals(1, success.result.updated)
    }

    @Test
    fun `sync should return reauth required on 403 INTEGRATION_REVOKED`() = runBlocking {
        val repository = CalendarRepositoryImpl(
            FakeCalendarApi(
                syncError = httpError(
                    code = 403,
                    body = """{"status":403,"code":"INTEGRATION_REVOKED","message":"revoked"}"""
                )
            )
        )

        val result = repository.sync()

        assertTrue(result is CalendarSyncOutcome.ReauthRequired)
    }

    @Test
    fun `sync should return recoverable failure on 403 GOOGLE_API_FORBIDDEN`() = runBlocking {
        val repository = CalendarRepositoryImpl(
            FakeCalendarApi(
                syncError = httpError(
                    code = 403,
                    body = """{"status":403,"code":"GOOGLE_API_FORBIDDEN","message":"missing scope"}"""
                )
            )
        )

        val result = repository.sync()

        assertTrue(result is CalendarSyncOutcome.RecoverableFailure)
        val failure = result as CalendarSyncOutcome.RecoverableFailure
        assertEquals(403, failure.httpStatus)
        assertEquals("GOOGLE_API_FORBIDDEN", failure.backendCode)
    }

    @Test
    fun `sync should return recoverable failure when network throws io exception`() = runBlocking {
        val repository = CalendarRepositoryImpl(
            FakeCalendarApi(syncError = IOException("network down"))
        )

        val result = repository.sync()

        assertTrue(result is CalendarSyncOutcome.RecoverableFailure)
        val failure = result as CalendarSyncOutcome.RecoverableFailure
        assertTrue(failure.backendMessage?.contains("network down") == true)
    }

    @Test
    fun `integrationStatus should map API status payload`() = runBlocking {
        val repository = CalendarRepositoryImpl(
            FakeCalendarApi(
                statusResult = IntegrationStatusDto(
                    status = "REAUTH_REQUIRED",
                    lastSyncAt = null,
                    errorCategory = "REVOKED",
                    errorMessage = "token revoked"
                )
            )
        )

        val result = repository.integrationStatus()

        assertEquals("REAUTH_REQUIRED", result.status)
        assertTrue(result.isReauthRequired())
    }

    @Test
    fun `sync should include startDate query when provided`() = runBlocking {
        val fakeApi = FakeCalendarApi()
        val repository = CalendarRepositoryImpl(fakeApi)

        repository.sync(startDate = LocalDate.of(2026, 4, 7))

        assertEquals("2026-04-07", fakeApi.lastSyncStartDate)
    }

    @Test
    fun `sync should call api without startDate when filter is disabled`() = runBlocking {
        val fakeApi = FakeCalendarApi()
        val repository = CalendarRepositoryImpl(fakeApi)

        repository.sync(startDate = null)

        assertNull(fakeApi.lastSyncStartDate)
    }
}

private class FakeCalendarApi(
    private val syncResult: SyncResponseDto = SyncResponseDto(created = 0, updated = 0, deleted = 0),
    private val syncError: Throwable? = null,
    private val statusResult: IntegrationStatusDto = IntegrationStatusDto(
        status = "SYNCED",
        lastSyncAt = null,
        errorCategory = null,
        errorMessage = null
    )
) : CalendarApi {
    var lastSyncStartDate: String? = null

    override suspend fun sync(startDate: String?): SyncResponseDto {
        lastSyncStartDate = startDate
        syncError?.let { throw it }
        return syncResult
    }

    override suspend fun listEvents(
        eventStart: String?,
        eventEnd: String?,
        page: Int,
        size: Int
    ): SpringPageDto<CalendarEventDto> {
        return SpringPageDto(
            content = listOf(
                CalendarEventDto(
                    id = 1L,
                    googleEventId = "g1",
                    title = "Evento",
                    eventStart = "2026-01-01T10:00:00Z",
                    eventEnd = "2026-01-01T11:00:00Z",
                    identified = true,
                    serviceDescription = "Servico",
                    serviceValue = BigDecimal("10.00")
                )
            ),
            totalPages = 1,
            number = page,
            size = size,
            totalElements = 1
        )
    }

    override suspend fun status(): IntegrationStatusDto = statusResult
}

private fun httpError(code: Int, body: String): HttpException {
    val response = Response.error<SyncResponseDto>(
        code,
        body.toResponseBody("application/json".toMediaType())
    )
    return HttpException(response)
}
