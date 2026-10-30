package com.tcc.androidnative.testutil

import android.content.SharedPreferences

class FakeSharedPreferences : SharedPreferences {
    private val values = linkedMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
        val storedValues = values[key] as? Set<*> ?: return defValues
        return storedValues.filterIsInstance<String>().toMutableSet()
    }

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = Editor(values)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    private class Editor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?) = stage(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?) = stage(key, values)
        override fun putInt(key: String?, value: Int) = stage(key, value)
        override fun putLong(key: String?, value: Long) = stage(key, value)
        override fun putFloat(key: String?, value: Float) = stage(key, value)
        override fun putBoolean(key: String?, value: Boolean) = stage(key, value)
        override fun remove(key: String?) = stage(key, null)

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
            pending.clear()
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearRequested) values.clear()
            pending.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
            pending.clear()
            clearRequested = false
        }

        private fun stage(key: String?, value: Any?): SharedPreferences.Editor = apply {
            pending[key.orEmpty()] = value
        }
    }
}
