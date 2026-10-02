package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.repository.ProfileRepository
import javax.inject.Inject

/** Saves name and/or username; pass null for a field that didn't change. */
class UpdateProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(displayName: String?, username: String?): AuthOutcome<MyProfile> =
        repository.update(displayName?.trim(), username?.trim())

    companion object {
        const val MAX_NAME_LENGTH = 128
    }
}
