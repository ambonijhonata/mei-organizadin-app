package com.tcc.androidnative.feature.reports.data.remote

import com.tcc.androidnative.feature.reports.data.remote.dto.CashFlowReportDto
import com.tcc.androidnative.feature.reports.data.remote.dto.RevenueReportDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ReportApi {
    // Apenas /api/reports/* na V1 (sem aliases /api/report/*).
    @GET("/api/reports/cashflow")
    suspend fun cashFlow(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): CashFlowReportDto

    @GET("/api/reports/revenue")
    suspend fun revenue(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): RevenueReportDto
}

