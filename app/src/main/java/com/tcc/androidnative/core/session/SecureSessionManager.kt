package com.tcc.androidnative.core.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal const val SECURE_SESSION_FILE_NAME = "secure_session"

@Singleton
class SecureSessionManager @Inject constructor(
    @ApplicationContext context: Context
): SessionManager by SecureSessionManagerImpl(
    secureStore = AndroidEncryptedSessionStore(context),
    logger = AndroidSessionFallbackLogger()
)

internal class SecureSessionManagerImpl(
    private val secureStore: SecureSessionStore,
    private val logger: SessionFallbackLogger
) : SessionManager {
    private val prefs = initializeSecurePrefsWithFallback()
    private var fallbackRecoveryApplied = false

    private val sessionFlow = MutableStateFlow(readSession())

    override val sessionState: StateFlow<UserSession?> = sessionFlow

    override fun saveSession(session: UserSession) {
        prefs.edit()
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NAME, session.name)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_ACCESS_TOKEN_EXPIRES_AT, session.accessTokenExpiresAtEpochSeconds)
            .putLong(KEY_REFRESH_TOKEN_EXPIRES_AT, session.refreshTokenExpiresAtEpochSeconds)
            .apply()
        sessionFlow.value = session
    }

    override fun clearSession() {
        runCatching { prefs.edit().clear().apply() }
            .onFailure { logger.logEvent("session_secure_store_clear_failed", it) }
        sessionFlow.value = null
    }

    override fun currentSession(): UserSession? = sessionFlow.value

    private fun readSession(): UserSession? {
        val state = runCatching {
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
            val email = prefs.getString(KEY_EMAIL, null).orEmpty()
            val name = prefs.getString(KEY_NAME, null).orEmpty()
            val userId = prefs.getLong(KEY_USER_ID, -1L)
            val accessTokenExpiresAt = prefs.getLong(KEY_ACCESS_TOKEN_EXPIRES_AT, -1L)
            val refreshTokenExpiresAt = prefs.getLong(KEY_REFRESH_TOKEN_EXPIRES_AT, -1L)
            if (userId <= 0L) return null
            if (accessTokenExpiresAt <= 0L || refreshTokenExpiresAt <= 0L) return null
            UserSession(
                userId = userId,
                email = email,
                name = name,
                accessToken = accessToken,
                refreshToken = refreshToken,
                accessTokenExpiresAtEpochSeconds = accessTokenExpiresAt,
                refreshTokenExpiresAtEpochSeconds = refreshTokenExpiresAt
            )
        }
        if (state.isSuccess) return state.getOrNull()

        val failure = state.exceptionOrNull()
        if (!isRecoverableSecureStorageFailure(failure)) {
            throw failure ?: IllegalStateException("Unknown secure storage read failure")
        }
        if (fallbackRecoveryApplied) {
            logger.logEvent("session_secure_store_recovery_failed_second_attempt", failure)
            throw IllegalStateException("Secure session storage recovery failed", failure)
        }

        fallbackRecoveryApplied = true
        logger.logEvent("session_secure_store_recovery_start", failure)
        return recoverFromSecureStorageFailure()
    }

    private fun recoverFromSecureStorageFailure(): UserSession? {
        runCatching { secureStore.clearStorage() }
            .onFailure { logger.logEvent("session_secure_store_recovery_clear_failed", it) }
        val reinitialized = runCatching { secureStore.create() }.getOrElse { error ->
            logger.logEvent("session_secure_store_recovery_reinit_failed", error)
            throw IllegalStateException("Unable to recover secure session storage", error)
        }
        runCatching { reinitialized.edit().clear().apply() }
            .onFailure { logger.logEvent("session_secure_store_recovery_post_clear_failed", it) }
        logger.logEvent("session_secure_store_recovery_success", null)
        return null
    }

    private fun initializeSecurePrefsWithFallback(): SessionSecurePrefs {
        return runCatching { secureStore.create() }
            .getOrElse { error ->
                if (!isRecoverableSecureStorageFailure(error)) throw error
                logger.logEvent("session_secure_store_init_recoverable_failure", error)
                runCatching { secureStore.clearStorage() }
                    .onFailure { logger.logEvent("session_secure_store_init_clear_failed", it) }
                runCatching { secureStore.create() }
                    .getOrElse { reinitError ->
                        logger.logEvent("session_secure_store_init_recovery_failed", reinitError)
                        throw IllegalStateException("Unable to initialize secure session storage", reinitError)
                    }
            }
    }

    private fun isRecoverableSecureStorageFailure(error: Throwable?): Boolean {
        if (error == null) return false
        if (error is GeneralSecurityException) return true
        val simpleName = error.javaClass.simpleName
        if (simpleName.contains("AEADBadTagException")) return true
        if (simpleName.contains("KeyStoreException")) return true
        val message = error.message.orEmpty()
        if (message.contains("AEADBadTagException", ignoreCase = true)) return true
        if (message.contains("keystore", ignoreCase = true)) return true
        return isRecoverableSecureStorageFailure(error.cause)
    }

    override fun getIdToken(): String? = sessionFlow.value?.accessToken
    override fun getAccessToken(): String? = sessionFlow.value?.accessToken
    override fun getRefreshToken(): String? = sessionFlow.value?.refreshToken
    override fun getAccessTokenExpiresAtEpochSeconds(): Long? = sessionFlow.value?.accessTokenExpiresAtEpochSeconds
    override fun getRefreshTokenExpiresAtEpochSeconds(): Long? = sessionFlow.value?.refreshTokenExpiresAtEpochSeconds

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ACCESS_TOKEN_EXPIRES_AT = "access_token_expires_at"
        const val KEY_REFRESH_TOKEN_EXPIRES_AT = "refresh_token_expires_at"
    }
}

