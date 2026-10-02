package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.ServerInfo
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {

    /** The last loaded own profile (null until [refresh] succeeds once this session). */
    val me: StateFlow<MyProfile?>

    suspend fun refresh(): AuthOutcome<MyProfile>

    /**
     * Saves the fields that changed (null = leave as is). A taken username fails with
     * [com.example.overgram.domain.model.AuthError.Validation] carrying the server's message.
     */
    suspend fun update(displayName: String?, username: String?): AuthOutcome<MyProfile>

    /** Reads the picked image ([imageUri] from the system photo picker), uploads it and sets it as avatar. */
    suspend fun setAvatar(imageUri: String): AuthOutcome<MyProfile>

    suspend fun removeAvatar(): AuthOutcome<MyProfile>

    suspend fun serverInfo(): AuthOutcome<ServerInfo>

    /**
     * Drops locally stored chats, history and profiles (they're re-downloaded on demand).
     * Messages still waiting to be sent are kept.
     */
    suspend fun clearCache()
}
