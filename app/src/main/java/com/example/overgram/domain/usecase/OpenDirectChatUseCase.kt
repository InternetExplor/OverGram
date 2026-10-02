package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

/** Gets or creates the DIRECT chat with a user; returns the chat id. */
class OpenDirectChatUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(peerUserId: String): AuthOutcome<String> =
        repository.openDirectChat(peerUserId)
}
