package com.tcc.androidnative.feature.services.data.remote.dto

import java.math.BigDecimal

data class ServiceUpsertRequestDto(
    val description: String,
    val value: BigDecimal
)

data class ServiceDto(
    val id: Long,
    val description: String,
    val value: BigDecimal,
    val createdAt: String,
    val updatedAt: String
)

data class PaginatedServicesDto(
    val items: List<ServiceDto>,
    val totalItems: Int,
    val itemsPerPage: Int,
    val totalPages: Int,
    val pageIndex: Int
)

data class BulkDeleteServiceResponseDto(
    val deleted: Int,
    val hasLink: Int
)

