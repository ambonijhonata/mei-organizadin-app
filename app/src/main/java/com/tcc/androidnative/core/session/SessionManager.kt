package com.tcc.androidnative.core.session

import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val sessionState: StateFlow<UserSession?>
    fun saveSession(session: UserSession)
    fun clearSession()
    fun currentSession(): UserSession?
    fun getIdToken(): String?
}
