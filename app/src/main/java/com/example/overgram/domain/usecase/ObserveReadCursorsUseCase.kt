package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.RealtimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** How far each member has read [chatId] (userId → seq), as seen since the app started. */
class ObserveReadCursorsUseCase @Inject constructor(
    private val repository: RealtimeRepository
) {
    operator fun invoke(chatId: String): Flow<Map<String, Long>> = repository.readCursors(chatId)
}
