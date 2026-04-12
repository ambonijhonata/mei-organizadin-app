package com.tcc.androidnative.feature.auth.ui

import android.content.Intent
import com.tcc.androidnative.R
import com.tcc.androidnative.core.session.UserSession
import com.tcc.androidnative.feature.auth.data.AuthRepository
import com.tcc.androidnative.feature.auth.data.google.GoogleAuthTokens
import com.tcc.androidnative.feature.auth.data.google.GoogleSignInGateway
import com.tcc.androidnative.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful google tokens should authenticate and emit one shot success event`() = runTest {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository(shouldFail = false),
            googleSignInGateway = FakeGoogleSignInGateway()
        )
        val eventDeferred = async { viewModel.loginSuccessEvents.first() }

        viewModel.onGoogleTokensReceived(
            GoogleAuthTokens(
                idToken = "id-token",
                authorizationCode = "auth-code"
            )
        )
        advanceUntilIdle()

        eventDeferred.await()
        assertTrue(viewModel.uiState.value.isAuthenticated)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `failed login should not emit success event and should expose error state`() = runTest {
        val viewModel = LoginViewModel(
            authRepository = FakeAuthRepository(shouldFail = true),
            googleSignInGateway = FakeGoogleSignInGateway()
        )

        viewModel.onGoogleTokensReceived(
            GoogleAuthTokens(
                idToken = "id-token",
                authorizationCode = "auth-code"
            )
        )
        runCurrent()

        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertEquals(R.string.login_feedback_error, viewModel.uiState.value.transientMessage?.textResId)
    }
}

private class FakeAuthRepository(
    private val shouldFail: Boolean
) : AuthRepository {
    override suspend fun login(idToken: String, authorizationCode: String): UserSession {
        if (shouldFail) {
            throw IllegalStateException("login failed")
        }
        return UserSession(
            userId = 1L,
            email = "test@example.com",
            name = "Test User",
            idToken = idToken
        )
    }

    override fun logout() {
    }
}

private class FakeGoogleSignInGateway : GoogleSignInGateway {
    override fun signInIntent(): Intent = Intent("fake-sign-in")

    override fun extractTokens(data: Intent?): Result<GoogleAuthTokens> {
        return Result.success(
            GoogleAuthTokens(
                idToken = "id-token",
                authorizationCode = "auth-code"
            )
        )
    }
}
