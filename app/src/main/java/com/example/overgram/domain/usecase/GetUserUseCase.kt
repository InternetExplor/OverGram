package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(userId: String): AuthOutcome<UserProfile> = repository.getUser(userId)
}
