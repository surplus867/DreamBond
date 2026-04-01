package com.example.dreambond.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameProgressDao {

    @Query("SELECT * FROM game_progress WHERE id = :characterId")
    suspend fun getProgress(characterId: Int): GameProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: GameProgressEntity)

    @Query("DELETE FROM game_progress")
    suspend fun clearAll()
}