package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.SentMessage
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class SendTextMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, clientMessageId: String, text: String): AuthOutcome<SentMessage> =
        repository.sendText(chatId, clientMessageId, text)
}
