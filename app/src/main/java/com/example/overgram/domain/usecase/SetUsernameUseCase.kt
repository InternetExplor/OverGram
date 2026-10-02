package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.repository.ProfileRepository
import javax.inject.Inject

class SetUsernameUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(username: String): AuthOutcome<MyProfile> =
        repository.update(displayName = null, username = username)

    companion object {
        /** Server rule for usernames. */
        val USERNAME_PATTERN = Regex("^[a-zA-Z0-9_]{3,32}$")
    }
}
