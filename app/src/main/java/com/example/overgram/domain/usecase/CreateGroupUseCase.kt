package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

/** Creates a GROUP chat with the caller as OWNER; returns the chat id. */
class CreateGroupUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(title: String, memberIds: List<String>): AuthOutcome<String> =
        repository.createGroup(title.trim(), memberIds)

    companion object {
        const val MAX_TITLE_LENGTH = 128
    }
}
