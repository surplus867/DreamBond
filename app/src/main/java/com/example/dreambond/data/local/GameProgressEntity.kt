package com.example.dreambond.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgressEntity(

    @PrimaryKey
    val id: Int = 1,

    val selectedCharacter: String = "Mina",

    val affection: Int = 0,

    val day: Int = 1,

    // Memory system
    val favoriteDate: String = "",
    val favoriteFood: String = "",
    val favoriteTime: String = "",
    val lastDateScene: String = "",

    // Personality shaping
    val gentlePoints: Int = 0,
    val playfulPoints: Int = 0,
    val distantPoints: Int = 0,

    // Mood system
    val mood: String = "Calm",
    val moodIntensity: Int = 0
)