package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class RefreshChatsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): AuthOutcome<Unit> = repository.refreshChats()
}
