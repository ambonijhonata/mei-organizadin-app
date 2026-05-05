package com.tcc.androidnative.feature.onboarding.data

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesFirstAccessPreferenceStoreTest {
    @Test
    fun `first access should be false by default`() {
        val store = SharedPreferencesFirstAccessPreferenceStore(
            prefs = FakeSharedPreferences()
        )

        assertFalse(store.isFirstAcessCompleted())
    }

    @Test
    fun `set completed should persist true value`() {
        val store = SharedPreferencesFirstAccessPreferenceStore(
            prefs = FakeSharedPreferences()
        )

        store.setFirstAcessCompleted(completed = true)

        assertTrue(store.isFirstAcessCompleted())
    }

    @Test
    fun `first access preference should be isolated by authenticated user id`() {
        val prefs = FakeSharedPreferences()
        val user1Store = SharedPreferencesFirstAccessPreferenceStore(
            prefs = prefs,
            currentUserIdProvider = { 1L }
        )
        val user2Store = SharedPreferencesFirstAccessPreferenceStore(
            prefs = prefs,
            currentUserIdProvider = { 2L }
        )

        user1Store.setFirstAcessCompleted(completed = true)

        assertTrue(user1Store.isFirstAcessCompleted())
        assertFalse(user2Store.isFirstAcessCompleted())
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

