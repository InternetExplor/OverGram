package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/** Loads the page before the oldest stored message; the result says if there's more. */
class LoadOlderMessagesUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String): AuthOutcome<Boolean> = repository.loadOlder(chatId)
}
