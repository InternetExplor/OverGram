package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/** Changes the text of our own message (sender only, within 48 hours of sending). */
class EditMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String, text: String): AuthOutcome<Unit> {
        val trimmed = text.trim()
        // Relay rejects a blank body; say so without a round trip.
        if (trimmed.isEmpty()) return AuthOutcome.Failure(AuthError.Validation(null))
        return repository.editMessage(clientMessageId, trimmed)
    }
}
