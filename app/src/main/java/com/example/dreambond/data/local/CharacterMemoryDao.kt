package com.example.dreambond.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterMemoryDao {
    @Insert
    suspend fun insertMemory(memory: CharacterMemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: CharacterMemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: CharacterMemoryEntity)

    @Query("SELECT * FROM character_memories WHERE characterName = :characterName ORDER BY unlockedDate DESC")
    fun getCharacterMemories(characterName: String): Flow<List<CharacterMemoryEntity>>

    @Query("SELECT * FROM character_memories WHERE characterName = :characterName AND memoryType = :memoryType")
    fun getMemoriesByType(characterName: String, memoryType: String): Flow<List<CharacterMemoryEntity>>

    @Query("SELECT * FROM character_memories WHERE characterName = :characterName AND unlocked = 1")
    fun getUnlockedMemories(characterName: String): Flow<List<CharacterMemoryEntity>>

    @Query("UPDATE character_memories SET unlocked = 1, unlockedDate = :timestamp WHERE id = :memoryId")
    suspend fun unlockMemory(memoryId: Int, timestamp: Long)
}