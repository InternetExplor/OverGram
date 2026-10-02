package com.example.overgram.presentation.common

import com.example.overgram.BuildConfig

/**
 * URL of a media file for the image loader (`GET /v1/media/{id}`, authenticated by the app's
 * HTTP client). A media id never changes content, so it also works as the cache key.
 */
fun mediaUrl(mediaId: String?): String? =
    mediaId?.takeIf { it.isNotBlank() }?.let { BuildConfig.API_BASE_URL + "v1/media/" + it }

/** "+998901234567" → "+998 90 123 45 67"; other formats are returned as they are. */
fun formatPhone(phone: String): String {
    val digits = phone.removePrefix("+")
    if (!phone.startsWith("+998") || digits.length != 12 || !digits.all(Char::isDigit)) return phone
    return "+998 ${digits.substring(3, 5)} ${digits.substring(5, 8)} ${digits.substring(8, 10)} ${digits.substring(10)}"
}
