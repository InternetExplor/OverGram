package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/** Drops a message that hasn't gone out yet, stopping its upload. */
class CancelSendingUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String) = repository.cancelSending(clientMessageId)
}
