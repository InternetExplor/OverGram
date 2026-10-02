package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MessagePage
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, beforeSeq: Long? = null): AuthOutcome<MessagePage> =
        repository.getMessages(chatId, beforeSeq)
}
