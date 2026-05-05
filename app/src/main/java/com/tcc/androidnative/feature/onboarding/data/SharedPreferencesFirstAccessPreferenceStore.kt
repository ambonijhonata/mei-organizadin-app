package com.tcc.androidnative.feature.onboarding.data

import android.content.Context
import android.content.SharedPreferences
import com.tcc.androidnative.core.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesFirstAccessPreferenceStore internal constructor(
    private val prefs: SharedPreferences,
    private val currentUserIdProvider: () -> Long? = { null }
) : FirstAccessPreferenceStore {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ) : this(
        prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
        currentUserIdProvider = { sessionManager.currentSession()?.userId }
    )

    override fun isFirstAcessCompleted(): Boolean {
        return prefs.getBoolean(scopedKey(KEY_FIRST_ACCESS_COMPLETED), false)
    }

    override fun setFirstAcessCompleted(completed: Boolean) {
        prefs.edit()
            .putBoolean(scopedKey(KEY_FIRST_ACCESS_COMPLETED), completed)
            .apply()
    }

    private fun scopedKey(baseKey: String): String {
        return "${baseKey}_${currentUserScope()}"
    }

    private fun currentUserScope(): String {
        return currentUserIdProvider()?.toString() ?: ANONYMOUS_SCOPE
    }

    private companion object {
        const val FILE_NAME = "onboarding_preferences"
        const val KEY_FIRST_ACCESS_COMPLETED = "isFirstAcess"
        const val ANONYMOUS_SCOPE = "anonymous"
    }
}

