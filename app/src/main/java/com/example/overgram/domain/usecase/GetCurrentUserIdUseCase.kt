package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.ChatRepository
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): String? = repository.currentUserId()
}
