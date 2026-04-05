package com.tcc.androidnative.core.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class SecureSessionManager @Inject constructor(
    @ApplicationContext context: Context
) : SessionManager {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val sessionFlow = MutableStateFlow(readSession())

    override val sessionState: StateFlow<UserSession?> = sessionFlow

    override fun saveSession(session: UserSession) {
        prefs.edit()
            .putLong(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_NAME, session.name)
            .putString(KEY_ID_TOKEN, session.idToken)
            .apply()
        sessionFlow.value = session
    }

    override fun clearSession() {
        prefs.edit().clear().apply()
        sessionFlow.value = null
    }

    override fun currentSession(): UserSession? = sessionFlow.value

    private fun readSession(): UserSession? {
        val idToken = prefs.getString(KEY_ID_TOKEN, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null).orEmpty()
        val name = prefs.getString(KEY_NAME, null).orEmpty()
        val userId = prefs.getLong(KEY_USER_ID, -1L)
        if (userId <= 0L) return null
        return UserSession(
            userId = userId,
            email = email,
            name = name,
            idToken = idToken
        )
    }

    override fun getIdToken(): String? = sessionFlow.value?.idToken

    private companion object {
        const val FILE_NAME = "secure_session"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_NAME = "name"
        const val KEY_ID_TOKEN = "id_token"
    }
}
