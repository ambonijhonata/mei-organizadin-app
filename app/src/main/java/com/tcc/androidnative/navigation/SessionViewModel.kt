package com.tcc.androidnative.navigation

import androidx.lifecycle.ViewModel
import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    val sessionState: StateFlow<UserSession?> = sessionManager.sessionState

    fun logout() {
        sessionManager.clearSession()
    }
}
