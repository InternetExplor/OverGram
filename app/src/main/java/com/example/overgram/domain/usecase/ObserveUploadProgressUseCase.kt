package com.example.overgram.domain.usecase

import com.example.overgram.domain.repository.MessageRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Upload progress (0..1) of queued attachments, by clientMessageId. */
class ObserveUploadProgressUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    operator fun invoke(): StateFlow<Map<String, Float>> = repository.uploadProgress
}
