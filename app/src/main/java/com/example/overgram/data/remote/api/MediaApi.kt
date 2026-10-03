package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.StartUploadRequestDto
import com.example.overgram.data.remote.dto.StartUploadResultDto
import com.example.overgram.data.remote.dto.UploadChunkResultDto
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** Resumable media uploads. Bearer token via [AuthInterceptor]; downloads go through OkHttp directly. */
interface MediaApi {

    @POST("v1/media/uploads")
    suspend fun startUpload(@Body body: StartUploadRequestDto): Response<StartUploadResultDto>

    /** [offset] must equal the server's confirmed offset (409 OFFSET_MISMATCH otherwise). */
    @PUT("v1/media/uploads/{uploadId}")
    suspend fun uploadChunk(
        @Path("uploadId") uploadId: String,
        @Header("Upload-Offset") offset: Long,
        @Body chunk: RequestBody
    ): Response<UploadChunkResultDto>

    /** The confirmed offset comes back in the `Upload-Offset` response header. */
    @HEAD("v1/media/uploads/{uploadId}")
    suspend fun uploadOffset(@Path("uploadId") uploadId: String): Response<Void>
}
