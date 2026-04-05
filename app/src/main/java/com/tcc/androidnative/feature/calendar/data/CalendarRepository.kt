package com.tcc.androidnative.feature.calendar.data

import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.calendar.data.remote.CalendarApi
import java.math.BigDecimal
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

data class CalendarSyncResult(val created: Int, val updated: Int, val deleted: Int)

sealed interface CalendarSyncOutcome {
    data class Success(val result: CalendarSyncResult) : CalendarSyncOutcome

    data class RecoverableFailure(
        val httpStatus: Int? = null,
        val backendCode: String? = null,
        val backendMessage: String? = null
    ) : CalendarSyncOutcome

    data object ReauthRequired : CalendarSyncOutcome
}

data class CalendarIntegrationStatus(
    val status: String,
    val lastSyncAt: String?,
    val errorCategory: String?,
    val errorMessage: String?
) {
    fun isReauthRequired(): Boolean {
        return status.equals(REAUTH_REQUIRED, ignoreCase = true) ||
            errorCategory.equals(REVOKED_CATEGORY, ignoreCase = true)
    }

    private companion object {
        const val REAUTH_REQUIRED = "REAUTH_REQUIRED"
        const val REVOKED_CATEGORY = "REVOKED"
    }
}

data class CalendarEventModel(
    val id: Long,
    val title: String,
    val eventStart: Instant,
    val eventEnd: Instant?,
    val identified: Boolean,
    val serviceDescription: String?,
    val serviceValue: BigDecimal?
)

interface CalendarRepository {
    suspend fun sync(): CalendarSyncOutcome
    suspend fun integrationStatus(): CalendarIntegrationStatus
    suspend fun eventsByDay(date: LocalDate): List<CalendarEventModel>
}

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val api: CalendarApi
) : CalendarRepository {
    override suspend fun sync(): CalendarSyncOutcome {
        return try {
            val response = api.sync()
            CalendarSyncOutcome.Success(
                CalendarSyncResult(
                    created = response.created,
                    updated = response.updated,
                    deleted = response.deleted
                )
            )
        } catch (error: HttpException) {
            val errorBody = error.response()?.errorBody()?.string().orEmpty()
            val backendCode = extractJsonField(errorBody, "code")
            val backendMessage = extractJsonField(errorBody, "message")
            if (error.code() == HTTP_FORBIDDEN && backendCode == INTEGRATION_REVOKED) {
                CalendarSyncOutcome.ReauthRequired
            } else {
                CalendarSyncOutcome.RecoverableFailure(
                    httpStatus = error.code(),
                    backendCode = backendCode,
                    backendMessage = backendMessage
                )
            }
        } catch (error: IOException) {
            CalendarSyncOutcome.RecoverableFailure(
                backendMessage = error.message
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            CalendarSyncOutcome.RecoverableFailure(
                backendMessage = error.message
            )
        }
    }

    override suspend fun integrationStatus(): CalendarIntegrationStatus {
        val response = api.status()
        return CalendarIntegrationStatus(
            status = response.status,
            lastSyncAt = response.lastSyncAt,
            errorCategory = response.errorCategory,
            errorMessage = response.errorMessage
        )
    }

    override suspend fun eventsByDay(date: LocalDate): List<CalendarEventModel> {
        // Fluxo da UI usa "date"; API real exige eventStart/eventEnd.
        val dateValue = DateFormats.toApiDate(date)
        val page = api.listEvents(eventStart = dateValue, eventEnd = dateValue)
        return page.content.map {
            CalendarEventModel(
                id = it.id,
                title = it.title,
                eventStart = DateFormats.parseInstant(it.eventStart),
                eventEnd = it.eventEnd?.let(DateFormats::parseInstant),
                identified = it.identified,
                serviceDescription = it.serviceDescription,
                serviceValue = it.serviceValue
            )
        }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val match = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"").find(json) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val HTTP_FORBIDDEN = 403
        const val INTEGRATION_REVOKED = "INTEGRATION_REVOKED"
    }
}
