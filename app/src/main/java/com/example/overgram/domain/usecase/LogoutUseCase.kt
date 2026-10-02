package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): AuthOutcome<Unit> = repository.logout()
}
