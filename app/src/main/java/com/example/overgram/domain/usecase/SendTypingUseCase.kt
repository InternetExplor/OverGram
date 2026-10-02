package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.RealtimeRepository
import javax.inject.Inject

class SendTypingUseCase @Inject constructor(
    private val repository: RealtimeRepository
) {
    operator fun invoke(chatId: String) = repository.sendTyping(chatId)
}
