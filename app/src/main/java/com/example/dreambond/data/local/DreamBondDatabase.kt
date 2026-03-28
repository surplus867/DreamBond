package com.example.dreambond.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DreamBondDatabase : RoomDatabase() {
    abstract fun gameProgressDao(): GameProgressDao
}