package com.example.overgram.domain.model

/**
 * Successful authentication: token pair plus identity of the user and this device.
 */
data class AuthResult(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val deviceId: String,
    val isNewUser: Boolean
)
