package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveMyProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(): StateFlow<MyProfile?> = repository.me
}
