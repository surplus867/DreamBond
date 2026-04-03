package com.example.dreambond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreambond.data.GameRepository
import com.example.dreambond.data.local.GameProgressEntity
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val selectedCharacter: GirlfriendCharacter? = null,
    val affection: Int = 0,
    val currentMessage: String = "",
    val latestResponse: String = "",
    val day: Int = 1,
    val sessionEnded: Boolean = false
)

class GameViewModel(private val repository: GameRepository) : ViewModel() {
    val characters = listOf(
        GirlfriendCharacter(
            id = 1,
            name = "Mina",
            personality = "Calm and caring",
            introLine = "You came back tonight. I was waiting for you."
        )
    )
    val dialogueOptions = listOf(
        DialogueOption(
            text = "I wanted to see you.",
            affectionChange = 3,
            response = "That makes me happy... I hoped you would say that."
        ),
        DialogueOption(
            text = "I could not sleep.",
            affectionChange = 1,
            response = "Then stay with me a little longer tonight."
        ),
        DialogueOption(
            text = "I was just curious.",
            affectionChange = 0,
            response = "Curious? That is still a reason to come back, I guess."
        )
    )
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    fun selectCharacter(character: GirlfriendCharacter) {
        viewModelScope.launch {
            val savedProgress = repository.getProgress(character.id)

            if (savedProgress != null) {
                _uiState.value = GameUiState(
                    selectedCharacter = character,
                    affection = savedProgress.affection,
                    currentMessage = character.introLine,
                    latestResponse = "",
                    day = savedProgress.day,
                    sessionEnded = false
                )
            } else {
                _uiState.value = GameUiState(
                    selectedCharacter = character,
                    affection = 0,
                    currentMessage = character.introLine,
                    latestResponse = "",
                    day = 1,
                    sessionEnded = false
                )
            }
        }
    }

    fun chooseReply(option: DialogueOption) {
        val dynamicReply = getDynamicReply(option)

        _uiState.update { current ->
            current.copy(
                affection = current.affection + option.affectionChange,
                latestResponse = dynamicReply,
                sessionEnded = true
            )
        }
    }

    fun nextDay() {
        val character = _uiState.value.selectedCharacter
        _uiState.update { current ->
            current.copy(
                day = current.day + 1,
                currentMessage = character?.introLine ?: "",
                latestResponse = "",
                sessionEnded = false
            )
        }
    }

    fun saveProgress() {
        val state = _uiState.value
        val character = state.selectedCharacter ?: return

        val progress = GameProgressEntity(
            id = character.id,
            selectedCharacter = character.name,
            affection = state.affection,
            day = state.day
        )

        viewModelScope.launch {
            repository.saveProgress(progress)
        }
    }

    fun resetGame() {
        _uiState.value = GameUiState()
    }

    fun clearAllProgress() {
        viewModelScope.launch {
            repository.clearAllProgress()
            _uiState.value = GameUiState()
        }
    }

    fun getRelationShipLevel(): String {
        val affection = _uiState.value.affection
        return when {
            affection < 10 -> "Stranger"
            affection < 20 -> "Friend"
            affection < 50 -> "Close"
            else -> "Special"
        }
    }

    fun getDynamicReply(option: DialogueOption): String {
        val affection = _uiState.value.affection

        return when {
            affection < 10 -> {
                when (option.text) {
                    "I wanted to see you." -> "You wanted to see me... ? I didn't expect that."
                    "I could not sleep." -> "Then maybe the night brought you here for a reason."
                    else -> "You're a little hard to read... but I don't mind."
                }
            }

            affection < 25 -> {
                when (option.text) {
                    "I wanted to see you." -> "I'm glad you came back tonight."
                    "I could not sleep." -> "Then stay with me for a while. It's peaceful here."
                    else -> "You always say things that make me curious."
                }
            }

            affection < 50 -> {
                when (option.text) {
                    "I wanted to see you." -> "I was hoping you'd say that."
                    "I could not sleep." -> "Then don't rush off yet. I like these quiet moments with you."
                    else -> "You know... you're kind of cute when you act mysterious."
                }
            }

            else -> {
                when (option.text) {
                    "I wanted to see you." -> "I missed you... I was waiting for you again."
                    "I could not sleep." -> "Then stay. Nights feel softer when you're here."
                    else -> "Even when you pretend otherwise, you always come back to me."
                }
            }
        }
    }
}