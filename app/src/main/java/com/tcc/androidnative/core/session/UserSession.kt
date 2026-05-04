package com.tcc.androidnative.core.session

data class UserSession(
    val userId: Long,
    val email: String,
    val name: String,
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAtEpochSeconds: Long,
    val refreshTokenExpiresAtEpochSeconds: Long
) {
    val idToken: String
        get() = accessToken
}
