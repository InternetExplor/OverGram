package com.example.overgram.core.di

import android.os.Build
import com.example.overgram.data.repository.AuthRepositoryImpl
import com.example.overgram.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** Human-readable name of this device, sent to the server on OTP verify. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceName

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        @DeviceName
        fun provideDeviceName(): String = Build.MODEL ?: "Android"
    }
}
