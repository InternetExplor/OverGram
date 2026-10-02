package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.repository.ProfileRepository
import javax.inject.Inject

class SetAvatarUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    /** [imageUri] as returned by the system photo picker. */
    suspend operator fun invoke(imageUri: String): AuthOutcome<MyProfile> = repository.setAvatar(imageUri)
}
