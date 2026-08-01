package com.example.dreambond.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_memories")
data class CharacterMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val characterName: String,
    val memoryType: String, // "favorite_food", "favorite_date", "favorite_time", "scene", "interaction"
    val content: String,
    val unlocked: Boolean = false,
    val unlockedDate: Long? = null, // timestamp
    val affectionRequired: Int = 0
)