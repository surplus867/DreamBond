package com.example.dreambond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dreambond.data.GameRepository

// Factory lets ViewModelProvider create GameViewModel with a repository dependency.
class GameViewModelFactory(
    private val repository: GameRepository
): ViewModelProvider.Factory {

    // Safe cast is intentional because we check modelClass first.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Only create GameViewModel here; fail fast for unknown types.
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}