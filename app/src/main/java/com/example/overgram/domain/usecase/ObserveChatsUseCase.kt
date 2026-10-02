package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The stored chat list; emits right away (works offline) and on every change. */
class ObserveChatsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatSummary>> = repository.observeChats()
}
