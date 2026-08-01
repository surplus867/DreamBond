package com.example.dreambond.data

import com.example.dreambond.data.local.CharacterMemoryDao
import com.example.dreambond.data.local.CharacterMemoryEntity
import kotlinx.coroutines.flow.Flow

class CharacterMemoryRepository(
    private val characterMemoryDao: CharacterMemoryDao
) {
    fun getCharacterMemories(characterName: String): Flow<List<CharacterMemoryEntity>> {
        return characterMemoryDao.getCharacterMemories(characterName)
    }

    fun getMemoriesByType(characterName: String, memoryType: String): Flow<List<CharacterMemoryEntity>> {
        return characterMemoryDao.getMemoriesByType(characterName, memoryType)
    }

    fun getUnlockedMemories(characterName: String): Flow<List<CharacterMemoryEntity>> {
        return characterMemoryDao.getUnlockedMemories(characterName)
    }

    suspend fun insertMemory(memory: CharacterMemoryEntity): Long {
        return characterMemoryDao.insertMemory(memory)
    }

    suspend fun updateMemory(memory: CharacterMemoryEntity) {
        characterMemoryDao.updateMemory(memory)
    }

    suspend fun unlockMemory(memoryId: Int, timestamp: Long) {
        characterMemoryDao.unlockMemory(memoryId, timestamp)
    }

    suspend fun deleteMemory(memory: CharacterMemoryEntity) {
        characterMemoryDao.deleteMemory(memory)
    }
}
