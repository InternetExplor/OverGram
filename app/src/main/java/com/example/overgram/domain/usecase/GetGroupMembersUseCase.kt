package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.GroupMembers
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class GetGroupMembersUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String): AuthOutcome<GroupMembers> =
        repository.getGroupMembers(chatId)
}
