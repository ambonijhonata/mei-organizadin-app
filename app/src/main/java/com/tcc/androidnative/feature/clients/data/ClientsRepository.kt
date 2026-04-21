package com.tcc.androidnative.feature.clients.data

import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.core.network.BackendApiException
import com.tcc.androidnative.core.network.BackendErrorParser
import com.tcc.androidnative.feature.clients.data.remote.ClientApi
import com.tcc.androidnative.feature.clients.data.remote.dto.ClientUpsertRequestDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

data class ClientModel(
    val id: Long,
    val name: String,
    val cpf: String?,
    val dateOfBirth: LocalDate?,
    val email: String?,
    val phone: String?
)

data class ClientsPage(
    val items: List<ClientModel>,
    val pageIndex: Int,
    val totalPages: Int,
    val totalItems: Long
)

enum class DeleteClientOutcome {
    DELETED,
    HAS_LINK,
    FAILED
}

interface ClientsRepository {
    suspend fun list(name: String?, pageIndex: Int, itemsPerPage: Int): ClientsPage
    suspend fun getById(id: Long): ClientModel
    suspend fun create(model: ClientModel): ClientModel
    suspend fun update(id: Long, model: ClientModel): ClientModel
    suspend fun delete(id: Long): DeleteClientOutcome
    suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int>
}

@Singleton
class ClientsRepositoryImpl @Inject constructor(
    private val api: ClientApi,
    private val backendErrorParser: BackendErrorParser
) : ClientsRepository {
    override suspend fun list(name: String?, pageIndex: Int, itemsPerPage: Int): ClientsPage {
        // Cliente usa pagina 1-based no backend; a UI segue o mesmo contrato.
        val response = api.listClients(name = name, pageIndex = pageIndex, itemsPerPage = itemsPerPage)
        return ClientsPage(
            items = response.items.map { dto ->
                ClientModel(
                    id = dto.id,
                    name = dto.name,
                    cpf = dto.cpf,
                    dateOfBirth = dto.dateOfBirth?.let(DateFormats::parseApiDate),
                    email = dto.email,
                    phone = dto.phone
                )
            },
            pageIndex = response.pageIndex,
            totalPages = response.totalPages,
            totalItems = response.totalItems
        )
    }

    override suspend fun getById(id: Long): ClientModel {
        val response = api.getClient(id)
        return ClientModel(
            id = response.id,
            name = response.name,
            cpf = response.cpf,
            dateOfBirth = response.dateOfBirth?.let(DateFormats::parseApiDate),
            email = response.email,
            phone = response.phone
        )
    }

    override suspend fun create(model: ClientModel): ClientModel {
        return try {
            val response = api.createClient(
                request = ClientUpsertRequestDto(
                    name = model.name,
                    cpf = model.cpf,
                    dateOfBirth = model.dateOfBirth?.let(DateFormats::toApiDate),
                    email = model.email,
                    phone = model.phone
                )
            )
            ClientModel(
                id = response.id,
                name = response.name,
                cpf = response.cpf,
                dateOfBirth = response.dateOfBirth?.let(DateFormats::parseApiDate),
                email = response.email,
                phone = response.phone
            )
        } catch (error: HttpException) {
            throw BackendApiException(backendErrorParser.parse(error), error)
        }
    }

    override suspend fun update(id: Long, model: ClientModel): ClientModel {
        return try {
            val response = api.updateClient(
                id = id,
                request = ClientUpsertRequestDto(
                    name = model.name,
                    cpf = model.cpf,
                    dateOfBirth = model.dateOfBirth?.let(DateFormats::toApiDate),
                    email = model.email,
                    phone = model.phone
                )
            )
            ClientModel(
                id = response.id,
                name = response.name,
                cpf = response.cpf,
                dateOfBirth = response.dateOfBirth?.let(DateFormats::parseApiDate),
                email = response.email,
                phone = response.phone
            )
        } catch (error: HttpException) {
            throw BackendApiException(backendErrorParser.parse(error), error)
        }
    }

    override suspend fun delete(id: Long): DeleteClientOutcome {
        return try {
            api.deleteClient(id)
            DeleteClientOutcome.DELETED
        } catch (error: Throwable) {
            if (error is HttpException && isHasLinkedAppointmentError(error)) {
                DeleteClientOutcome.HAS_LINK
            } else {
                DeleteClientOutcome.FAILED
            }
        }
    }

    override suspend fun bulkDelete(ids: List<Long>): Pair<Int, Int> {
        val response = api.bulkDelete(ids)
        return response.deleted to response.hasLink
    }

    private fun isHasLinkedAppointmentError(error: HttpException): Boolean {
        if (error.code() != 422) return false

        return backendErrorParser.parse(error).code == "BUSINESS_ERROR"
    }
}
