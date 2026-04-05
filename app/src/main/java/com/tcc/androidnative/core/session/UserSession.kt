package com.tcc.androidnative.core.session

data class UserSession(
    val userId: Long,
    val email: String,
    val name: String,
    val idToken: String
)

