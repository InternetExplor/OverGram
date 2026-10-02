package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.RealtimeEvent
import com.example.overgram.domain.repository.RealtimeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Live server events (new/edited/deleted messages, receipts, typing, presence…). */
class ObserveRealtimeEventsUseCase @Inject constructor(
    private val repository: RealtimeRepository
) {
    operator fun invoke(): Flow<RealtimeEvent> = repository.events
}