internal interface SessionSecurePrefs {
    fun getString(key: String, defaultValue: String?): String?
    fun getLong(key: String, defaultValue: Long): Long
    fun edit(): SessionSecurePrefsEditor
}

internal interface SessionSecurePrefsEditor {
    fun putLong(key: String, value: Long): SessionSecurePrefsEditor
    fun putString(key: String, value: String?): SessionSecurePrefsEditor
    fun clear(): SessionSecurePrefsEditor
    fun apply()
}

internal interface SecureSessionStore {
    fun create(): SessionSecurePrefs
    fun clearStorage()
}

internal class AndroidEncryptedSessionStore(
    private val context: Context
) : SecureSessionStore {
    override fun create(): SessionSecurePrefs {
        val prefs = EncryptedSharedPreferences.create(
            context,
            SECURE_SESSION_FILE_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return SharedPreferencesAdapter(prefs)
    }

    override fun clearStorage() {
        context.deleteSharedPreferences(SECURE_SESSION_FILE_NAME)
    }
}

internal class SharedPreferencesAdapter(
    private val delegate: SharedPreferences
) : SessionSecurePrefs {
    override fun getString(key: String, defaultValue: String?): String? = delegate.getString(key, defaultValue)
    override fun getLong(key: String, defaultValue: Long): Long = delegate.getLong(key, defaultValue)
    override fun edit(): SessionSecurePrefsEditor = SharedPreferencesEditorAdapter(delegate.edit())
}

internal class SharedPreferencesEditorAdapter(
    private val delegate: SharedPreferences.Editor
) : SessionSecurePrefsEditor {
    override fun putLong(key: String, value: Long): SessionSecurePrefsEditor {
        delegate.putLong(key, value)
        return this
    }

    override fun putString(key: String, value: String?): SessionSecurePrefsEditor {
        delegate.putString(key, value)
        return this
    }

    override fun clear(): SessionSecurePrefsEditor {
        delegate.clear()
        return this
    }

    override fun apply() {
        delegate.apply()
    }
}

internal interface SessionFallbackLogger {
    fun logEvent(event: String, error: Throwable?)
}

internal class AndroidSessionFallbackLogger : SessionFallbackLogger {
    override fun logEvent(event: String, error: Throwable?) {
        runCatching {
            if (error != null) {
                Log.w(
                    "SecureSessionManager",
                    "event=$event error_type=${error.javaClass.simpleName}",
                    error
                )
            } else {
                Log.i("SecureSessionManager", "event=$event")
            }
        }
    }
}
