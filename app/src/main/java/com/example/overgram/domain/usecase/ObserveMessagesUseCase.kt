package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.StoredMessage
import com.example.overgram.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** A chat's stored messages (queued outgoing first, then newest first). */
class ObserveMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(chatId: String): Flow<List<StoredMessage>> = repository.observeMessages(chatId)
}
