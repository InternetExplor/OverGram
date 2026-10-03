package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class RemoveMemberUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, userId: String): AuthOutcome<Unit> =
        repository.removeMember(chatId, userId)
}
