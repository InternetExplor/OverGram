package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ServerInfo
import com.example.overgram.domain.repository.ProfileRepository
import javax.inject.Inject

class GetServerInfoUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(): AuthOutcome<ServerInfo> = repository.serverInfo()
}
