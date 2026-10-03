package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.ChatReceipts
import com.example.overgram.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** How far the other members of a chat got with our messages (delivered / read ticks). */
class ObserveReceiptsUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<ChatReceipts> = repository.observeReceipts(chatId)
}
