package com.example.overgram

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.overgram.data.realtime.RealtimeSync
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class OverGramApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var realtimeSync: RealtimeSync

    /** The app's client: attaches the bearer token (media downloads require it) and refreshes it. */
    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Live events → local database, and queued messages out, for as long as the process lives.
        realtimeSync.start()
    }

    /** Coil loads avatars from `GET /v1/media/{id}`, which needs auth. Media ids never change content. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
}
