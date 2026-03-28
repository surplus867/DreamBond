package com.example.dreambond.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey val id: Int = 1,
    val selectedCharacter: String = "Mina",
    val affection: Int = 0,
    val day: Int = 1
)