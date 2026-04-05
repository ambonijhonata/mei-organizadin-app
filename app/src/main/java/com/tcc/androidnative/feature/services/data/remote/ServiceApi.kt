package com.tcc.androidnative.feature.services.data.remote

import com.tcc.androidnative.feature.services.data.remote.dto.BulkDeleteServiceResponseDto
import com.tcc.androidnative.feature.services.data.remote.dto.PaginatedServicesDto
import com.tcc.androidnative.feature.services.data.remote.dto.ServiceDto
import com.tcc.androidnative.feature.services.data.remote.dto.ServiceUpsertRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ServiceApi {
    @GET("/api/services")
    suspend fun listServices(
        @Query("description") description: String?,
        @Query("sortBy") sortBy: String = "id",
        @Query("direction") direction: String = "asc",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 25
    ): PaginatedServicesDto

    @POST("/api/services")
    suspend fun createService(@Body request: ServiceUpsertRequestDto): ServiceDto

    @GET("/api/services/{id}")
    suspend fun getService(@Path("id") id: Long): ServiceDto

    @PUT("/api/services/{id}")
    suspend fun updateService(@Path("id") id: Long, @Body request: ServiceUpsertRequestDto): ServiceDto

    @DELETE("/api/services/{id}")
    suspend fun deleteService(@Path("id") id: Long)

    @POST("/api/services/delete")
    suspend fun bulkDelete(@Body ids: List<Long>): BulkDeleteServiceResponseDto
}

