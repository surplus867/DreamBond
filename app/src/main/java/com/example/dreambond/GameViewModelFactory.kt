package com.example.dreambond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dreambond.data.CharacterMemoryRepository
import com.example.dreambond.data.GameRepository
import com.example.dreambond.data.PlayerJournalRepository

// Factory lets ViewModelProvider create GameViewModel with a repository dependency.
class GameViewModelFactory(
    private val repository: GameRepository,
    private val characterMemoryRepository: CharacterMemoryRepository,
    private val playerJournalRepository: PlayerJournalRepository
): ViewModelProvider.Factory {

    // Safe cast is intentional because we check modelClass first.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Only create GameViewModel here; fail fast for unknown types.
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(
                repository = repository,
                characterMemoryRepository = characterMemoryRepository,
                playerJournalRepository = playerJournalRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}