package com.tcc.androidnative.core.session

import android.util.Log
import com.tcc.androidnative.feature.auth.data.AuthRepository
import dagger.Lazy
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

@Singleton
class SessionRefreshCoordinator @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepositoryProvider: Lazy<AuthRepository>
) {
    private val stateMutex = Mutex()
    private var inFlightRefresh: CompletableDeferred<RefreshExecutionResult>? = null
    private var lastSuccessfulRefreshAtMillis: Long = 0L
    private var lastRecoverableFailureAtMillis: Long = 0L

    suspend fun refreshIfNeeded(
        force: Boolean = false,
        trigger: RefreshTrigger = RefreshTrigger.UNKNOWN
    ): RefreshOutcome {
        val session = sessionManager.currentSession() ?: return RefreshOutcome.NoSession
        val nowEpochMillis = System.currentTimeMillis()
        val nowEpochSeconds = nowEpochMillis / 1000L
        val isNearExpiry = session.accessTokenExpiresAtEpochSeconds - nowEpochSeconds <= REFRESH_THRESHOLD_SECONDS
        if (!force && !isNearExpiry) {
            logInfo("session_refresh_skipped_not_needed trigger=${trigger.value}")
            return RefreshOutcome.NotNeeded
        }

        val inFlightState = stateMutex.withLock {
            val existing = inFlightRefresh
            if (existing != null) {
                logInfo("session_refresh_join_inflight trigger=${trigger.value}")
                return@withLock InFlightState(
                    deferred = existing,
                    ownsExecution = false,
                    skippedRecent = false,
                    skippedRecentRecoverableFailure = false
                )
            }
            if (isWithinSuccessCooldown(nowEpochMillis)) {
                logInfo("session_refresh_dedup_recent_success trigger=${trigger.value}")
                return@withLock InFlightState(
                    deferred = null,
                    ownsExecution = false,
                    skippedRecent = true,
                    skippedRecentRecoverableFailure = false
                )
            }
            if (isWithinRecoverableFailureCooldown(nowEpochMillis)) {
                logInfo("session_refresh_dedup_recent_recoverable_failure trigger=${trigger.value}")
                return@withLock InFlightState(
                    deferred = null,
                    ownsExecution = false,
                    skippedRecent = false,
                    skippedRecentRecoverableFailure = true
                )
            }

            val created = CompletableDeferred<RefreshExecutionResult>()
            inFlightRefresh = created
            logInfo("session_refresh_start trigger=${trigger.value} force=$force nearExpiry=$isNearExpiry")
            InFlightState(
                deferred = created,
                ownsExecution = true,
                skippedRecent = false,
                skippedRecentRecoverableFailure = false
            )
        }

        if (inFlightState.skippedRecent) {
            return RefreshOutcome.Success(RefreshSuccessKind.SKIPPED_RECENT_SUCCESS)
        }
        if (inFlightState.skippedRecentRecoverableFailure) {
            return RefreshOutcome.RecoverableFailure("recent_recoverable_failure_cooldown")
        }

        val sharedDeferred = inFlightState.deferred ?: return RefreshOutcome.RecoverableFailure(
            "missing_refresh_state"
        )
        if (inFlightState.ownsExecution) {
            val refreshResult = executeRefresh(trigger = trigger, refreshToken = session.refreshToken)
            sharedDeferred.complete(refreshResult)
            stateMutex.withLock {
                if (inFlightRefresh === sharedDeferred) {
                    inFlightRefresh = null
                }
                when (refreshResult) {
                    is RefreshExecutionResult.Success -> {
                        lastSuccessfulRefreshAtMillis = System.currentTimeMillis()
                    }
                    is RefreshExecutionResult.RecoverableFailure -> {
                        lastRecoverableFailureAtMillis = System.currentTimeMillis()
                    }
                    is RefreshExecutionResult.TerminalFailure -> Unit
                }
            }
        }

        val result = sharedDeferred.await()
        return when (result) {
            is RefreshExecutionResult.Success -> {
                if (inFlightState.ownsExecution) {
                    RefreshOutcome.Success(RefreshSuccessKind.REFRESHED)
                } else {
                    RefreshOutcome.Success(RefreshSuccessKind.REUSED_IN_FLIGHT)
                }
            }
            is RefreshExecutionResult.TerminalFailure -> {
                RefreshOutcome.TerminalFailure(result.code, result.message)
            }
            is RefreshExecutionResult.RecoverableFailure -> {
                RefreshOutcome.RecoverableFailure(result.message)
            }
        }
    }

    private suspend fun executeRefresh(
        trigger: RefreshTrigger,
        refreshToken: String
    ): RefreshExecutionResult {
        var attempt = 0
        while (attempt < MAX_RECOVERABLE_ATTEMPTS) {
            val outcome = runCatching {
                authRepositoryProvider.get().refresh(refreshToken)
            }.fold(
                onSuccess = {
                    logInfo("session_refresh_success trigger=${trigger.value} attempt=${attempt + 1}")
                    RefreshExecutionResult.Success
                },
                onFailure = { error ->
                    val terminalCode = extractTerminalRefreshCode(error)
                    if (terminalCode != null) {
                        logWarn(
                            "session_refresh_terminal_failure trigger=${trigger.value} " +
                                "code=${terminalCode.value} message=${error.message}"
                        )
                        RefreshExecutionResult.TerminalFailure(
                            code = terminalCode,
                            message = error.message
                        )
                    } else if (isRetryableRefreshFailure(error)) {
                        val retryableCode = extractRetryableRefreshCode(error)
                        logWarn(
                            "session_refresh_retryable_failure trigger=${trigger.value} " +
                                "attempt=${attempt + 1} code=$retryableCode message=${error.message}"
                        )
                        RefreshExecutionResult.RecoverableFailure(error.message, retryable = true)
                    } else {
                        logWarn("session_refresh_recoverable_failure trigger=${trigger.value} message=${error.message}")
                        RefreshExecutionResult.RecoverableFailure(error.message, retryable = false)
                    }
                }
            )

            when (outcome) {
                is RefreshExecutionResult.Success -> return outcome
                is RefreshExecutionResult.TerminalFailure -> return outcome
                is RefreshExecutionResult.RecoverableFailure -> {
                    val canRetry = outcome.retryable && attempt < MAX_RECOVERABLE_ATTEMPTS - 1
                    if (!canRetry) {
                        return outcome
                    }
                    delay(RECOVERABLE_RETRY_BACKOFF_MS[attempt])
                    attempt += 1
                }
            }
        }
        return RefreshExecutionResult.RecoverableFailure("refresh_retry_exhausted", retryable = true)
    }

    private fun isWithinSuccessCooldown(nowEpochMillis: Long): Boolean {
        if (lastSuccessfulRefreshAtMillis <= 0L) return false
        return nowEpochMillis - lastSuccessfulRefreshAtMillis <= SUCCESS_DEDUP_WINDOW_MILLIS
    }

    private fun isWithinRecoverableFailureCooldown(nowEpochMillis: Long): Boolean {
        if (lastRecoverableFailureAtMillis <= 0L) return false
        return nowEpochMillis - lastRecoverableFailureAtMillis <= RECOVERABLE_FAILURE_DEDUP_WINDOW_MILLIS
    }

    private fun extractTerminalRefreshCode(error: Throwable): RefreshTerminalCode? {
        if (error !is HttpException) return null
        val code = error.code()
        if (code != 401) return null
        val body = runCatching { error.response()?.errorBody()?.string().orEmpty() }
            .getOrDefault("")
        return when {
            body.contains("REFRESH_TOKEN_INVALID") -> RefreshTerminalCode.REFRESH_TOKEN_INVALID
            body.contains("REFRESH_TOKEN_REVOKED") -> RefreshTerminalCode.REFRESH_TOKEN_REVOKED
            body.contains("REFRESH_TOKEN_REUSED") -> RefreshTerminalCode.REFRESH_TOKEN_REUSED
            body.contains("REFRESH_TOKEN_EXPIRED") -> RefreshTerminalCode.REFRESH_TOKEN_EXPIRED
            else -> null
        }
    }

    private fun extractRetryableRefreshCode(error: Throwable): String? {
        if (error !is HttpException) return null
        return runCatching { error.response()?.errorBody()?.string().orEmpty() }
            .getOrDefault("")
            .takeIf { it.contains("REFRESH_RETRYABLE") }
    }

    private fun isRetryableRefreshFailure(error: Throwable): Boolean {
        return when (error) {
            is IOException -> true
            is HttpException -> {
                val code = error.code()
                if (code == 408 || code == 429 || code >= 500) return true
                runCatching { error.response()?.errorBody()?.string().orEmpty() }
                    .getOrDefault("")
                    .contains("REFRESH_RETRYABLE")
            }
            else -> false
        }
    }

    private fun logWarn(message: String) {
        runCatching { Log.w(TAG, message) }
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    companion object {
        const val REFRESH_THRESHOLD_SECONDS = 5 * 60L
        const val SUCCESS_DEDUP_WINDOW_MILLIS = 10_000L
        const val RECOVERABLE_FAILURE_DEDUP_WINDOW_MILLIS = 8_000L
        const val MAX_RECOVERABLE_ATTEMPTS = 3
        val RECOVERABLE_RETRY_BACKOFF_MS = listOf(1_000L, 2_000L)
        private const val TAG = "SessionRefreshCoordinator"
    }
}

sealed class RefreshOutcome {
    data object NotNeeded : RefreshOutcome()
    data object NoSession : RefreshOutcome()
    data class Success(val kind: RefreshSuccessKind) : RefreshOutcome()
    data class RecoverableFailure(val message: String? = null) : RefreshOutcome()
    data class TerminalFailure(
        val code: RefreshTerminalCode,
        val message: String? = null
    ) : RefreshOutcome()
}

enum class RefreshSuccessKind {
    REFRESHED,
    REUSED_IN_FLIGHT,
    SKIPPED_RECENT_SUCCESS
}

enum class RefreshTerminalCode(val value: String) {
    REFRESH_TOKEN_INVALID("REFRESH_TOKEN_INVALID"),
    REFRESH_TOKEN_REVOKED("REFRESH_TOKEN_REVOKED"),
    REFRESH_TOKEN_REUSED("REFRESH_TOKEN_REUSED"),
    REFRESH_TOKEN_EXPIRED("REFRESH_TOKEN_EXPIRED")
}

enum class RefreshTrigger(val value: String) {
    PROACTIVE("proactive"),
    REACTIVE_401("reactive_401"),
    UNKNOWN("unknown")
}

private data class InFlightState(
    val deferred: CompletableDeferred<RefreshExecutionResult>?,
    val ownsExecution: Boolean,
    val skippedRecent: Boolean,
    val skippedRecentRecoverableFailure: Boolean
)

private sealed class RefreshExecutionResult {
    data object Success : RefreshExecutionResult()
    data class RecoverableFailure(
        val message: String?,
        val retryable: Boolean
    ) : RefreshExecutionResult()
    data class TerminalFailure(
        val code: RefreshTerminalCode,
        val message: String?
    ) : RefreshExecutionResult()
}
