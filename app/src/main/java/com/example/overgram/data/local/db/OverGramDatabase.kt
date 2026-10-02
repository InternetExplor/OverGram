package com.example.overgram.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, ChatEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OverGramDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "overgram.db"
    }
}
