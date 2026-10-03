package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.AuthRepository
import javax.inject.Inject

/** Whether a stored session exists, i.e. the app can skip the auth flow on launch. */
class HasSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Boolean = repository.hasSession()
}
