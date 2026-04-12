package com.tcc.androidnative.feature.calendar.data.remote

import com.tcc.androidnative.feature.calendar.data.remote.dto.CalendarEventDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.IntegrationStatusDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.SpringPageDto
import com.tcc.androidnative.feature.calendar.data.remote.dto.SyncResponseDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CalendarApi {
    @POST("/api/calendar/sync")
    suspend fun sync(
        @Query("startDate") startDate: String? = null
    ): SyncResponseDto

    @GET("/api/calendar/events")
    suspend fun listEvents(
        @Query("eventStart") eventStart: String?,
        @Query("eventEnd") eventEnd: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): SpringPageDto<CalendarEventDto>

    @GET("/api/calendar/status")
    suspend fun status(): IntegrationStatusDto
}
