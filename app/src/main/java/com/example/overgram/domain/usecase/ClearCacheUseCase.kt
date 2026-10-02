package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.ProfileRepository
import javax.inject.Inject

/** Frees local storage; chats and history are downloaded again when opened. Unsent messages stay. */
class ClearCacheUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke() = repository.clearCache()
}
