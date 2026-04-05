package com.tcc.androidnative.feature.clients.data.remote.dto

data class ClientUpsertRequestDto(
    val name: String,
    val cpf: String?,
    val dateOfBirth: String?,
    val email: String?,
    val phone: String?
)

data class ClientDto(
    val id: Long,
    val name: String,
    val cpf: String?,
    val dateOfBirth: String?,
    val email: String?,
    val phone: String?,
    val createdAt: String
)

data class PaginatedClientsDto(
    val items: List<ClientDto>,
    val totalItems: Long,
    val totalPages: Int,
    val itemsPerPage: Int,
    val pageIndex: Int
)

data class BulkDeleteResponseDto(
    val deleted: Int,
    val hasLink: Int
)

