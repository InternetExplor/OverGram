package com.example.overgram.data.remote.api

import com.example.overgram.data.remote.dto.UpdatesPageDto
import com.example.overgram.data.remote.dto.UpdatesStateDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/** The REST side of the update stream: catch-up after a gap or a reconnect. */
interface SyncApi {

    /** The caller's current cursor, without events. Read BEFORE the chat list at bootstrap. */
    @GET("v1/updates/state")
    suspend fun getState(): Response<UpdatesStateDto>

    /** Events strictly after [since], ascending and gap-free. */
    @GET("v1/updates")
    suspend fun getUpdates(
        @Query("since") since: Long,
        @Query("limit") limit: Int
    ): Response<UpdatesPageDto>
}
