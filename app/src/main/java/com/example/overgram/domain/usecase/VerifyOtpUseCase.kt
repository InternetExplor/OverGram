package com.example.overgram.domain.usecase

import com.example.overgram.core.di.DeviceName
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.AuthResult
import com.example.overgram.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val repository: AuthRepository,
    @param:DeviceName private val deviceName: String
) {
    suspend operator fun invoke(phone: String, code: String): AuthOutcome<AuthResult> =
        repository.verifyOtp(phone, code, deviceName)
}
