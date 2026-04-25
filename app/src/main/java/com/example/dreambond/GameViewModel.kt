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
import kotlin.random.Random

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
    val mood: String = "Calm",
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

    private fun getMoodFromChoice(choice: String): String {
        return when (choice) {
            "I wanted to see you." -> "Happy"
            "I could not sleep." -> "Calm"
            "I was just curious." -> "Playful"
            else -> "Calm"
        }
    }

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
                mood = getMoodFromChoice(option.text),
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

        val defaultIntro = when {
            memory.lastChoice == "I wanted to see you." ->
                "You came back tonight... I was hoping you would."

            memory.lastChoice == "I could not sleep." ->
                "Another quiet night... are you having trouble sleeping again?"

            memory.lastChoice == "I was just curious." ->
                "You're here again... still curious about me?"

            shouldAskTime ->
                "I keep thinking about what you told me... can I ask you one more thing?"

            shouldAskFood ->
                "I've been curious about you more and more... is it okay if I ask something?"

            memory.favoriteDate.isBlank() ->
                "Before tonight begins... can I ask you something?"

            else ->
                character?.introLine ?: ""
        }

        val initiatedLine = getMinaInitiatedLine()
        val recallLine = getMemoryRecallLine()

        val intro = when {
            shouldAskDate || shouldAskFood || shouldAskTime -> defaultIntro
            initiatedLine != null && Random.nextInt(100) < 60 -> initiatedLine
            recallLine != null && Random.nextInt(100) < 40 -> recallLine
            else -> defaultIntro
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
        val mood = _uiState.value.mood
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
                        when (mood) {
                            "Happy" -> "You came back... that really makes me happy."
                            "Playful" -> "You missed me that much?"
                            "Calm" -> "I'm glad you came back tonight."
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
                    "I was just curious." -> "Still curious about me... I don't mind that at all."
                    else -> "You know... you're kind of cute when you act mysterious."
                }
            }

            else -> {
                when (option.text) {
                    "I wanted to see you." -> "I missed you... I was waiting for you again."
                    "I could not sleep." -> "Then stay. Nights feel softer when you're here."
                    "I was just curious." -> "You're here again... still curious about me?"
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

    fun getGoodnightMessage(): String {
        val memory = _uiState.value.memory
        val mood = _uiState.value.mood

        return when {
            memory.lastDateScene == "Cafe Date ☕" ->
                "Goodnight... I keep thinking about that quiet cafe with you."

            memory.lastDateScene == "Bingsu Date 🍧" ->
                "Goodnight... I keep thinking about our bingsu date."

            memory.favoriteFood.contains("Coffee", true) ->
                "Goodnight... maybe next time we can sit somewhere quiet with coffee."

            memory.favoriteTime == "Rain 🌧️" ->
                "Goodnight... I like nights like this, soft and quiet."

            else -> when (mood) {
                "Happy" -> "Goodnight... today felt really nice."
                "Shy" -> "Goodnight... I'll be here tomorrow."
                "Playful" -> "Goodnight... don't forget about me."
                else -> "Goodnight... I'll be waiting."
            }
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
                currentMessage = response,
                sessionEnded = true,
                readyToEndDay = false,
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
        if (food.contains("Bingsu", ignoreCase = true)) {
            _uiState.update { current ->
                current.copy(
                    showFoodQuestion = false,
                    foodOptions = emptyList(),
                    readyToEndDay = false,
                    memory = current.memory.copy(favoriteFood = food),
                    messages = current.messages +
                            ChatMessage(text = food, isFromUser = true) +
                            ChatMessage(
                                text = "Mm... $food sounds nice. I'll remember that.",
                                isFromUser = false
                            )
                )
            }
            startBingsuDateScene()
            return
        }

        if (food.contains("Coffee", ignoreCase = true)) {
            _uiState.update { current ->
                current.copy(
                    showFoodQuestion = false,
                    foodOptions = emptyList(),
                    readyToEndDay = false,
                    memory = current.memory.copy(favoriteFood = food),
                    messages = current.messages +
                            ChatMessage(text = food, isFromUser = true) +
                            ChatMessage(
                                text = "Mm... $food sounds nice. I'll remember that.",
                                isFromUser = false
                            )
                )
            }
            startCafeDateScene()
            return
        }

        val response = "Mm... $food sounds nice. I'll remember that."
        _uiState.update { current ->
            current.copy(
                showFoodQuestion = false,
                foodOptions = emptyList(),
                currentMessage = response,
                latestResponse = response,
                sessionEnded = true,
                readyToEndDay = false,
                memory = current.memory.copy(favoriteFood = food),
                messages = current.messages +
                        ChatMessage(text = food, isFromUser = true) +
                        ChatMessage(text = response, isFromUser = false)
            )
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
                currentMessage = response,
                sessionEnded = true,
                readyToEndDay = false,
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
                currentMessage = "I remembered you like bingsu... do you want to go with me tonight?",
                latestResponse = "",
                sessionEnded = false,
                readyToEndDay = false,
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

    fun startCafeDateScene() {
        _uiState.update { current ->
            current.copy(
                activeScene = "CAFE_DATE",
                sessionEnded = false,
                messages = current.messages + ChatMessage(
                    text = "I remembered you like coffee... would you like to go to a quiet cafe with me?",
                    isFromUser = false
                ),
                sceneOptions = listOf(
                    "Let's sit by the window.",
                    "You choose the drink for me.",
                    "Maybe just a short visit."
                )
            )
        }
    }

    fun chooseSceneOption(choice: String) {
        val currentScene = _uiState.value.activeScene

        val reply = when (currentScene) {
            "CAFE_DATE" -> when (choice) {
                "Let's sit by the window.", "Let’s sit by the window." ->
                    "That sounds perfect... I like watching the city lights with you."

                "You choose the drink for me." ->
                    "Then I'll choose something sweet. I hope you trust my taste."

                "Maybe just a short visit." ->
                    "That's okay... even a short time with you is enough."

                else -> "..."
            }

            "BINGSU_DATE" -> when (choice) {
                "Of course, let's go together.", "Of course, let’s go together." ->
                    "Then let's share one. It feels sweeter with you."

                "Only if you choose the flavor." ->
                    "Then I'll choose strawberry. Don't complain later."

                "Maybe next time." ->
                    "Okay... maybe another night."

                else -> "..."
            }

            else -> "..."
        }

        val affectionGain = when (choice) {
            "Let's sit by the window.", "Let’s sit by the window." -> 4
            "You choose the drink for me." -> 3
            "Maybe just a short visit." -> 1

            "Of course, let's go together.", "Of course, let’s go together." -> 4
            "Only if you choose the flavor." -> 3
            "Maybe next time." -> 0

            else -> 0
        }

        val sceneName = when (currentScene) {
            "CAFE_DATE" -> "Cafe Date ☕"
            "BINGSU_DATE" -> "Bingsu Date 🍧"
            else -> ""
        }

        _uiState.update { current ->
            current.copy(
                affection = current.affection + affectionGain,
                activeScene = "",
                sceneOptions = emptyList(),
                sessionEnded = true,
                readyToEndDay = false,
                memory = current.memory.copy(
                    lastDateScene = sceneName
                ),
                messages = current.messages +
                        ChatMessage(choice, isFromUser = true) +
                        ChatMessage(reply, isFromUser = false),
                latestResponse = reply,
                currentMessage = reply
            )
        }
    }

    fun getMemoryRecallLine(): String? {
        val memory = _uiState.value.memory
        val personality = getPersonalityType()

        return when {
            memory.lastDateScene == "Cafe Date ☕" ->
                "I was thinking about that quiet cafe... sitting with you felt peaceful."

            memory.lastDateScene == "Bingsu Date 🍧" ->
                "I keep thinking about our bingsu date... it felt sweeter because you were there."

            memory.favoriteFood.contains("Bingsu", ignoreCase = true) ->
                "You said you like bingsu... I still remember that."

            memory.favoriteFood.contains("Coffee", ignoreCase = true) ->
                "You said you like coffee... maybe that suits quiet nights like this."

            memory.favoriteTime == "Rain 🌧️" ->
                "Rainy nights always remind me of soft conversations."

            memory.favoriteTime == "Night 🌙" ->
                "Nights like this feel familiar somehow."

            personality == "Playful" ->
                "You have this playful side... I notice it more than you think."

            personality == "Distant" ->
                "You still feel a little hard to read... but I want to understand you."

            else -> null
        }
    }

    fun getMinaInitiatedLine(): String? {
        val memory = _uiState.value.memory
        val personality = getPersonalityType()
        val mood = _uiState.value.mood

        return when {
            memory.lastDateScene == "Cafe Date ☕" ->
                "I was thinking about that cafe again... it felt peaceful being there with you."

            memory.lastDateScene == "Bingsu Date 🍧" ->
                "I still remember our bingsu date... it made me smile today."

            memory.favoriteFood.contains("Coffee", ignoreCase = true) ->
                "I saw a quiet cafe in my dream... it reminded me of you."

            memory.favoriteFood.contains("Bingsu", ignoreCase = true) ->
                "I thought about bingsu today... maybe because of you."

            memory.favoriteTime == "Rain 🌧️" ->
                "It feels like a rainy kind of night... soft and quiet."

            personality == "Playful" ->
                "You know... you always make things a little more fun."

            personality == "Distant" ->
                "Sometimes I wonder what you’re really thinking."

            mood == "Happy" ->
                "I had a good feeling you would come back tonight."

            else -> null
        }
    }

    fun continueAfterReply() {
        val finalLine = getGoodnightMessage()

        _uiState.update { current ->
            if (!current.sessionEnded || current.readyToEndDay) {
                return@update current
            }

            current.copy(
                readyToEndDay = true,
                latestResponse = finalLine,
                currentMessage = finalLine,
                messages = current.messages + ChatMessage(
                    text = finalLine,
                    isFromUser = false
                )
            )
        }
    }
}