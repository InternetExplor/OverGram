package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

class RefreshMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String): AuthOutcome<Unit> = repository.refreshNewest(chatId)
}
