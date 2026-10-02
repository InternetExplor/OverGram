package com.example.overgram.core.di

import android.content.Context
import android.content.SharedPreferences
import com.example.overgram.data.local.prefs.SyncPreferences
import com.example.overgram.data.local.prefs.TokenPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthPrefs

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    @AuthPrefs
    fun provideAuthSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences(TokenPreferences.PREFS_NAME, Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideTokenPreferences(@AuthPrefs prefs: SharedPreferences): TokenPreferences =
        TokenPreferences(prefs)

    @Provides
    @Singleton
    fun provideSyncPreferences(@AuthPrefs prefs: SharedPreferences): SyncPreferences =
        SyncPreferences(prefs)
}
