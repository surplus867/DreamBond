package com.example.dreambond.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_journal")
data class PlayerJournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val title: String,
    val content: String,
    val characterName: String, // which character this entry relates to
    val mood: String = "Neutral", // player's mood when writing
    val date: Long, //timestamp
    val gameDay: Int, // in-game day
    val tags: String = "" // comma-separated tags
)