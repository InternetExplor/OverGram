package com.example.overgram.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `UserMe`: the public profile plus the caller's own phone number. */
data class UserMeDto(
    @SerializedName("id") val id: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("avatarMediaId") val avatarMediaId: String?,
    @SerializedName("phone") val phone: String?
)

data class StartUploadRequestDto(
    @SerializedName("kind") val kind: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("sizeBytes") val sizeBytes: Long,
    /** Hex SHA-256 of the whole file; checked by the server after the last chunk. */
    @SerializedName("sha256") val sha256: String,
    @SerializedName("width") val width: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("durationMs") val durationMs: Long? = null
)

data class StartUploadResultDto(
    @SerializedName("uploadId") val uploadId: String?,
    @SerializedName("mediaId") val mediaId: String?,
    @SerializedName("chunkSize") val chunkSize: Int?
)

data class UploadChunkResultDto(
    @SerializedName("confirmedOffset") val confirmedOffset: Long?,
    @SerializedName("mediaReady") val mediaReady: Boolean?
)

/** 409 OFFSET_MISMATCH carries where the server actually is. */
data class OffsetMismatchDto(
    @SerializedName("code") val code: String?,
    @SerializedName("currentOffset") val currentOffset: Long?
)

data class ServerInfoDto(
    @SerializedName("version") val version: String?,
    @SerializedName("pushEnabled") val pushEnabled: Boolean?
)
