package com.tcc.androidnative.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tcc.androidnative.core.session.SessionManager
import com.tcc.androidnative.core.session.UserSession
import com.tcc.androidnative.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    val sessionState: StateFlow<UserSession?> = sessionManager.sessionState

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
