package com.example.dreambond.data

import com.example.dreambond.data.local.GameProgressDao
import com.example.dreambond.data.local.GameProgressEntity

class GameRepository(
    private val gameProgressDao: GameProgressDao
) {

    suspend fun saveProgress(progress: GameProgressEntity) {
        gameProgressDao.saveProgress(progress)
    }

    suspend fun getProgress(characterId: Int): GameProgressEntity? {
        return gameProgressDao.getProgress(characterId)
    }

    suspend fun clearAllProgress() {
        gameProgressDao.clearAll()
    }
}