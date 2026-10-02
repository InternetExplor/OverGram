package com.example.overgram

import android.app.Application
import com.example.overgram.data.realtime.RealtimeSync
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class OverGramApp : Application() {

    @Inject
    lateinit var realtimeSync: RealtimeSync

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Live events → local database, and queued messages out, for as long as the process lives.
        realtimeSync.start()
    }
}
