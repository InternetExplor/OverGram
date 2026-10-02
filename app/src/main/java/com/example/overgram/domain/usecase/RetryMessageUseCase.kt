package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

class RetryMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(clientMessageId: String) = repository.retry(clientMessageId)
}
