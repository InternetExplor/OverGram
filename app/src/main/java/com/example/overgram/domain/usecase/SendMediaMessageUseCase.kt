package com.example.overgram.domain.usecase

import com.example.overgram.domain.model.MediaSendResult
import com.example.overgram.domain.repository.MessageRepository
import javax.inject.Inject

/** Queues a photo, video or document; it uploads and goes out now or once back online. */
class SendMediaMessageUseCase @Inject constructor(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(chatId: String, uri: String, caption: String?, asFile: Boolean): MediaSendResult =
        repository.sendMedia(chatId, uri, caption?.trim()?.takeIf { it.isNotEmpty() }, asFile)
}
