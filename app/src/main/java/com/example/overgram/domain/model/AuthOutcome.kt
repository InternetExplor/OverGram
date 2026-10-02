package com.example.overgram.domain.model

/**
 * Result wrapper for auth operations: either a value or a typed [AuthError].
 */
sealed interface AuthOutcome<out T> {
    data class Success<T>(val value: T) : AuthOutcome<T>
    data class Failure(val error: AuthError) : AuthOutcome<Nothing>
}
