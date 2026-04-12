package com.tcc.androidnative.feature.settings.data

import android.content.SharedPreferences
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesCalendarSyncSettingsStoreTest {
    @Test
    fun `defaults should use device local date with filter disabled and setup pending`() {
        val store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = FakeSharedPreferences(),
            currentDateProvider = { LocalDate.of(2026, 4, 12) }
        )

        val settings = store.getSettings()

        assertFalse(settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 12), settings.startDate)
        assertFalse(settings.initialSetupCompleted)
    }

    @Test
    fun `save should persist values and mark initial setup completed`() {
        val store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = FakeSharedPreferences(),
            currentDateProvider = { LocalDate.of(2026, 4, 12) }
        )

        store.saveSettings(
            useStartDateFilter = true,
            startDate = LocalDate.of(2026, 4, 7)
        )

        val settings = store.getSettings()
        assertTrue(settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), settings.startDate)
        assertTrue(settings.initialSetupCompleted)
    }

    @Test
    fun `settings should be isolated by authenticated user id`() {
        val prefs = FakeSharedPreferences()
        val user1Store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = prefs,
            currentDateProvider = { LocalDate.of(2026, 4, 12) },
            currentUserIdProvider = { 1L }
        )
        val user2Store = SharedPreferencesCalendarSyncSettingsStore(
            prefs = prefs,
            currentDateProvider = { LocalDate.of(2026, 4, 12) },
            currentUserIdProvider = { 2L }
        )

        user1Store.saveSettings(
            useStartDateFilter = true,
            startDate = LocalDate.of(2026, 4, 7)
        )
        user2Store.saveSettings(
            useStartDateFilter = false,
            startDate = LocalDate.of(2026, 5, 1)
        )

        val user1Settings = user1Store.getSettings()
        val user2Settings = user2Store.getSettings()
        assertTrue(user1Settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), user1Settings.startDate)
        assertTrue(user1Settings.initialSetupCompleted)
        assertFalse(user2Settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 5, 1), user2Settings.startDate)
        assertTrue(user2Settings.initialSetupCompleted)
    }

    @Test
    fun `anonymous fallback scope should remain stable when user id is unavailable`() {
        val prefs = FakeSharedPreferences()
        val anonymousStore = SharedPreferencesCalendarSyncSettingsStore(
            prefs = prefs,
            currentDateProvider = { LocalDate.of(2026, 4, 12) },
            currentUserIdProvider = { null }
        )

        anonymousStore.saveSettings(
            useStartDateFilter = true,
            startDate = LocalDate.of(2026, 4, 7)
        )

        val settings = anonymousStore.getSettings()
        assertTrue(settings.useStartDateFilter)
        assertEquals(LocalDate.of(2026, 4, 7), settings.startDate)
        assertTrue(settings.initialSetupCompleted)
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? {
        return values[key] as? String ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        return (values[key] as? MutableSet<String>) ?: defValues
    }

    override fun getInt(key: String?, defValue: Int): Int {
        return values[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long {
        return values[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float {
        return values[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor(values)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    }

    private class Editor(
        private val values: MutableMap<String, Any?>
    ) : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private var clearRequested: Boolean = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            pending[key.orEmpty()] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            pending[key.orEmpty()] = values
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            pending[key.orEmpty()] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            pending[key.orEmpty()] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            pending[key.orEmpty()] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            pending[key.orEmpty()] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            pending[key.orEmpty()] = null
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            pending.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) {
                values.clear()
            }
            pending.forEach { (key, value) ->
                if (value == null) {
                    values.remove(key)
                } else {
                    values[key] = value
                }
            }
            pending.clear()
            clearRequested = false
        }
    }
}
