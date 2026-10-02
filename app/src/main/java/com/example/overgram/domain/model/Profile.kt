package com.example.overgram.domain.model

/** The logged-in user's own profile: the public one plus the phone number only they see. */
data class MyProfile(
    val id: String,
    val displayName: String,
    val username: String?,
    val avatarMediaId: String?,
    val phone: String
)

data class ServerInfo(
    val version: String,
    val pushEnabled: Boolean
)
