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
    val readyToEndDay: Boolean = false,
    val isTyping: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val memory: MinaMemory = MinaMemory(),
    val showDateQuestion: Boolean = false,
    val dateOptions: List<String> = emptyList(),
    val showFoodQuestion: Boolean = false,
    val foodOptions: List<String> = emptyList(),
    val showTimeQuestion: Boolean = false,
    val timeOptions: List<String> = emptyList(),
    val activeScene: String = "",
    val sceneOptions: List<String> = emptyList()
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
            val updatedMemory = when (option.text) {
                "I wanted to see you." -> current.memory.copy(
                    lastChoice = option.text,
                    gentlePoints = current.memory.gentlePoints + 1
                )

                "I could not sleep." -> current.memory.copy(
                    lastChoice = option.text,
                    gentlePoints = current.memory.gentlePoints + 1
                )

                "I was just curious." -> current.memory.copy(
                    lastChoice = option.text,
                    distantPoints = current.memory.distantPoints + 1
                )

                else -> current.memory.copy(
                    lastChoice = option.text
                )
            }

            current.copy(
                isTyping = true,
                sessionEnded = false,
                readyToEndDay = false,
                messages = current.messages + ChatMessage(text = option.text, isFromUser = true),
                memory = updatedMemory
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
                    readyToEndDay = false,
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

        val shouldAskDate = memory.favoriteDate.isBlank()
        val shouldAskFood = memory.favoriteDate.isNotBlank() && memory.favoriteFood.isBlank()
        val shouldAskTime = memory.favoriteFood.isNotBlank() && memory.favoriteTime.isBlank()

        val intro = when {
            memory.favoriteTime == "Night 🌙" ->
                "The night feels especially quiet today... I thought of you."

            memory.favoriteTime == "Rain 🌧️" ->
                "Something about soft rain always makes me think of warm conversations."

            memory.favoriteTime == "Sunset 🌆" ->
                "Sunset has such a gentle feeling... I wish you could see it with me."

            shouldAskTime ->
                "I keep thinking about what you told me... can I ask you one more thing?"

            shouldAskFood ->
                "I've been curious about you more and more... is it okay if I ask something?"

            memory.favoriteDate.isBlank() ->
                "Before tonight begins... can I ask you something?"

            memory.lastChoice == "I wanted to see you." ->
                "You came back tonight... I was hoping you would."

            memory.lastChoice == "I could not sleep." ->
                "Another quiet night... are you having trouble sleeping again?"

            memory.lastChoice == "I was just curious." ->
                "You're here again... still curious about me?"

            else ->
                character?.introLine ?: ""
        }

        if (shouldAskDate) {
            askFavoriteDateQuestion(intro)
            return
        }
        if (shouldAskFood) {
            askFavoriteFoodQuestion(intro)
            return
        }
        if (shouldAskTime) {
            askFavoriteTimeQuestion(intro)
            return
        }

        _uiState.update { current ->
            current.copy(
                day = current.day + 1,
                currentMessage = intro,
                latestResponse = "",
                sessionEnded = false,
                isTyping = false,
                messages = listOf(ChatMessage(text = intro, isFromUser = false)),
                showDateQuestion = false,
                dateOptions = emptyList(),
                showFoodQuestion = false,
                foodOptions = emptyList(),
                showTimeQuestion = false,
                timeOptions = emptyList()
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
        val personalityType = getPersonalityType()
        val affection = _uiState.value.affection
        val lastChoice = _uiState.value.memory.lastChoice
        val favoriteDate = _uiState.value.memory.favoriteDate

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
                        when (personalityType) {
                            "Gentle" -> "That makes me happy... you always make this place feel warmer."
                            "Playful" -> "You missed me that much? That's kind of cute."
                            "Distant" -> "You say that, but you still feel a little hard to read."
                            else -> "I'm glad you came back tonight."
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

    fun getPersonalityType(): String {
        val memory = _uiState.value.memory

        return when {
            memory.playfulPoints > memory.gentlePoints &&
                    memory.playfulPoints > memory.distantPoints -> "Playful"

            memory.distantPoints > memory.gentlePoints &&
                    memory.distantPoints > memory.playfulPoints -> "Distant"

            else -> "Gentle"
        }
    }

    fun askFavoriteDateQuestion(intro: String? = null) {
        _uiState.update { current ->
            val dayIntro = intro ?: current.currentMessage
            val baseMessages = if (intro != null) {
                listOf(ChatMessage(text = dayIntro, isFromUser = false))
            } else {
                current.messages
            }
            current.copy(
                day = if (intro != null) current.day + 1 else current.day,
                currentMessage = dayIntro,
                latestResponse = if (intro != null) "" else current.latestResponse,
                sessionEnded = if (intro != null) false else current.sessionEnded,
                isTyping = if (intro != null) false else current.isTyping,
                showDateQuestion = true,
                dateOptions = listOf("Night walk", "Cafe date", "Movie night"),
                showFoodQuestion = false,
                foodOptions = emptyList(),
                showTimeQuestion = false,
                timeOptions = emptyList(),
                messages = baseMessages + ChatMessage(
                    text = "What kind of date would you like with me?",
                    isFromUser = false
                )
            )
        }
    }

    fun selectFavoriteDate(date: String) {
        val response = "Ahh... $date sounds nice. I'll remember that."
        _uiState.update { current ->
            current.copy(
                showDateQuestion = false,
                dateOptions = emptyList(),
                sessionEnded = true,
                latestResponse = response,
                memory = current.memory.copy(favoriteDate = date),
                messages = current.messages +
                        ChatMessage(text = date, isFromUser = true) +
                        ChatMessage(text = response, isFromUser = false)
            )
        }
    }

    fun askFavoriteFoodQuestion(intro: String? = null) {
        _uiState.update { current ->
            val dayIntro = intro ?: current.currentMessage
            val baseMessages = if (intro != null) {
                listOf(ChatMessage(text = dayIntro, isFromUser = false))
            } else {
                current.messages
            }
            current.copy(
                day = if (intro != null) current.day + 1 else current.day,
                currentMessage = dayIntro,
                latestResponse = if (intro != null) "" else current.latestResponse,
                sessionEnded = if (intro != null) false else current.sessionEnded,
                isTyping = if (intro != null) false else current.isTyping,
                showFoodQuestion = true,
                foodOptions = listOf("Bingsu 🍧", "Coffee ☕", "Cake 🍰"),
                showDateQuestion = false,
                dateOptions = emptyList(),
                showTimeQuestion = false,
                timeOptions = emptyList(),
                messages = baseMessages + ChatMessage(
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
                memory = current.memory.copy(favoriteFood = food),
                messages = current.messages +
                        ChatMessage(text = food, isFromUser = true) +
                        ChatMessage(
                            text = "Mm... $food sounds nice. I'll remember that.",
                            isFromUser = false
                        )
            )
        }

        if (food.contains("Bingsu", ignoreCase = true)) {
            startBingsuDateScene()
        }
    }

    fun askFavoriteTimeQuestion(intro: String? = null) {
        _uiState.update { current ->
            val dayIntro = intro ?: current.currentMessage
            val baseMessages = if (intro != null) {
                listOf(ChatMessage(text = dayIntro, isFromUser = false))
            } else {
                current.messages
            }
            current.copy(
                day = if (intro != null) current.day + 1 else current.day,
                currentMessage = dayIntro,
                latestResponse = if (intro != null) "" else current.latestResponse,
                sessionEnded = if (intro != null) false else current.sessionEnded,
                isTyping = if (intro != null) false else current.isTyping,
                showTimeQuestion = true,
                timeOptions = listOf("Night 🌙", "Rain 🌧️", "Sunset 🌆"),
                showDateQuestion = false,
                dateOptions = emptyList(),
                showFoodQuestion = false,
                foodOptions = emptyList(),
                messages = baseMessages + ChatMessage(
                    text = "What is your favorite time of day?",
                    isFromUser = false
                )
            )
        }
    }

    fun selectFavoriteTime(time: String) {
        val response = "Ahh… $time. That tells me a lot about you. I'll keep that close."
        _uiState.update { current ->
            current.copy(
                showTimeQuestion = false,
                timeOptions = emptyList(),
                sessionEnded = true,
                latestResponse = response,
                memory = current.memory.copy(
                    favoriteTime = time
                ),
                messages = current.messages +
                        ChatMessage(text = time, isFromUser = true) +
                        ChatMessage(text = response, isFromUser = false)
            )
        }
    }

    fun startBingsuDateScene() {
        _uiState.update { current ->
            current.copy(
                activeScene = "BINGSU_DATE",
                sessionEnded = false,
                messages = current.messages + ChatMessage(
                    text = "I remembered you like bingsu... do you want to go with me tonight?",
                    isFromUser = false
                ),
                sceneOptions = listOf(
                    "Of course, let's go together.",
                    "Only if you choose the flavor.",
                    "Maybe next time."
                )
            )
        }
    }

    fun chooseSceneOption(choice: String) {
        val reply = when (choice) {
            "Of course, let's go together." ->
                "Then let's share one. It feels sweeter with you."

            "Only if you choose the flavor." ->
                "Then I'll choose strawberry. Dont complain later."

            "Maybe next time." ->
                "Okay... maybe another night."

            else -> "..."
        }

        val affectionGain = when (choice) {
            "Of course, let's go together." -> 4
            "Only if you choose the flavor." -> 3
            "Maybe next time." -> 0
            else -> 0
        }

        _uiState.update { current ->
            current.copy(
                affection = current.affection + affectionGain,
                activeScene = "",
                sceneOptions = emptyList(),
                sessionEnded = true,
                messages = current.messages +
                        ChatMessage(choice, isFromUser = true) +
                        ChatMessage(reply, isFromUser = false),
                latestResponse = reply,
                currentMessage = reply
            )
        }
    }

    fun continueAfterReply() {
        _uiState.update { current ->
            current.copy(
                currentMessage = "The night feels softer when we talk like this.",
                latestResponse = "The night feels softer when we talk like this.",
                readyToEndDay = true,
                messages = current.messages + ChatMessage(
                    text = "The night feels softer when we talk like this.",
                    isFromUser = false
                )
            )
        }
    }
}