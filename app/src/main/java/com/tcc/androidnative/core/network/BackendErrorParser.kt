package com.tcc.androidnative.core.network

import com.squareup.moshi.Moshi
import javax.inject.Inject
import retrofit2.HttpException

data class BackendErrorDetails(
    val httpStatus: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val fieldErrors: Map<String, String> = emptyMap()
)

class BackendApiException(
    val details: BackendErrorDetails,
    cause: Throwable? = null
) : RuntimeException(details.message ?: "Backend request failed", cause)

class BackendErrorParser @Inject constructor(
    moshi: Moshi
) {
    private val adapter = moshi.adapter(BackendErrorResponse::class.java)

    fun parse(error: HttpException): BackendErrorDetails {
        val rawBody = runCatching { error.response()?.errorBody()?.string().orEmpty() }
            .getOrDefault("")
        val parsed = runCatching { adapter.fromJson(rawBody) }.getOrNull()
        return BackendErrorDetails(
            httpStatus = error.code(),
            code = parsed?.code,
            message = parsed?.message?.takeIf { it.isNotBlank() } ?: error.message(),
            fieldErrors = parsed?.errors.orEmpty()
                .mapNotNull { fieldError ->
                    val field = fieldError.field?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val message = fieldError.message?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    field to message
                }
                .toMap()
        )
    }
}

private data class BackendErrorResponse(
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val errors: List<BackendFieldErrorResponse> = emptyList()
)

private data class BackendFieldErrorResponse(
    val field: String? = null,
    val message: String? = null
)
