package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/** Queues a message; it goes out now or as soon as there's a connection. Returns its id. */
class SendTextMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, text: String): String = repository.sendText(chatId, text)
}
