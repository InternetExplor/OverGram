package com.example.overgram.core.di

import android.content.Context
import androidx.room.Room
import com.example.overgram.data.local.db.ChatDao
import com.example.overgram.data.local.db.MessageDao
import com.example.overgram.data.local.db.OverGramDatabase
import com.example.overgram.data.local.db.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OverGramDatabase =
        Room.databaseBuilder(context, OverGramDatabase::class.java, OverGramDatabase.NAME)
            // The DB is only a cache of server data (plus the outbox): rebuilding it is safe.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideMessageDao(db: OverGramDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideChatDao(db: OverGramDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideUserDao(db: OverGramDatabase): UserDao = db.userDao()
}
