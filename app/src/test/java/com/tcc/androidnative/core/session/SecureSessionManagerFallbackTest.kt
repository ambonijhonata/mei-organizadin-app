package com.tcc.androidnative.core.session

import java.security.GeneralSecurityException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureSessionManagerFallbackTest {
    @Test
    fun `should recover when secure store init fails with crypto error`() {
        val logger = RecordingFallbackLogger()
        val store = RecoverableInitFailureStore()

        val manager = SecureSessionManagerImpl(store, logger)

        assertNull(manager.currentSession())
        assertTrue(store.clearStorageCalls >= 1)
        assertTrue(logger.events.any { it.event == "session_secure_store_init_recoverable_failure" })
    }

    @Test
    fun `should clear secure storage and reinit when read fails with crypto error`() {
        val logger = RecordingFallbackLogger()
        val store = ReadFailureThenRecoverStore()

        val manager = SecureSessionManagerImpl(store, logger)

        assertNull(manager.currentSession())
        assertEquals(1, store.clearStorageCalls)
        assertTrue(logger.events.any { it.event == "session_secure_store_recovery_start" })
        assertTrue(logger.events.any { it.event == "session_secure_store_recovery_success" })
    }

    @Test
    fun `should keep user logged out after fallback recovery`() {
        val logger = RecordingFallbackLogger()
        val store = ReadFailureThenRecoverStore()

        val manager = SecureSessionManagerImpl(store, logger)

        assertNull(manager.currentSession())
        assertNull(manager.getAccessToken())
        assertNull(manager.getRefreshToken())
    }

    @Test
    fun `should emit non sensitive telemetry fields`() {
        val logger = RecordingFallbackLogger()
        val store = ReadFailureThenRecoverStore()

        SecureSessionManagerImpl(store, logger)

        val firstError = logger.events.firstOrNull { it.errorClass != null }
        assertNotNull(firstError)
        assertTrue(firstError!!.errorClass!!.contains("Exception"))
        assertTrue(firstError.message!!.contains("AEADBadTagException", ignoreCase = true))
        assertTrue(firstError.message!!.contains("token").not())
    }
}

private class RecoverableInitFailureStore : SecureSessionStore {
    private var createCalls = 0
    var clearStorageCalls = 0
        private set

    override fun create(): SessionSecurePrefs {
        createCalls += 1
        if (createCalls == 1) {
            throw GeneralSecurityException("AEADBadTagException simulated at init")
        }
        return InMemorySessionPrefs()
    }

    override fun clearStorage() {
        clearStorageCalls += 1
    }
}

private class ReadFailureThenRecoverStore : SecureSessionStore {
    private var createCalls = 0
    var clearStorageCalls = 0
        private set

    override fun create(): SessionSecurePrefs {
        createCalls += 1
        return if (createCalls == 1) {
            ThrowingSessionPrefs()
        } else {
            InMemorySessionPrefs()
        }
    }

    override fun clearStorage() {
        clearStorageCalls += 1
    }
}

private class ThrowingSessionPrefs : SessionSecurePrefs {
    override fun getString(key: String, defaultValue: String?): String? {
        throw GeneralSecurityException("AEADBadTagException simulated at read")
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        throw GeneralSecurityException("AEADBadTagException simulated at read")
    }

    override fun edit(): SessionSecurePrefsEditor {
        throw GeneralSecurityException("AEADBadTagException simulated at read")
    }
}

private class InMemorySessionPrefs : SessionSecurePrefs {
    private val stringMap = mutableMapOf<String, String?>()
    private val longMap = mutableMapOf<String, Long>()

    override fun getString(key: String, defaultValue: String?): String? = stringMap[key] ?: defaultValue
    override fun getLong(key: String, defaultValue: Long): Long = longMap[key] ?: defaultValue

    override fun edit(): SessionSecurePrefsEditor = InMemoryEditor(stringMap, longMap)
}

private class InMemoryEditor(
    private val stringMap: MutableMap<String, String?>,
    private val longMap: MutableMap<String, Long>
) : SessionSecurePrefsEditor {
    override fun putLong(key: String, value: Long): SessionSecurePrefsEditor {
        longMap[key] = value
        return this
    }

    override fun putString(key: String, value: String?): SessionSecurePrefsEditor {
        stringMap[key] = value
        return this
    }

    override fun clear(): SessionSecurePrefsEditor {
        stringMap.clear()
        longMap.clear()
        return this
    }

    override fun apply() = Unit
}

private class RecordingFallbackLogger : SessionFallbackLogger {
    val events = mutableListOf<LogEvent>()

    override fun logEvent(event: String, error: Throwable?) {
        events += LogEvent(
            event = event,
            errorClass = error?.javaClass?.simpleName,
            message = error?.message
        )
    }
}

private data class LogEvent(
    val event: String,
    val errorClass: String?,
    val message: String?
)
