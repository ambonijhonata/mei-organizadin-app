package com.tcc.androidnative.feature.reports.data.remote.dto

import java.math.BigDecimal

data class SyncMetadataDto(
    val dataUpToDate: Boolean,
    val lastSyncAt: String?,
    val reauthRequired: Boolean
)

data class ServiceEntryDto(
    val name: String,
    val total: BigDecimal
)

data class CashFlowEntryDto(
    val date: String,
    val total: BigDecimal,
    val services: List<ServiceEntryDto>
)

data class CashFlowReportDto(
    val entries: List<CashFlowEntryDto>,
    val startDate: String,
    val endDate: String,
    val syncMetadata: SyncMetadataDto
)

data class RevenueReportDto(
    val totalRevenue: BigDecimal,
    val startDate: String,
    val endDate: String,
    val syncMetadata: SyncMetadataDto
)

