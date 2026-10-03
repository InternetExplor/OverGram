package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/**
 * Queues a message; it goes out now or as soon as there's a connection. [replyTo] is the
 * clientMessageId of the message being answered. Returns the new message's id.
 */
class SendTextMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, text: String, replyTo: String? = null): String =
        repository.sendText(chatId, text, replyTo)
}
