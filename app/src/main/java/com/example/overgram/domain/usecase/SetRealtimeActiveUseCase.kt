package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.RealtimeRepository
import javax.inject.Inject

/** Keeps the live connection up while the app is in the foreground (and logged in). */
class SetRealtimeActiveUseCase @Inject constructor(
    private val repository: RealtimeRepository
) {
    operator fun invoke(active: Boolean) {
        if (active) repository.start() else repository.stop()
    }
}
