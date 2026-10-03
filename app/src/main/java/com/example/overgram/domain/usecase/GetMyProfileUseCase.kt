package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.repository.ProfileRepository
import javax.inject.Inject

/** Loads the own profile from the server (also updates [ObserveMyProfileUseCase]). */
class GetMyProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): AuthOutcome<MyProfile> = repository.refresh()
}
