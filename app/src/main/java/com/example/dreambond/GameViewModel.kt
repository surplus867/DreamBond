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
        _uiState.update {
            it.copy(
                selectedCharacter = character,
                currentMessage = character.introLine,
                latestResponse = "",
                sessionEnded = false
            )
        }
    }
    fun chooseReply(option: DialogueOption) {
        _uiState.update { current ->
            current.copy(
                affection = current.affection + option.affectionChange,
                latestResponse = option.response,
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
        val characterName = state.selectedCharacter?.name ?: return
        viewModelScope.launch {
            repository.saveProgress(
                GameProgressEntity(
                    id = 1,
                    selectedCharacter = characterName,
                    affection = state.affection,
                    day = state.day
                )
            )
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
            affection <50 -> "Close"
            else -> "Special"
        }
    }
}