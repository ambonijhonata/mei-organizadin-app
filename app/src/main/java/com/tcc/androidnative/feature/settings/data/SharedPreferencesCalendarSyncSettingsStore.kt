package com.tcc.androidnative.feature.settings.data

import android.content.Context
import android.content.SharedPreferences
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.core.session.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesCalendarSyncSettingsStore internal constructor(
    private val prefs: SharedPreferences,
    private val currentDateProvider: () -> LocalDate = { LocalDate.now(ZoneId.systemDefault()) },
    private val currentUserIdProvider: () -> Long? = { null }
) : CalendarSyncSettingsStore {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ) : this(
        prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE),
        currentDateProvider = { LocalDate.now(ZoneId.systemDefault()) },
        currentUserIdProvider = { sessionManager.currentSession()?.userId }
    )

    override fun getSettings(): CalendarSyncSettings {
        val defaultDate = currentDateProvider()
        val storedDateValue = prefs.getString(scopedKey(KEY_START_DATE_ISO), null)
        val storedDate = runCatching {
            storedDateValue?.let(DateFormats::parseApiDate)
        }.getOrNull()

        return CalendarSyncSettings(
            useStartDateFilter = prefs.getBoolean(scopedKey(KEY_USE_START_DATE_FILTER), false),
            startDate = storedDate ?: defaultDate,
            initialSetupCompleted = prefs.getBoolean(scopedKey(KEY_INITIAL_SETUP_COMPLETED), false)
        )
    }

    override fun saveSettings(useStartDateFilter: Boolean, startDate: LocalDate) {
        prefs.edit()
            .putBoolean(scopedKey(KEY_USE_START_DATE_FILTER), useStartDateFilter)
            .putString(scopedKey(KEY_START_DATE_ISO), DateFormats.toApiDate(startDate))
            .putBoolean(scopedKey(KEY_INITIAL_SETUP_COMPLETED), true)
            .apply()
    }

    private fun scopedKey(baseKey: String): String {
        return "${baseKey}_${currentUserScope()}"
    }

    private fun currentUserScope(): String {
        return currentUserIdProvider()?.toString() ?: ANONYMOUS_SCOPE
    }

    private companion object {
        const val FILE_NAME = "calendar_sync_settings"
        const val KEY_USE_START_DATE_FILTER = "use_start_date_filter"
        const val KEY_START_DATE_ISO = "start_date_iso"
        const val KEY_INITIAL_SETUP_COMPLETED = "initial_setup_completed"
        const val ANONYMOUS_SCOPE = "anonymous"
    }
}
