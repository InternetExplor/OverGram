package com.example.overgram.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OtpRequestDto(
    @SerializedName("phone") val phone: String
)

data class OtpVerifyRequestDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("code") val code: String,
    @SerializedName("deviceName") val deviceName: String
)

data class RefreshRequestDto(
    @SerializedName("refreshToken") val refreshToken: String
)

/**
 * Fields are nullable because Gson ignores Kotlin nullability; the mapper validates them.
 */
data class TokenPairDto(
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("isNewUser") val isNewUser: Boolean?
)

/**
 * Shared Relay error body: `{ code, message, retryable }`.
 * `botUrl` is only sent with 409 TELEGRAM_NOT_LINKED; `remainingAttempts` is not in the
 * current contract but is read if the server ever starts sending it.
 */
data class ErrorDto(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("retryable") val retryable: Boolean?,
    @SerializedName("botUrl") val botUrl: String? = null,
    @SerializedName("remainingAttempts") val remainingAttempts: Int? = null
)
