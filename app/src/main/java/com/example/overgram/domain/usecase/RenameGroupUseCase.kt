package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class RenameGroupUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, title: String): AuthOutcome<ChatSummary> =
        repository.renameGroup(chatId, title.trim())
}
