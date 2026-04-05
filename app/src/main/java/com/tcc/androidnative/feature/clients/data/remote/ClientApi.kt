package com.tcc.androidnative.feature.clients.data.remote

import com.tcc.androidnative.feature.clients.data.remote.dto.BulkDeleteResponseDto
import com.tcc.androidnative.feature.clients.data.remote.dto.ClientDto
import com.tcc.androidnative.feature.clients.data.remote.dto.ClientUpsertRequestDto
import com.tcc.androidnative.feature.clients.data.remote.dto.PaginatedClientsDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ClientApi {
    @GET("/api/clients")
    suspend fun listClients(
        @Query("name") name: String?,
        @Query("sortBy") sortBy: String = "id",
        @Query("direction") direction: String = "asc",
        @Query("pageIndex") pageIndex: Int = 1,
        @Query("itemsPerPage") itemsPerPage: Int = 25
    ): PaginatedClientsDto

    @POST("/api/clients")
    suspend fun createClient(@Body request: ClientUpsertRequestDto): ClientDto

    @GET("/api/clients/{id}")
    suspend fun getClient(@Path("id") id: Long): ClientDto

    @PUT("/api/clients/{id}")
    suspend fun updateClient(
        @Path("id") id: Long,
        @Body request: ClientUpsertRequestDto
    ): ClientDto

    @DELETE("/api/clients/{id}")
    suspend fun deleteClient(@Path("id") id: Long)

    @POST("/api/clients/delete")
    suspend fun bulkDelete(@Body ids: List<Long>): BulkDeleteResponseDto
}

