package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.repository.RealtimeRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: RealtimeRepository
) {
    operator fun invoke(): StateFlow<ConnectionState> = repository.connectionState
}
