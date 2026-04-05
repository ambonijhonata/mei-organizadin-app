package com.tcc.androidnative.feature.calendar.data.remote.dto

import java.math.BigDecimal

data class SyncResponseDto(
    val created: Int,
    val updated: Int,
    val deleted: Int
)

data class CalendarEventDto(
    val id: Long,
    val googleEventId: String,
    val title: String,
    val eventStart: String,
    val eventEnd: String?,
    val identified: Boolean,
    val serviceDescription: String?,
    val serviceValue: BigDecimal?
)

data class IntegrationStatusDto(
    val status: String,
    val lastSyncAt: String?,
    val errorCategory: String?,
    val errorMessage: String?
)

data class SpringPageDto<T>(
    val content: List<T>,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val totalElements: Long
)

