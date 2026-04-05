package com.tcc.androidnative.feature.services.data

import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.feature.services.data.remote.ServiceApi
import com.tcc.androidnative.feature.services.data.remote.dto.ServiceUpsertRequestDto
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

data class ServiceModel(
    val id: Long,
    val description: String,
    val value: BigDecimal
)

data class ServicesPage(
    val items: List<ServiceModel>,
    val pageIndex: Int,
    val totalPages: Int,
    val totalItems: Int
)

interface ServicesRepository {
    suspend fun list(description: String?, uiPageIndex: Int, pageSize: Int): ServicesPage
    suspend fun getById(id: Long): ServiceModel
    suspend fun create(description: String, valueInput: String): ServiceModel
    suspend fun update(id: Long, description: String, valueInput: String): ServiceModel
    suspend fun delete(id: Long)
    suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int>
}

@Singleton
class ServicesRepositoryImpl @Inject constructor(
    private val api: ServiceApi
) : ServicesRepository {
    override suspend fun list(description: String?, uiPageIndex: Int, pageSize: Int): ServicesPage {
        // UI trabalha em pagina 1-based; API de services usa page 0-based.
        val apiPage = (uiPageIndex - 1).coerceAtLeast(0)
        val response = api.listServices(
            description = description,
            page = apiPage,
            size = pageSize
        )
        return ServicesPage(
            items = response.items.map { dto ->
                ServiceModel(
                    id = dto.id,
                    description = dto.description,
                    value = dto.value
                )
            },
            pageIndex = response.pageIndex + 1,
            totalPages = response.totalPages,
            totalItems = response.totalItems
        )
    }

    override suspend fun getById(id: Long): ServiceModel {
        val response = api.getService(id)
        return ServiceModel(
            id = response.id,
            description = response.description,
            value = response.value
        )
    }

    override suspend fun create(description: String, valueInput: String): ServiceModel {
        val response = api.createService(
            ServiceUpsertRequestDto(
                description = description,
                value = CurrencyFormats.parseUiValue(valueInput)
            )
        )
        return ServiceModel(response.id, response.description, response.value)
    }

    override suspend fun update(id: Long, description: String, valueInput: String): ServiceModel {
        val response = api.updateService(
            id = id,
            request = ServiceUpsertRequestDto(
                description = description,
                value = CurrencyFormats.parseUiValue(valueInput)
            )
        )
        return ServiceModel(response.id, response.description, response.value)
    }

    override suspend fun delete(id: Long) {
        api.deleteService(id)
    }

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> {
        val response = api.bulkDelete(ids)
        return response.deleted to response.hasLink
    }
}
