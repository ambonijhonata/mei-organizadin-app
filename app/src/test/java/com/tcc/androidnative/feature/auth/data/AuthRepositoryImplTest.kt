package com.tcc.androidnative.feature.auth.data

import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import com.tcc.androidnative.feature.auth.data.remote.AuthApi
import com.tcc.androidnative.feature.auth.data.remote.dto.LoginRequestDto
import com.tcc.androidnative.feature.auth.data.remote.dto.LoginResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class AuthRepositoryImplTest {
    @Test
    fun `login should persist session on success`() = runBlocking {
        val fakeApi = FakeAuthApi(
            response = LoginResponseDto(
                userId = 10L,
                email = "user@test.com",
                name = "User"
            )
        )
        val fakeSession = FakeSessionManager()
        val repository = AuthRepositoryImpl(fakeApi, fakeSession)

        val session = repository.login(idToken = "id-token", authorizationCode = "auth-code")

        assertEquals(10L, session.userId)
        assertEquals("user@test.com", fakeSession.currentSession()?.email)
        assertEquals("id-token", fakeSession.getIdToken())
    }

    @Test
    fun `login should propagate failure when api returns error`() = runBlocking {
        val fakeApi = FakeAuthApi(error = IllegalStateException("boom"))
        val fakeSession = FakeSessionManager()
        val repository = AuthRepositoryImpl(fakeApi, fakeSession)

        try {
            repository.login(idToken = "id-token", authorizationCode = "auth-code")
            fail("Expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals("boom", expected.message)
        }
    }

    @Test
    fun `logout should clear persisted session`() {
        val fakeApi = FakeAuthApi(
            response = LoginResponseDto(
                userId = 1L,
                email = "a@a.com",
                name = "A"
            )
        )
        val fakeSession = FakeSessionManager().apply {
            saveSession(UserSession(1L, "a@a.com", "A", "id-token"))
        }
        val repository = AuthRepositoryImpl(fakeApi, fakeSession)

        repository.logout()

        assertNull(fakeSession.currentSession())
        assertNull(fakeSession.getIdToken())
    }
}

private class FakeAuthApi(
    private val response: LoginResponseDto? = null,
    private val error: Throwable? = null
) : AuthApi {
    override suspend fun login(body: LoginRequestDto): LoginResponseDto {
        error?.let { throw it }
        return requireNotNull(response)
    }
}

private class FakeSessionManager : SessionManager {
    private val mutableState = MutableStateFlow<UserSession?>(null)
    override val sessionState: StateFlow<UserSession?> = mutableState

    override fun saveSession(session: UserSession) {
        mutableState.value = session
    }

    override fun clearSession() {
        mutableState.value = null
    }

    override fun currentSession(): UserSession? = mutableState.value

    override fun getIdToken(): String? = mutableState.value?.idToken
}
