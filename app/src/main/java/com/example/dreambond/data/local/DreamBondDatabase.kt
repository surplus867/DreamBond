package com.example.dreambond.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameProgressEntity::class, CharacterMemoryEntity::class, PlayerJournalEntity::class],
    version = 3,
    exportSchema = false
)
abstract class DreamBondDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao
    abstract fun characterMemoryDao(): CharacterMemoryDao
    abstract fun playerJournalDao(): PlayerJournalDao
}