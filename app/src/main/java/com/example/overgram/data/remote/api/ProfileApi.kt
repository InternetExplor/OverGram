package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.ServerInfoDto
import com.example.overgram.data.remote.dto.StartUploadRequestDto
import com.example.overgram.data.remote.dto.StartUploadResultDto
import com.example.overgram.data.remote.dto.UploadChunkResultDto
import com.example.overgram.data.remote.dto.UserMeDto
import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** The caller's own profile, media uploads and server info. Bearer token via [AuthInterceptor]. */
interface ProfileApi {

    @GET("v1/users/me")
    suspend fun getMe(): Response<UserMeDto>

    /**
     * Partial update: only the fields present are changed. A JsonObject body so that
     * `avatarMediaId: null` (remove the avatar) can be sent explicitly.
     */
    @PATCH("v1/users/me")
    suspend fun updateMe(@Body body: JsonObject): Response<UserMeDto>

    @POST("v1/media/uploads")
    suspend fun startUpload(@Body body: StartUploadRequestDto): Response<StartUploadResultDto>

    /** [offset] must equal the server's confirmed offset (409 OFFSET_MISMATCH otherwise). */
    @PUT("v1/media/uploads/{uploadId}")
    suspend fun uploadChunk(
        @Path("uploadId") uploadId: String,
        @Header("Upload-Offset") offset: Long,
        @Body chunk: RequestBody
    ): Response<UploadChunkResultDto>

    @Headers(AuthInterceptor.NO_AUTH_HEADER)
    @GET("v1/server/info")
    suspend fun getServerInfo(): Response<ServerInfoDto>
}
