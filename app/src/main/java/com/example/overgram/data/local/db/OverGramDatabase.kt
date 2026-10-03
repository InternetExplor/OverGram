package com.example.overgram.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MessageEntity::class, ChatEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class OverGramDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "overgram.db"

        /** Attachments. A real migration so messages still waiting in the outbox survive the update. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN media TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN uploadId TEXT")
            }
        }
    }
}
