package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

class MarkChatReadUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, upToSeq: Long): AuthOutcome<Unit> =
        repository.markRead(chatId, upToSeq)
}
