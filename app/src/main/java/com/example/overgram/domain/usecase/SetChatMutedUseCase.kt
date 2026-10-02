package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class SetChatMutedUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, muted: Boolean): AuthOutcome<ChatSummary> =
        repository.setMuted(chatId, muted)
}
