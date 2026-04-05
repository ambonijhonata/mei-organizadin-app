package com.tcc.androidnative.feature.reports.data

import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.reports.data.remote.ReportApi
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class SyncMetadataModel(
    val dataUpToDate: Boolean,
    val lastSyncAt: String?,
    val reauthRequired: Boolean
)

data class CashFlowServiceModel(
    val name: String,
    val total: BigDecimal
)

data class CashFlowEntryModel(
    val date: LocalDate,
    val total: BigDecimal,
    val services: List<CashFlowServiceModel>
)

data class CashFlowReportModel(
    val entries: List<CashFlowEntryModel>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val syncMetadata: SyncMetadataModel
)

data class RevenueReportModel(
    val totalRevenue: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val syncMetadata: SyncMetadataModel
)

interface ReportsRepository {
    suspend fun cashFlow(startDate: LocalDate, endDate: LocalDate): CashFlowReportModel
    suspend fun revenue(startDate: LocalDate, endDate: LocalDate): RevenueReportModel
}

@Singleton
class ReportsRepositoryImpl @Inject constructor(
    private val api: ReportApi
) : ReportsRepository {
    override suspend fun cashFlow(startDate: LocalDate, endDate: LocalDate): CashFlowReportModel {
        val response = api.cashFlow(
            startDate = DateFormats.toApiDate(startDate),
            endDate = DateFormats.toApiDate(endDate)
        )
        return CashFlowReportModel(
            entries = response.entries.map { entry ->
                CashFlowEntryModel(
                    date = DateFormats.parseApiDate(entry.date),
                    total = entry.total,
                    services = entry.services.map { service ->
                        CashFlowServiceModel(name = service.name, total = service.total)
                    }
                )
            },
            startDate = DateFormats.parseApiDate(response.startDate),
            endDate = DateFormats.parseApiDate(response.endDate),
            syncMetadata = SyncMetadataModel(
                dataUpToDate = response.syncMetadata.dataUpToDate,
                lastSyncAt = response.syncMetadata.lastSyncAt,
                reauthRequired = response.syncMetadata.reauthRequired
            )
        )
    }

    override suspend fun revenue(startDate: LocalDate, endDate: LocalDate): RevenueReportModel {
        val response = api.revenue(
            startDate = DateFormats.toApiDate(startDate),
            endDate = DateFormats.toApiDate(endDate)
        )
        return RevenueReportModel(
            totalRevenue = response.totalRevenue,
            startDate = DateFormats.parseApiDate(response.startDate),
            endDate = DateFormats.parseApiDate(response.endDate),
            syncMetadata = SyncMetadataModel(
                dataUpToDate = response.syncMetadata.dataUpToDate,
                lastSyncAt = response.syncMetadata.lastSyncAt,
                reauthRequired = response.syncMetadata.reauthRequired
            )
        )
    }
}

