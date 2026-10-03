package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/** Deletes a message for everyone; an unsent one is just dropped. */
class DeleteMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String): AuthOutcome<Unit> = repository.deleteMessage(clientMessageId)
}
