package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class SetUsernameUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(username: String): AuthOutcome<UserProfile> =
        repository.setUsername(username)

    companion object {
        /** Server rule for usernames. */
        val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]{3,32}$")
    }
}
