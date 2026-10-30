package com.tcc.androidnative.feature.onboarding.data

import com.tcc.androidnative.testutil.FakeSharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesFirstAccessPreferenceStoreTest {
    @Test
    fun `first access should be false by default`() {
        val store = SharedPreferencesFirstAccessPreferenceStore(prefs = FakeSharedPreferences())

        assertFalse(store.isFirstAcessCompleted())
    }

    @Test
    fun `set completed should persist true value`() {
        val store = SharedPreferencesFirstAccessPreferenceStore(prefs = FakeSharedPreferences())

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
