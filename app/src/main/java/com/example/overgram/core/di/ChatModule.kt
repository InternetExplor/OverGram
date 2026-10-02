package com.example.overgram.core.di

import com.example.overgram.data.repository.ChatRepositoryImpl
import com.example.overgram.data.repository.MessageRepositoryImpl
import com.example.overgram.domain.repository.ChatRepository
import com.example.overgram.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository
}
