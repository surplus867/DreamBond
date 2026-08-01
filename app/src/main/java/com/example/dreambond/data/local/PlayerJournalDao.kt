package com.example.dreambond.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerJournalDao {
    @Insert
    suspend fun insertEntry(entry: PlayerJournalEntity): Long

    @Update
    suspend fun updateEntry(entry: PlayerJournalEntity)

    @Delete
    suspend fun deleteEntry(entry: PlayerJournalEntity)

    @Query("SELECT * FROM player_journal ORDER BY date DESC")
    fun getAllEntries(): Flow<List<PlayerJournalEntity>>

    @Query("SELECT * FROM player_journal WHERE characterName = :characterName ORDER BY date DESC")
    fun getEntriesByCharacter(characterName: String): Flow<List<PlayerJournalEntity>>

    @Query("SELECT * FROM player_journal WHERE id = :entryId")
    fun getEntryById(entryId: Int): Flow<PlayerJournalEntity>

    @Query("SELECT * FROM player_journal WHERE gameDay = :gameDay ORDER BY date DESC")
    fun getEntriesByGameDay(gameDay: Int): Flow<List<PlayerJournalEntity>>

    @Query("SELECT * FROM player_journal ORDER BY date DESC LIMIT 100")
    fun getRecentEntries(): Flow<List<PlayerJournalEntity>>
}