package com.example.dreambond.data

import com.example.dreambond.data.local.PlayerJournalDao
import com.example.dreambond.data.local.PlayerJournalEntity
import kotlinx.coroutines.flow.Flow

class PlayerJournalRepository(
    private val playerJournalDao: PlayerJournalDao
) {
    fun getAllEntries(): Flow<List<PlayerJournalEntity>> {
        return playerJournalDao.getAllEntries()
    }

    fun getEntriesByCharacter(characterName: String): Flow<List<PlayerJournalEntity>> {
        return playerJournalDao.getEntriesByCharacter(characterName)
    }

    fun getEntryById(entryId: Int): Flow<PlayerJournalEntity> {
        return playerJournalDao.getEntryById(entryId)
    }

    fun getEntriesByGameDay(gameDay: Int): Flow<List<PlayerJournalEntity>> {
        return playerJournalDao.getEntriesByGameDay(gameDay)
    }

    fun getRecentEntries(): Flow<List<PlayerJournalEntity>> {
        return playerJournalDao.getRecentEntries()
    }

    suspend fun insertEntry(entry: PlayerJournalEntity): Long {
        return playerJournalDao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: PlayerJournalEntity) {
        playerJournalDao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: PlayerJournalEntity) {
        playerJournalDao.deleteEntry(entry)
    }
}
