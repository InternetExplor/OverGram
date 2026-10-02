package com.example.overgram.core.di

import com.example.overgram.data.realtime.RealtimeClient
import com.example.overgram.domain.repository.RealtimeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** OkHttp client for the WebSocket: no read timeout (the socket idles), client pings to detect dead links. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RealtimeHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {

    @Binds
    @Singleton
    abstract fun bindRealtimeRepository(impl: RealtimeClient): RealtimeRepository

    companion object {
        @Provides
        @Singleton
        @RealtimeHttpClient
        fun provideRealtimeHttpClient(base: OkHttpClient): OkHttpClient =
            base.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                // The server drops sockets silent for 60 s and pings every 25 s; our own pings
                // also notice a dead network quickly.
                .pingInterval(20, TimeUnit.SECONDS)
                .build()
    }
}
