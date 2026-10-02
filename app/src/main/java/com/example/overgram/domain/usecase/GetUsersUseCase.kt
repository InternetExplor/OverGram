package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

/** Best-effort, cached profiles for several users (e.g. group message senders). */
class GetUsersUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(userIds: Collection<String>): Map<String, UserProfile> =
        repository.getUsers(userIds)
}
