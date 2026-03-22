package com.example.dreambond

import androidx.lifecycle.ViewModel
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val selectedCharacter:
    GirlfriendCharacter? = null,
    val affection: Int = 0,
    val currentMessage: String = "",
    val latestResponse: String = "",
    val day: Int = 1,
    val sessionEnded: Boolean = false
)

class GameViewModel : ViewModel() {

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

    private val _uiState =
        MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> =
        _uiState.asStateFlow()

    fun selectCharacter(
        character:
        GirlfriendCharacter
    ) {
        _uiState.update {
            it.copy(
                selectedCharacter =
                    character,

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
                latestResponse =
                    option.response,
                sessionEnded = true
            )
        }
    }

    fun nextDay() {
        val character =
            _uiState.value.selectedCharacter
        _uiState.update { current ->
            current.copy(
                day = current.day + 1,
                currentMessage =
                    character?.introLine ?: "",
                latestResponse = "",
                sessionEnded = false
            )
        }
    }

    fun resetGame() {
        _uiState.value = GameUiState()
    }
}