package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatMember
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class AddMembersUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, userIds: List<String>): AuthOutcome<List<ChatMember>> =
        repository.addMembers(chatId, userIds)
}
