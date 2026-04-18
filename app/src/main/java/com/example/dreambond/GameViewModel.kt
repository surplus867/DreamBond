package com.example.dreambond

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dreambond.data.GameRepository
import com.example.dreambond.data.local.GameProgressEntity
import com.example.dreambond.model.DialogueOption
import com.example.dreambond.model.GirlfriendCharacter
import com.example.dreambond.model.MinaMemory
import com.example.dreambond.ui.ChatMessage
import kotlinx.coroutines.delay
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
    val sessionEnded: Boolean = false,
    val isTyping: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val memory: MinaMemory = MinaMemory(),
    val showDateQuestion: Boolean = false,
    val dateOptions: List<String> = emptyList(),
    val showFoodQuestion: Boolean = false,
    val foodOptions: List<String> = emptyList()
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
                    sessionEnded = false,
                    isTyping = false,
                    messages = listOf(ChatMessage(text = character.introLine, isFromUser = false))
                )
            } else {
                _uiState.value = GameUiState(
                    selectedCharacter = character,
                    affection = 0,
                    currentMessage = character.introLine,
                    latestResponse = "",
                    day = 1,
                    sessionEnded = false,
                    isTyping = false,
                    messages = listOf(ChatMessage(text = character.introLine, isFromUser = false))
                )
            }
        }
    }

    fun chooseReply(option: DialogueOption) {
        val dynamicReply = getDynamicReply(option)

        // User message + start typing
        _uiState.update { current ->
            current.copy(
                isTyping = true,
                sessionEnded = false,
                messages = current.messages + ChatMessage(text = option.text, isFromUser = true),
                memory = current.memory.copy(lastChoice = option.text)
            )
        }

        // Delay + Mina response
        viewModelScope.launch {
            delay(1200)
            _uiState.update { current ->
                current.copy(
                    affection = current.affection + option.affectionChange,
                    currentMessage = dynamicReply,
                    latestResponse = dynamicReply,
                    sessionEnded = true,
                    isTyping = false,
                    messages = current.messages + ChatMessage(
                        text = dynamicReply,
                        isFromUser = false
                    )
                )
            }
        }
    }

    fun nextDay() {
        val character = _uiState.value.selectedCharacter
        val memory = _uiState.value.memory
        val favoriteDate = memory.favoriteDate

        val intro = when {
            favoriteDate.isNotBlank() ->
                "It's quiet tonight... it reminds me of a $favoriteDate. I'm glad you're here."

            memory.favoriteDate.isBlank() -> "Before tonight begins... can I ask you something?"
            memory.lastChoice == "I wanted to see you." -> "You came back tonight... I was hoping you would."
            memory.lastChoice == "I could not sleep." -> "Another quiet night... are you having trouble sleeping again?"
            memory.lastChoice == "I was just curious." -> "You're here again... still curious about me?"
            else -> character?.introLine ?: ""
        }

        _uiState.update { current ->
            current.copy(
                day = current.day + 1,
                currentMessage = intro,
                latestResponse = "",
                sessionEnded = false,
                isTyping = false,
                messages = listOf(
                    ChatMessage(
                        text = intro,
                        isFromUser = false
                    )
                )
            )
        }

        if (_uiState.value.memory.favoriteDate.isBlank()) {
            askFavoriteDateQuestion()
        } else if (
            _uiState.value.memory.favoriteDate.isNotBlank() &&
            _uiState.value.memory.favoriteFood.isBlank()
        ) {
            askFavoriteFoodQuestion()
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
        clearAllProgress()
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
        val lastChoice = _uiState.value.memory.lastChoice
        val favoriteDate = _uiState.value.memory.favoriteDate
        val favoriteFood = _uiState.value.memory.favoriteFood

        return when {
            affection < 10 -> {
                when (option.text) {
                    "I wanted to see you." -> {
                        if (lastChoice == "I could not sleep.") {
                            "You came back again... was it another restless night?"
                        } else {
                            "You wanted to see me... ? I didn't expect that."
                        }
                    }

                    "I could not sleep." -> "Then maybe the night brought you here for a reason."
                    else -> "You're a little hard to read... but I don't mind."
                }
            }

            affection < 25 -> {
                when (option.text) {
                    "I wanted to see you." -> {
                        if (favoriteFood.isNotBlank()) {
                            "I'm glad you came back… maybe we can share some $favoriteFood together someday."
                        } else {
                            "I'm glad you came back tonight."
                        }
                    }

                    "I could not sleep." -> {
                        if (favoriteDate == "Night walk") {
                            "Maybe a quiet night walk would help you rest... you said you liked that."
                        } else {
                            "Then stay with me for a while. It's peaceful here."
                        }
                    }

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

    fun askFavoriteDateQuestion() {
        _uiState.update { current ->
            current.copy(
                showDateQuestion = true,
                dateOptions = listOf("Night walk", "Cafe date", "Movie night"),
                messages = current.messages + ChatMessage(
                    text = "What kind of date would you like with me?",
                    isFromUser = false
                )
            )
        }
    }

    fun selectFavoriteDate(date: String) {
        _uiState.update { current ->
            current.copy(
                showDateQuestion = false,
                dateOptions = emptyList(),
                memory = current.memory.copy(
                    favoriteDate = date
                ),
                messages = current.messages +
                        ChatMessage(
                            text = date,
                            isFromUser = true
                        ) +
                        ChatMessage(
                            text = "Mm... $date sounds nice. I'll remember that.",
                            isFromUser = false
                        )
            )
        }
    }

    fun askFavoriteFoodQuestion() {
        _uiState.update { current ->
            current.copy(
                showFoodQuestion = true,
                foodOptions = listOf("Bingsu 🍧", "Coffee ☕", "Cake 🍰"),
                messages = current.messages + ChatMessage(
                    text = "What kind of dessert do you like?",
                    isFromUser = false
                )
            )
        }
    }

    fun selectFavoriteFood(food: String) {
        _uiState.update { current ->
            current.copy(
                showFoodQuestion = false,
                foodOptions = emptyList(),
                memory = current.memory.copy(
                    favoriteFood = food
                ),
                messages = current.messages +
                        ChatMessage(
                            text = food,
                            isFromUser = true
                        ) +
                        ChatMessage(
                            text = "Mm… $food sounds nice. It feels like something we could enjoy on a quiet night.",
                            isFromUser = false
                        )
            )
        }
    }
}